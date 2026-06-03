package com.g2rain.department.principal;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 部门 Principal 增强配置。
 */
@Data
@ConfigurationProperties(prefix = "g2rain.principal.department")
public class DepartmentPrincipalProperties {

    /**
     * 是否启用部门 Principal 增强。
     */
    private boolean enabled = true;

    /**
     * 目标微服务名称。
     */
    private String serviceName = "g2rain-department";

    /**
     * 目标微服务绝对地址，配置后覆盖 serviceName。
     */
    private String serviceUrl = "";

    /**
     * 接口基础路径。
     */
    private String servicePath = "department_user_relation";

    /**
     * Principal 增强信息接口路径。
     */
    private String pathBusiness = "/principal_enrichment";
}
