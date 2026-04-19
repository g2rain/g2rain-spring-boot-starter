package com.g2rain.data.isolation;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>数据隔离 SQL 处理器配置类，用于绑定 Spring Boot 配置属性。</p>
 * <p>配置前缀为 {@code g2rain.data.isolation}。</p>
 * <p>使用示例：</p>
 * <pre>{@code
 * @Autowired
 * private IsolationProperties properties;
 *
 * boolean enabled = properties.isEnabled();
 * }</pre>
 *
 * @author alpha
 * @since 2025/10/13
 */
@Data
@ConfigurationProperties(prefix = "g2rain.data.isolation")
public class IsolationProperties {

    /**
     * <p>是否启用数据隔离拦截。</p>
     * <p>{@code true} 表示启用，{@code false} 表示禁用。</p>
     */
    private boolean enabled = true;


    /**
     * 目标微服务的名称，用于注册中心发现。默认 g2rain-basis。
     * 对应占位符：${g2rain.data.isolation.service-name}
     */
    private String serviceName = "g2rain-basis";

    /**
     * 目标微服务的绝对 URL（用于开发环境直连，不走注册中心）。默认空。
     * 对应占位符：${g2rain.data.isolation.service-url}
     */
    private String serviceUrl = "";

    /**
     * 接口的基础路径，默认 organ。
     * 对应占位符：${g2rain.data.isolation.service-path}
     */
    private String servicePath = "organ";

    /**
     * 具体检查层级关系的业务路径。
     * 对应占位符：${g2rain.data.isolation.path-business}
     */
    private String pathBusiness = "/hierarchy/exists";
}
