package com.g2rain.stream.redis.binder;

import lombok.NonNull;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.stream.binder.AbstractBinder;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.DefaultBinding;
import org.springframework.cloud.stream.binder.EmbeddedHeaderUtils;
import org.springframework.cloud.stream.binder.HeaderMode;
import org.springframework.cloud.stream.binder.MessageValues;
import org.springframework.cloud.stream.binder.PartitionHandler;
import org.springframework.cloud.stream.binder.ProducerProperties;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.codec.Codec;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 实现的 Stream Binder。
 * <p>
 * 设计上延续 spring-cloud-stream-binder-redis 的核心语义：
 * destination + group 的队列隔离、分区路由、并发消费与错误队列降级。
 * </p>
 *
 * <p>核心机制：</p>
 * <ul>
 *     <li>Producer：基于 {@code groups.<destination>} 的 group 集合进行 fan-out</li>
 *     <li>Consumer：绑定到 {@code destination.group} 队列，分区场景追加 {@code -instanceIndex}</li>
 *     <li>HeaderMode：仅支持 embeddedHeaders（byte[] + embedded headers）</li>
 *     <li>失败重试：当 {@code maxAttempts > 1} 启用重试，耗尽后转入 {@code ERRORS:<queue>}</li>
 * </ul>
 *
 * @author alpha
 * @since 2026/4/13
 */
public class G2rainRedisMessageChannelBinder extends AbstractBinder<MessageChannel, ConsumerProperties, ProducerProperties> {

    /**
     * 错误队列头字段，值为 ERRORS:<queueName>。
     */
    private static final String ERROR_HEADER = "errorKey";

    /**
     * Redis ZSET 前缀，用于维护 destination 下有效 group。
     */
    private static final String GROUPS_KEY_PREFIX = "groups.";

    /**
     * 分区队列路由表达式解析器。
     */
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    /**
     * 生产端 embedded headers 模式需要打包的头字段集合。
     */
    private final String[] headersToMap;

    /**
     * Redis 操作入口（用于 group ZSET 管理）。
     */
    private final RedisOperations<String, String> redisOperations;

    /**
     * Redis 连接工厂（用于创建发送/接收端点）。
     */
    private final RedisConnectionFactory connectionFactory;

    /**
     * 重试耗尽后的错误消息写入端点。
     */
    private final RedisQueueOutboundChannelAdapter errorAdapter;

    /**
     * 可选 Codec，用于非 byte[] payload 的编码。
     */
    private final Codec codec;

    /**
     * consumer 未配置 group 时的模式。
     */
    private final String noGroupConsumerMode;

    /**
     * 创建 Binder（不指定 codec）。
     *
     * @param connectionFactory Redis 连接工厂
     * @param headersToMap      需要额外打包的 header
     */
    public G2rainRedisMessageChannelBinder(RedisConnectionFactory connectionFactory, String... headersToMap) {
        this(connectionFactory, null, "pubsub", headersToMap);
    }

    /**
     * 创建 Binder。
     *
     * @param connectionFactory Redis 连接工厂
     * @param codec             可选编码器（用于非 byte[] 负载编码）
     * @param headersToMap      需要额外打包的 header
     */
    public G2rainRedisMessageChannelBinder(RedisConnectionFactory connectionFactory, Codec codec, String noGroupConsumerMode, String... headersToMap) {
        Assert.notNull(connectionFactory, "connectionFactory must not be null");
        this.connectionFactory = connectionFactory;
        this.codec = codec;
        this.noGroupConsumerMode = (noGroupConsumerMode == null ? "pubsub" : noGroupConsumerMode);
        StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
        template.afterPropertiesSet();
        this.redisOperations = template;
        this.headersToMap = mergeHeaders(headersToMap);
        this.errorAdapter = new RedisQueueOutboundChannelAdapter(
            PARSER.parseExpression("headers['" + ERROR_HEADER + "']"), connectionFactory
        );
    }

