package com.g2rain.stream.redis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis Binder 扩展配置。
 * <p>
 * 该类与 {@code spring.cloud.stream.redis.binder.*} 配置前缀绑定，
 * 只承载 Binder 级扩展参数，不覆盖 Spring Cloud Stream 的标准 binding 参数。
 * </p>
 *
 * @author alpha
 * @since 2026/4/13
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.cloud.stream.redis.binder")
public class G2rainRedisBinderProperties {

    /**
     * 需要额外内嵌到 payload 的 header 列表。
     * <p>
     * 实际生效集合 = {@code BinderHeaders.STANDARD_HEADERS + headers}。
     * </p>
     */
    private String[] headers = new String[0];

    /**
     * Consumer 在未配置 group 时的语义。
     * <ul>
     *   <li>{@code pubsub}：使用 Redis PUB/SUB（广播；不落地，晚启动会丢消息）</li>
     *   <li>{@code anon-queue}：为每个实例生成匿名 group，使用队列模式（可靠广播；会产生临时队列）</li>
     * </ul>
     */
    private String noGroupConsumerMode = "pubsub";
}
