package com.g2rain.syncer;


import com.g2rain.common.syncer.AbstractEventSubscriber;
import com.g2rain.common.syncer.DefaultMessageDispatcher;
import com.g2rain.common.syncer.MessageDispatcher;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>基于 Spring Cloud Stream 的事件订阅者实现，用于从显式声明的 {@link SubscribableChannel} 接收消息，
 * 并通过 {@link AbstractEventSubscriber#dispatcher} 分发处理。</p>
 *
 * <p>该实现只会订阅“输入 binding”（优先使用 {@link BindingServiceProperties#getInputBindings()}），
 * 并在容器中解析到对应的 {@link SubscribableChannel} 后再执行订阅，避免误接管业务应用里其他无关的 Stream channel。</p>
 *
 * <p><b>线程安全：</b>订阅动作在 {@link #afterSingletonsInstantiated()} 中只会执行一次（双重检查 + synchronized），
 * 避免重复 subscribe。</p>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>消息载荷会先通过 {@link StreamEventPayloads} 归一化为 dispatcher 可处理的原始 JSON 字符串</li>
 *     <li>若未声明 binding 配置，则不会订阅任何通道</li>
 * </ul>
 *
 * @author alpha
 * @since 2026/1/2
 */
public class StreamEventSubscriber extends AbstractEventSubscriber implements SmartInitializingSingleton {

    /**
     * 容器中可用的 {@code bindingName -> SubscribableChannel} 映射提供器。
     * <p>这里使用 {@link ObjectProvider} 以避免在某些应用未启用 Stream 时强依赖该 Bean。</p>
     */
    private final ObjectProvider<Map<String, SubscribableChannel>> channelsProvider;

    /**
     * Spring BeanFactory 提供器，用于在未显式提供 {@code bindingName -> channel} 映射时，从容器按类型兜底获取通道。
     * <p>这样可以兼容不同 Spring Cloud Stream 版本/运行时对通道 Bean 的组织形式差异。</p>
     */
    private final ObjectProvider<ListableBeanFactory> beanFactoryProvider;

    /**
     * Cloud Stream binding 配置提供器，用于确定“哪些 binding 需要被订阅”。
     */
    private final ObjectProvider<BindingServiceProperties> bindingServicePropertiesProvider;

    /**
     * 是否已完成订阅；用于确保 subscribe 只执行一次。
     */
    private volatile boolean subscribed;

    /**
     * 创建 {@code StreamEventSubscriber}。
     *
     * <p>每个显式声明的 input binding 通道会被注册订阅回调，将接收到的消息归一化后交由事件分发器处理。</p>
     *
     * @param channelsProvider                 通道名称到 {@link SubscribableChannel} 的映射提供器
     * @param beanFactoryProvider              Spring BeanFactory 提供器（兜底按类型查找通道）
     * @param bindingServicePropertiesProvider binding 配置提供器
     * @param dispatcher                       消息分发器
     */
    public StreamEventSubscriber(ObjectProvider<Map<String, SubscribableChannel>> channelsProvider,
                                 ObjectProvider<ListableBeanFactory> beanFactoryProvider,
                                 ObjectProvider<BindingServiceProperties> bindingServicePropertiesProvider,
                                 MessageDispatcher dispatcher) {
        super(Objects.requireNonNullElseGet(dispatcher, DefaultMessageDispatcher::new));
        this.channelsProvider = channelsProvider;
        this.beanFactoryProvider = beanFactoryProvider;
        this.bindingServicePropertiesProvider = bindingServicePropertiesProvider;
    }

    /**
     * 在 Spring 容器完成单例初始化后执行一次订阅注册。
     *
     * <p>该时机能够确保 Stream 基础设施与 {@link SubscribableChannel} 先完成装配，避免过早订阅导致通道未就绪。</p>
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (subscribed) {
            return;
        }

        synchronized (this) {
            if (subscribed) {
                return;
            }

            Map<String, SubscribableChannel> channels = resolveSubscribedChannels();
            channels.forEach((_, channel) -> channel.subscribe(this::dispatchMessage));
            subscribed = true;
        }
    }

    /**
     * 计算实际需要订阅的通道集合。
     *
     * <p>策略：只订阅“consumer input bindings”。</p>
     * <ul>
     *   <li>优先使用 {@link BindingServiceProperties#getInputBindings()}（逗号分隔列表）</li>
     *   <li>若未配置 input-bindings，则仅订阅命名符合函数式约定的 {@code *-in-0}（避免误订阅 output）</li>
     * </ul>
     */
    private Map<String, SubscribableChannel> resolveSubscribedChannels() {
        Map<String, SubscribableChannel> channels = channelsProvider.getIfAvailable(Collections::emptyMap);
        if (channels.isEmpty()) {
            ListableBeanFactory beanFactory = beanFactoryProvider == null ? null : beanFactoryProvider.getIfAvailable();
            if (beanFactory != null) {
                // Spring Cloud Stream 通常会把每个 binding 的 input/output channel 作为 Bean 暴露出来。
                // 这里按类型兜底拿到全部通道，再用 bindings 配置做过滤，以避免误订阅无关通道。
                channels = new LinkedHashMap<>(beanFactory.getBeansOfType(SubscribableChannel.class));
            }
        }
        if (channels.isEmpty()) {
            return Map.of();
        }

        BindingServiceProperties bindingServiceProperties = bindingServicePropertiesProvider.getIfAvailable();
        if (Objects.isNull(bindingServiceProperties) || Objects.isNull(bindingServiceProperties.getBindings())
            || bindingServiceProperties.getBindings().isEmpty()) {
            return Map.of();
        }

        Set<String> inputBindingNames = resolveInputBindingNames(bindingServiceProperties);
        if (inputBindingNames.isEmpty()) {
            return Map.of();
        }

        Map<String, SubscribableChannel> subscribedChannels = new LinkedHashMap<>();
        for (String bindingName : inputBindingNames) {
            SubscribableChannel channel = channels.get(bindingName);
            if (Objects.isNull(channel)) {
                // 兼容 Spring Cloud Stream 运行时对通道名的前缀化（例如 <appName>.<bindingName>）。
                // 优先做后缀匹配，避免“扫描全容器通道”带来的误订阅风险。
                String suffix = "." + bindingName;
                String in0 = bindingName + "-in-0";
                String suffixIn0 = "." + in0;
                for (Map.Entry<String, SubscribableChannel> entry : channels.entrySet()) {
                    String channelName = entry.getKey();
                    if (channelName == null) {
                        continue;
                    }

                    // 常见命名：<bindingName>
                    if (channelName.equals(bindingName)) {
                        channel = entry.getValue();
                        break;
                    }
                    // 函数式模型常见命名：<bindingName>-in-0
                    if (channelName.equals(in0)) {
                        channel = entry.getValue();
                        break;
                    }
                    // 前缀化命名：<appName>.<bindingName> 或 <appName>.<bindingName>-in-0
                    if (channelName.endsWith(suffix) || channelName.endsWith(suffixIn0)) {
                        channel = entry.getValue();
                        break;
                    }
                }
            }

            if (Objects.nonNull(channel)) {
                subscribedChannels.put(bindingName, channel);
            }
        }

        return subscribedChannels;
    }

    private Set<String> resolveInputBindingNames(BindingServiceProperties bindingServiceProperties) {
        String configuredInputBindings = bindingServiceProperties.getInputBindings();
        if (configuredInputBindings != null && !configuredInputBindings.isBlank()) {
            Set<String> names = new LinkedHashSet<>();
            for (String raw : configuredInputBindings.split(",")) {
                String name = raw.trim();
                if (!name.isBlank()) {
                    names.add(name);
                }
            }
            return names;
        }

        // 关键约束：
        // - Spring Cloud 的 BindingProperties 在部分版本中 consumer/producers 可能默认非空；
        //   若用“是否存在 consumer 配置”来推断 input，会把纯 output binding 误判为 input，导致生产者自消费。
        // 因此这里不做启发式推断，而是仅在函数式命名约定下做兜底。
        Set<String> names = new LinkedHashSet<>();
        bindingServiceProperties.getBindings().keySet().forEach(name -> {
            if (name != null && name.endsWith("-in-0")) {
                names.add(name);
            }
        });
        return names;
    }

    /**
     * 将 Stream Message 归一化为 {@link DefaultMessageDispatcher} 可消费的 raw JSON 字符串并分发。
     */
    private void dispatchMessage(Message<?> message) {
        String rawMessage = StreamEventPayloads.extractRawMessage(message);
        if (Objects.isNull(rawMessage) || rawMessage.isBlank()) {
            // 空消息没有可分发内容；按 common.syncer 的契约直接跳过，避免无意义解析与噪音日志。
            return;
        }

        dispatcher.dispatch(rawMessage);
    }
}