    @Override
    protected void onInit() {
        // 错误端点需要显式绑定 evaluationContext 与 BeanFactory，否则表达式路由无法生效。
        this.errorAdapter.setIntegrationEvaluationContext(this.getEvaluationContext());
        this.errorAdapter.setBeanFactory(this.getBeanFactory());
        this.errorAdapter.afterPropertiesSet();
    }

    @Override
    protected Binding<MessageChannel> doBindConsumer(String name, String group, MessageChannel moduleInputChannel, ConsumerProperties properties) {
        if (!StringUtils.hasText(group)) {
            if ("anon-queue".equalsIgnoreCase(noGroupConsumerMode)) {
                group = "anon-" + UUID.randomUUID();
            } else {
                MessageProducerSupport adapter = createPubSubInboundAdapter(name);
                return doRegisterAnonymousConsumer(name, name, moduleInputChannel, adapter, properties);
            }
        }

        String queueName = groupedName(name, group);
        // 分区消费通过 instanceIndex 绑定到对应队列分片。
        if (properties.isPartitioned()) {
            queueName = queueName + "-" + properties.getInstanceIndex();
        }

        MessageProducerSupport adapter = createInboundAdapter(properties, queueName);
        return doRegisterConsumer(name, group, queueName, moduleInputChannel, adapter, properties);
    }

    @Override
    protected Binding<MessageChannel> doBindProducer(String name, MessageChannel moduleOutputChannel, ProducerProperties properties) {
        Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
        return doRegisterProducer(name, moduleOutputChannel, properties);
    }

    private MessageProducerSupport createInboundAdapter(ConsumerProperties accessor, String queueName) {
        int concurrency = Math.max(accessor.getConcurrency(), 1);
        if (concurrency == 1) {
            RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(queueName, this.connectionFactory);
            endpoint.setBeanFactory(this.getBeanFactory());
            endpoint.setSerializer(RedisSerializer.byteArray());
            return endpoint;
        }

        return new CompositeRedisQueueMessageDrivenEndpoint(queueName, concurrency);
    }

    private MessageProducerSupport createPubSubInboundAdapter(String topicName) {
        RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(this.connectionFactory);
        adapter.setBeanFactory(this.getBeanFactory());
        adapter.setSerializer(RedisSerializer.byteArray());
        adapter.setTopics(topicName);
        return adapter;
    }

    private Binding<MessageChannel> doRegisterAnonymousConsumer(String bindingName, String topicName,
                                                                MessageChannel moduleInputChannel,
                                                                MessageProducerSupport adapter,
                                                                ConsumerProperties properties) {
        DirectChannel bridgeToModuleChannel = new DirectChannel();
        bridgeToModuleChannel.setBeanFactory(this.getBeanFactory());
        bridgeToModuleChannel.setBeanName(topicName + ".bridge");

        MessageChannel bridgeInputChannel = addRetryIfNeeded(topicName, bridgeToModuleChannel, properties);
        adapter.setOutputChannel(bridgeInputChannel);
        adapter.setBeanName("inbound.pubsub." + topicName);
        adapter.afterPropertiesSet();

        ReceivingHandler convertingBridge = new ReceivingHandler(properties);
        convertingBridge.setOutputChannel(moduleInputChannel);
        convertingBridge.setBeanName(topicName + ".bridge.handler");
        convertingBridge.afterPropertiesSet();
        bridgeToModuleChannel.subscribe(convertingBridge);

        adapter.start();
        return new DefaultBinding<>(bindingName, null, moduleInputChannel, adapter);
    }

    // 说明：
    // - noGroupConsumerMode=pub/sub：无 group 时消费端走 PUB/SUB（广播；不落地）
    // - noGroupConsumerMode=anon-queue：无 group 时生成匿名队列（可靠广播；会产生临时队列）

