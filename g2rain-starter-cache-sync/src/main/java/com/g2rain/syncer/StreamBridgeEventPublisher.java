package com.g2rain.syncer;

import com.g2rain.common.syncer.EventMessage;
import com.g2rain.common.syncer.EventPublisher;
import org.springframework.cloud.stream.function.StreamBridge;

import java.util.Objects;

/**
 * 基于 {@link StreamBridge} 的事件发布器。
 * <p>
 * 该实现按 bindingName 发送消息，适配 Spring Cloud Stream 函数式绑定模型，
 * 避免依赖容器中的 {@code MessageChannel} Bean 命名。
 * </p>
 *
 * <p><b>payload 约定：</b>发布时会将 {@link EventMessage} 归一化为 dispatcher 可消费的 raw JSON 字符串，
 * 详见 {@link StreamEventPayloads#toRawMessage(EventMessage)}。</p>
 *
 * @author alpha
 * @since 2026/4/13
 */
public class StreamBridgeEventPublisher implements EventPublisher {

    /**
     * 目标 outbound binding 名称（例如 {@code sync-out-0}）。
     */
    private final String bindingName;

    /**
     * StreamBridge 发送入口。
     */
    private final StreamBridge streamBridge;

    /**
     * 创建一个按 bindingName 定向发送的发布器。
     *
     * @param bindingName  outbound binding 名称（例如 {@code sync-out-0}）
     * @param streamBridge StreamBridge 实例
     */
    public StreamBridgeEventPublisher(String bindingName, StreamBridge streamBridge) {
        this.bindingName = bindingName;
        this.streamBridge = streamBridge;
    }

    /**
     * 将事件消息发布到指定 outbound binding。
     *
     * <p>消息会被归一化为 raw JSON 字符串；若事件为空或归一化结果为空白，则不会发送。</p>
     */
    @Override
    public <V> void publish(EventMessage<V> eventMessage) {
        String rawMessage = StreamEventPayloads.toRawMessage(eventMessage);
        if (Objects.isNull(rawMessage) || rawMessage.isBlank()) {
            return;
        }

        streamBridge.send(bindingName, rawMessage);
    }
}
