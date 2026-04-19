package com.g2rain.identity;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author alpha
 * @since 2025/12/30
 */
@Data
@ConfigurationProperties(prefix = "g2rain.id.generator")
public class IdGeneratorProperties {

    /**
     * Feign Client name
     */
    private String serviceName = "g2rain-infra";

    /**
     * ID 服务 URL
     */
    private String serviceUrl = "";

    /**
     * 服务路径
     */
    private String servicePath = "g2rain_raindrop";

    /**
     * 雪花 ID 路径
     */
    @SuppressWarnings({"S10755"})
    private String pathSnowflake = "/snowflake";

    /**
     * Segment ID 路径
     */
    @SuppressWarnings({"S10755"})
    private String pathBusiness = "/business";
}