    /**
     * 注册 consumer 绑定并完成桥接通道、重试通道、header 解包处理器的装配。
     */
    private Binding<MessageChannel> doRegisterConsumer(String bindingName, String group, String queueName,
                                                       MessageChannel moduleInputChannel, MessageProducerSupport adapter,
                                                       ConsumerProperties properties) {
        DirectChannel bridgeToModuleChannel = new DirectChannel();
        bridgeToModuleChannel.setBeanFactory(this.getBeanFactory());
        bridgeToModuleChannel.setBeanName(queueName + ".bridge");

        MessageChannel bridgeInputChannel = addRetryIfNeeded(queueName, bridgeToModuleChannel, properties);
        adapter.setOutputChannel(bridgeInputChannel);
        adapter.setBeanName("inbound." + queueName);
        adapter.afterPropertiesSet();

        DefaultBinding<MessageChannel> consumerBinding = new DefaultBinding<>(bindingName, group, moduleInputChannel, adapter) {
            @Override
            protected void afterUnbind() {
                // unbind 时回收 group 活跃计数，避免 producer 长期向失效 group fan-out。
                Double newScore = redisOperations.boundZSetOps(GROUPS_KEY_PREFIX + getName()).incrementScore(getGroup(), -1);
                if (newScore != null && newScore <= 0) {
                    redisOperations.boundZSetOps(GROUPS_KEY_PREFIX + getName()).remove(getGroup());
                }
            }
        };

        ReceivingHandler convertingBridge = new ReceivingHandler(properties);
        convertingBridge.setOutputChannel(moduleInputChannel);
        convertingBridge.setBeanName(queueName + ".bridge.handler");
        convertingBridge.afterPropertiesSet();
        bridgeToModuleChannel.subscribe(convertingBridge);

        this.redisOperations.boundZSetOps(GROUPS_KEY_PREFIX + bindingName).incrementScore(group, 1);
        adapter.start();
        return consumerBinding;
    }

    /**
     * 根据 consumer 重试配置决定是否包装重试通道。
     */
    private MessageChannel addRetryIfNeeded(final String name, final DirectChannel bridgeToModuleChannel,
                                            ConsumerProperties properties) {
        if (properties.getMaxAttempts() <= 1) {
            return bridgeToModuleChannel;
        }

        RetryTemplate retryTemplate = buildRetryTemplate(properties);
        DirectChannel retryChannel = new DirectChannel() {
            @Override
            protected boolean doSend(@NonNull final Message<?> message, final long timeout) {
                try {
                    return retryTemplate.execute(() -> bridgeToModuleChannel.send(message, timeout));
                } catch (Exception ex) {
                    logger.error(ex, "Failed to deliver message; sent to ERRORS:" + name);
                    // 重试耗尽后将消息写入 ERRORS 队列，保持与旧 binder 的降级行为一致。
                    errorAdapter.handleMessage(
                        MessageBuilder.fromMessage(message).setHeader(ERROR_HEADER, "ERRORS:" + name).build()
                    );
                    return false;
                }
            }
        };
        retryChannel.setBeanFactory(this.getBeanFactory());
        retryChannel.setBeanName(name + ".retry.bridge");
        return retryChannel;
    }

