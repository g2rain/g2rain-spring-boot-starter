package com.g2rain.syncer;


import com.g2rain.common.syncer.EventPublisher;
import com.g2rain.common.syncer.EventPublisherHub;
import com.g2rain.common.syncer.DefaultMessageDispatcher;
import com.g2rain.common.syncer.MessageDispatcher;
import com.g2rain.common.utils.Collections;
import com.g2rain.common.utils.Strings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.messaging.SubscribableChannel;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>G2rain Syncer 的 Spring Cloud Stream 适配自动配置。</p>
 *
 * <p>该配置只负责将 {@code common.syncer} 适配到应用<strong>显式声明</strong>的 Spring Cloud Stream bindings：</p>
 * <ul>
 *     <li><b>发布侧</b>：为 outbound bindings 创建 {@link StreamBridgeEventPublisher}，并汇聚为 {@link EventPublisherHub}</li>
 *     <li><b>订阅侧</b>：仅对 {@link BindingServiceProperties#getBindings()} 中出现且能在容器中解析到的
 *     {@link SubscribableChannel} 进行订阅，交由 {@link StreamEventSubscriber} 分发</li>
 * </ul>
 *
 * <p><b>功能：</b></p>
 * <ul>
 *     <li>创建 {@link StreamEventSubscriber} Bean，用于订阅显式声明的 Stream binding 并交由事件分发器处理</li>
 *     <li>创建 {@link StreamBridgeEventPublisher} Bean 并汇聚到 {@link EventPublisherHub}，用于发布事件到 outbound binding</li>
 *     <li>注册 {@link SyncerInitializer} Bean，在所有单例 Bean 初始化完成后触发首次缓存加载</li>
 * </ul>
 *
 * <p><b>注意事项：</b></p>
 * <ul>
 *     <li>该适配层不再回退到“扫描容器中全部 MessageChannel/SubscribableChannel”的旧模式</li>
 *     <li>若未声明 binding 配置（或无可解析的通道），则发布者集合为空，订阅者也不会接管任何通道</li>
 *     <li>SyncerInitializer 仅负责首次缓存初始化，不干涉后续业务微服务的缓存操作</li>
 * </ul>
 *
 * @author alpha
 * @since 2026/1/2
 */
@AutoConfiguration
public class SyncerAutoConfiguration {

    /**
     * 创建 {@link StreamEventSubscriber} Bean。
     *
     * <p>从容器提供的 {@code bindingName -> SubscribableChannel} 映射中解析通道，并结合
     * {@link BindingServiceProperties#getBindings()} 做过滤，只订阅应用显式声明的 binding。</p>
     *
     * <p>当容器中已存在同类型 Bean 时，该方法不会重复注册。</p>
     *
     * @param channelsProvider Spring 容器提供的通道映射，key 为绑定名称
     * @return StreamEventSubscriber 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(StreamEventSubscriber.class)
    public StreamEventSubscriber streamEventSubscriber(
        ObjectProvider<Map<String, SubscribableChannel>> channelsProvider,
        ObjectProvider<ListableBeanFactory> beanFactoryProvider,
        ObjectProvider<BindingServiceProperties> bindingServicePropertiesProvider,
        ObjectProvider<MessageDispatcher> dispatcherProvider) {
        MessageDispatcher dispatcher = dispatcherProvider.getIfAvailable(DefaultMessageDispatcher::new);
        return new StreamEventSubscriber(channelsProvider, beanFactoryProvider, bindingServicePropertiesProvider, dispatcher);
    }

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(MessageDispatcher.class)
    public MessageDispatcher messageDispatcher() {
        return new DefaultMessageDispatcher();
    }

    /**
     * 创建 {@link EventPublisherHub} Bean。
     *
     * <p>基于 Spring Cloud Stream 显式声明（或可推断）的 outbound bindings 创建发布者，
     * 并汇聚成一个事件发布中心。发布时按 bindingName 进行路由。</p>
     *
     * <p>当容器中已存在同类型 Bean 时，该方法不会重复注册。</p>
     *
     * @return EventPublisherHub 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(EventPublisherHub.class)
    public EventPublisherHub eventPublisherHub(ObjectProvider<StreamBridge> streamBridgeProvider,
                                               ObjectProvider<BindingServiceProperties> bindingServicePropertiesProvider) {
        StreamBridge streamBridge = streamBridgeProvider.getIfAvailable();
        BindingServiceProperties bindingServiceProperties = bindingServicePropertiesProvider.getIfAvailable();
        if (Objects.isNull(streamBridge) || Objects.isNull(bindingServiceProperties)) {
            // 未启用 Stream（或 StreamBridge 尚不可用）时，不强行失败：返回空 Hub，让业务可在无消息总线场景下照常启动。
            return new EventPublisherHub(Map.of());
        }

        Map<String, EventPublisher> publishers = new LinkedHashMap<>();
        // outbound bindings 仅来自显式配置（outputBindings）或可推断的 bindings；避免“扫全容器通道”带来的误发送风险。
        Set<String> outboundBindings = resolveOutboundBindings(bindingServiceProperties);
        for (String bindingName : outboundBindings) {
            publishers.put(bindingName, new StreamBridgeEventPublisher(bindingName, streamBridge));
        }
        return new EventPublisherHub(publishers);
    }

    /**
     * 解析应用配置中的 outbound binding 名称集合。
     *
     * <p>优先使用 {@link BindingServiceProperties#getOutputBindings()}；若未配置，则从
     * {@link BindingServiceProperties#getBindings()} 中根据 producer 配置或命名约定推断。</p>
     */
    private Set<String> resolveOutboundBindings(BindingServiceProperties bindingServiceProperties) {
        String configuredOutputBindings = bindingServiceProperties.getOutputBindings();
        if (Strings.isNotBlank(configuredOutputBindings)) {
            return configuredBindingNames(configuredOutputBindings);
        }

        Map<String, BindingProperties> bindings = bindingServiceProperties.getBindings();
        if (Collections.isEmpty(bindings)) {
            return Set.of();
        }

        // 兼容简化配置：当应用只声明了 bindings 但未显式声明 output-bindings 时，
        // 尽量在“不误发”的前提下推断 outbound：
        // - 若配置了 input-bindings，则 outbound = bindings - inputBindings
        // - 若未配置 input-bindings 且只有一个 binding，则将其视为 outbound（典型的单向发布场景）
        String configuredInputBindings = bindingServiceProperties.getInputBindings();
        if (Strings.isNotBlank(configuredInputBindings)) {
            Set<String> inputs = configuredBindingNames(configuredInputBindings);
            return bindings.keySet().stream()
                .filter(name -> !inputs.contains(name))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
        if (bindings.size() == 1) {
            return new LinkedHashSet<>(bindings.keySet());
        }

        return bindings.entrySet().stream()
            .filter(entry -> isOutboundBinding(entry.getKey(), entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 将 {@link BindingServiceProperties#getOutputBindings()} 配置的逗号分隔列表解析为集合。
     *
     * <p>会执行 {@code trim} 并忽略空项，返回集合保持声明顺序。</p>
     *
     * @param configuredBindings 形如 {@code "a-out-0,b-out-0"} 的字符串
     * @return bindingName 集合（有序、去重）
     */
    private Set<String> configuredBindingNames(String configuredBindings) {
        return java.util.Arrays.stream(configuredBindings.split(","))
            .map(String::trim)
            .filter(Strings::isNotBlank)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 判断一个 binding 是否应视为 outbound。
     *
     * <p>以 producer 配置为准；若无法获取方向信息，则按命名约定推断（例如 {@code *-out-0}）。</p>
     */
    private boolean isOutboundBinding(String bindingName, BindingProperties bindingProperties) {
        if (Objects.nonNull(bindingProperties) && Objects.nonNull(bindingProperties.getProducer())) {
            return true;
        }

        // 无法可靠获取方向信息时，才使用命名约定做兜底推断（属于启发式规则，非强保证）。
        return bindingName.endsWith("-out-0") || bindingName.contains("-out-");
    }

    /**
     * 注册 {@link SyncerInitializer} Bean。
     *
     * <p>该 Bean 用于在 Spring 容器中所有单例 Bean 初始化完成后，触发
     * {@link SyncerInitializer#afterSingletonsInstantiated()} 方法执行缓存加载。</p>
     *
     * <p>特点与注意事项：</p>
     * <ul>
     *     <li>仅在 Spring 容器中不存在同类型 Bean 时才注册，避免重复定义。</li>
     *     <li>属于基础设施 Bean，标注 {@link BeanDefinition#ROLE_INFRASTRUCTURE}。</li>
     *     <li>负责首次缓存初始化，不干涉业务微服务后续的 reload 或缓存操作。</li>
     * </ul>
     *
     * @return SyncerInitializer 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(SyncerInitializer.class)
    public SyncerInitializer syncerInitializer() {
        return new SyncerInitializer();
    }
}
