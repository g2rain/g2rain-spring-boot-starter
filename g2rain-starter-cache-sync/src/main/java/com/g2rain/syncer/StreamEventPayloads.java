package com.g2rain.syncer;

import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecFactory;
import com.g2rain.common.syncer.EventMessage;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Stream 事件消息载荷工具。
 *
 * <p>统一 cache-sync 在 Stream 层传输的原始消息格式：
 * 整体消息始终是 JSON 字符串，且 {@code EventMessage.data} 固定序列化为 JSON 字符串，
 * 与 {@code common.syncer.DefaultMessageDispatcher} 的解析约定保持一致。</p>
 *
 * <p><b>Wire format 约定：</b></p>
 * <ul>
 *     <li>Stream 传输层 payload 推荐为 {@link String}（JSON 文本）或其 UTF-8 {@code byte[]}</li>
 *     <li>外层结构固定为 {@code EventMessage<String>} 的 JSON</li>
 *     <li>{@code data} 字段为“字符串化的 JSON”（即内层对象序列化后放入字符串字段），便于 dispatcher 二段解析</li>
 * </ul>
 *
 * @author alpha
 * @since 2026/4/14
 */
public final class StreamEventPayloads {

    /**
     * cache-sync 在 Stream 层使用的 JSON 编解码器。
     */
    private static final JsonCodec JSON_CODEC = JsonCodecFactory.instance();

    /**
     * 工具类禁止实例化。
     */
    private StreamEventPayloads() {
    }

    /**
     * 将任意类型事件消息归一化为 dispatcher 可直接消费的原始 JSON 字符串。
     *
     * <p>返回值为外层 {@code EventMessage<String>} 的 JSON 字符串，其中 {@code data} 是被序列化后的 JSON 字符串。</p>
     */
    public static <V> String toRawMessage(EventMessage<V> eventMessage) {
        if (Objects.isNull(eventMessage)) {
            return null;
        }

        EventMessage<String> serializedMessage = new EventMessage<>(
            eventMessage.getDataSource(),
            eventMessage.getEventType(),
            // data 固定序列化为 JSON 字符串(而不是嵌套对象), 以匹配 DefaultMessageDispatcher 的"外层先解析元数据、再二段解析 data"的约定
            JSON_CODEC.obj2str(eventMessage.getData())
        );

        return JSON_CODEC.obj2str(serializedMessage);
    }

    /**
     * 从 Stream Message 中提取 dispatcher 可消费的原始 JSON 字符串。
     *
     * <p>提取规则委托给 {@link #extractRawPayload(Object)}。</p>
     */
    public static String extractRawMessage(Message<?> message) {
        if (Objects.isNull(message)) {
            return null;
        }

        return extractRawPayload(message.getPayload());
    }

    /**
     * 从原始 payload 中提取 dispatcher 可消费的原始 JSON 字符串。
     *
     * <p>策略：</p>
     * <ul>
     *     <li>{@link String}：原样返回</li>
     *     <li>{@code byte[]}：按 UTF-8 解码为字符串</li>
     *     <li>{@link EventMessage}：按 {@link #toRawMessage(EventMessage)} 归一化</li>
     *     <li>其他对象：使用 JSON 编码为字符串（适用于测试或非 StreamBridge 发送场景）</li>
     * </ul>
     */
    public static String extractRawPayload(Object payload) {
        if (Objects.isNull(payload)) {
            return null;
        }

        return switch (payload) {
            case String text -> text;
            case byte[] b -> new String(
                b, StandardCharsets.UTF_8
            );
            case EventMessage<?> em -> toRawMessage(em);
            default -> JSON_CODEC.obj2str(payload);
        };
    }
}