    private Binding<MessageChannel> doRegisterProducer(final String name, MessageChannel moduleOutputChannel,
                                                       ProducerProperties properties) {
        MessageHandler handler = new SendingHandler(name, properties);
        EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) moduleOutputChannel, handler);
        consumer.setBeanFactory(this.getBeanFactory());
        consumer.setBeanName("outbound." + name);
        consumer.afterPropertiesSet();

        DefaultBinding<MessageChannel> producerBinding = new DefaultBinding<>(name, null, moduleOutputChannel, consumer);
        String[] requiredGroups = properties.getRequiredGroups();
        if (!ObjectUtils.isEmpty(requiredGroups)) {
            // requiredGroups 预注册，确保消费者未先启动时也能被 producer 感知。
            for (String group : requiredGroups) {
                this.redisOperations.boundZSetOps(GROUPS_KEY_PREFIX + name).incrementScore(group, 1);
            }
        }

        consumer.start();
        return producerBinding;
    }

    private RedisQueueOutboundChannelAdapter createProducerEndpoint(String name, ProducerProperties properties) {
        RedisQueueOutboundChannelAdapter queue;
        if (!properties.isPartitioned()) {
            queue = new RedisQueueOutboundChannelAdapter(name, this.connectionFactory);
        } else {
            // 分区路由：destination.group-<partition>
            queue = new RedisQueueOutboundChannelAdapter(
                PARSER.parseExpression("'" + name + "-' + headers['" + BinderHeaders.PARTITION_HEADER + "']"),
                this.connectionFactory
            );
        }
        queue.setIntegrationEvaluationContext(this.getEvaluationContext());
        queue.setBeanFactory(this.getBeanFactory());
        queue.afterPropertiesSet();
        return queue;
    }

    private String[] mergeHeaders(String[] extraHeaders) {
        if (ObjectUtils.isEmpty(extraHeaders)) {
            return BinderHeaders.STANDARD_HEADERS;
        }

        Set<String> merged = new HashSet<>(Arrays.asList(BinderHeaders.STANDARD_HEADERS));
        merged.addAll(Arrays.asList(extraHeaders));
        return merged.toArray(new String[0]);
    }

    /**
     * 将 payload 统一转为 byte[]。
     * <p>优先使用 codec 编码，失败时降级为 UTF-8 字符串字节。</p>
     */
    private byte[] payloadToBytes(Object payload) {
        if (payload instanceof byte[] bytes) {
            return bytes;
        }

        if (Objects.isNull(codec)) {
            throw new MessageConversionException(
                "Redis binder only supports byte[] payload (embedded headers). " +
                    "If you need POJO/String payload, provide an explicit Codec bean. " +
                    "payloadType=" + (payload == null ? "null" : payload.getClass().getName())
            );
        }

        try {
            return codec.encode(payload);
        } catch (Exception ex) {
            logger.error("Codec encode failed; payloadType=" + (payload == null ? "null" : payload.getClass().getName()), ex);
            throw new MessageConversionException(
                "Codec encode failed for payloadType=" + (payload == null ? "null" : payload.getClass().getName()), ex
            );
        }
    }

    private class SendingHandler extends AbstractMessageHandler {

        private final String bindingName;
        private final ProducerProperties producerProperties;
        private final Map<String, RedisQueueOutboundChannelAdapter> adapters = new ConcurrentHashMap<>();
        private final PartitionHandler partitionHandler;
        private final RedisPublishingMessageHandler pubSubPublisher;

        private SendingHandler(String bindingName, ProducerProperties producerProperties) {
            this.bindingName = bindingName;
            this.producerProperties = producerProperties;
            ConfigurableListableBeanFactory beanFactory = G2rainRedisMessageChannelBinder.this.getBeanFactory();
            this.partitionHandler = new PartitionHandler(
                getEvaluationContext(), producerProperties, Objects.requireNonNull(beanFactory, "beanFactory must not be null")
            );
            this.setBeanFactory(beanFactory);
            this.pubSubPublisher = new RedisPublishingMessageHandler(connectionFactory);
            this.pubSubPublisher.setBeanFactory(beanFactory);
            this.pubSubPublisher.setIntegrationEvaluationContext(getEvaluationContext());
            this.pubSubPublisher.setSerializer(RedisSerializer.byteArray());
            this.pubSubPublisher.setTopic(this.bindingName);
            this.pubSubPublisher.afterPropertiesSet();
            refreshChannelAdapters();
        }

        @Override
        protected void handleMessageInternal(@NonNull Message<?> message) {
            MessageValues transformed = new MessageValues(message);
            if (producerProperties.isPartitioned()) {
                transformed.put(BinderHeaders.PARTITION_HEADER, this.partitionHandler.determinePartition(message));
            }

            byte[] payload;
            if (producerProperties.getHeaderMode() == HeaderMode.embeddedHeaders) {
                // embedded 模式：payload + headers 打包为一个 byte[]。
                payload = payloadToBytes(transformed.getPayload());
                transformed.setPayload(payload);
                payload = EmbeddedHeaderUtils.embedHeaders(transformed, headersToMap);
            } else {
                payload = payloadToBytes(transformed.getPayload());
            }

            refreshChannelAdapters();
            Message<byte[]> messageToSend = MessageBuilder.withPayload(payload).copyHeaders(transformed).build();

            // 广播模式：无 group 消费者通过 Redis PUB/SUB topic 订阅 destination。
            // 即使当前没有订阅者，发布也不会产生副作用。
            pubSubPublisher.handleMessage(messageToSend);

            for (RedisQueueOutboundChannelAdapter adapter : adapters.values()) {
                adapter.handleMessage(messageToSend);
            }
        }

        /**
         * 按 destination 的活跃 group 刷新并缓存发送端点。
         */
        private void refreshChannelAdapters() {
            Set<String> groups = redisOperations.boundZSetOps(GROUPS_KEY_PREFIX + bindingName).rangeByScore(1, Double.MAX_VALUE);
            if (ObjectUtils.isEmpty(groups)) {
                adapters.clear();
                return;
            }

            for (String existing : new HashSet<>(adapters.keySet())) {
                if (!groups.contains(existing)) {
                    adapters.remove(existing);
                }
            }

            for (String group : groups) {
                adapters.computeIfAbsent(group, g -> {
                    String channel = this.bindingName + "." + g;
                    return createProducerEndpoint(channel, producerProperties);
                });
            }
        }
    }

    private class ReceivingHandler extends AbstractReplyProducingMessageHandler {

        private final ConsumerProperties consumerProperties;

        private ReceivingHandler(ConsumerProperties consumerProperties) {
            this.consumerProperties = consumerProperties;
            ConfigurableListableBeanFactory beanFactory = G2rainRedisMessageChannelBinder.this.getBeanFactory();
            this.setBeanFactory(Objects.requireNonNull(beanFactory, "beanFactory must not be null"));
        }

        @Override
        protected Object handleRequestMessage(@NonNull Message<?> requestMessage) {
            Object payload = requestMessage.getPayload();
            if (!(payload instanceof byte[] bytes)) {
                return requestMessage;
            }

            // 兼容两种情况：
            // 1) headerMode=embeddedHeaders：按约定解包
            // 2) headerMode!=embeddedHeaders，但上游仍发送了 embedded 格式：尽量解包成功则闭环，否则原样返回
            try {
                MessageValues messageValues = EmbeddedHeaderUtils.extractHeaders(bytes);
                return messageValues.toMessage(getMessageBuilderFactory());
            } catch (Exception ex) {
                if (consumerProperties.getHeaderMode() == HeaderMode.embeddedHeaders) {
                    logger.warn(ex, "Failed to extract embedded headers, fallback to original message");
                }
                return requestMessage;
            }
        }

        @Override
        protected boolean shouldCopyRequestHeaders() {
            return false;
        }
    }

    /**
     * 并发消费端点，内部维护多个 RedisQueueMessageDrivenEndpoint。
     */
    private class CompositeRedisQueueMessageDrivenEndpoint extends MessageProducerSupport {

        private final List<RedisQueueMessageDrivenEndpoint> consumers = new ArrayList<>();

        private CompositeRedisQueueMessageDrivenEndpoint(String queueName, int concurrency) {
            for (int i = 0; i < concurrency; i++) {
                RedisQueueMessageDrivenEndpoint endpoint = new RedisQueueMessageDrivenEndpoint(
                    queueName, connectionFactory
                );
                endpoint.setBeanFactory(getBeanFactory());
                endpoint.setSerializer(RedisSerializer.byteArray());
                endpoint.setBeanName("inbound." + queueName + "." + i);
                this.consumers.add(endpoint);
            }

            this.setBeanFactory(getBeanFactory());
        }

        @Override
        protected void onInit() {
            for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
                consumer.afterPropertiesSet();
            }
        }

        @Override
        protected void doStart() {
            for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
                consumer.start();
            }
        }

        @Override
        protected void doStop() {
            for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
                consumer.stop();
            }
        }

        @Override
        public void setOutputChannel(@NonNull MessageChannel outputChannel) {
            for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
                consumer.setOutputChannel(outputChannel);
            }
        }

        @Override
        public void setErrorChannel(@NonNull MessageChannel errorChannel) {
            for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
                consumer.setErrorChannel(errorChannel);
            }
        }
    }
}
