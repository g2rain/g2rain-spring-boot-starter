package com.g2rain.department.principal.support;

import com.g2rain.common.model.Result;
import com.g2rain.department.principal.DepartmentPrincipalClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 基于 OpenFeign 的部门 Principal 增强客户端实现。
 */
@FeignClient(name = "${g2rain.principal.department.service-name:g2rain-department}", contextId = "departmentPrincipalClient", url = "${g2rain.principal.department.service-url:}", path = "${g2rain.principal.department.service-path:department_user_relation}")
public interface DepartmentPrincipalOpenFeign extends DepartmentPrincipalClient {

    /**
     * 获取 Principal 增强信息。
     *
     * @param organId 机构标识
     * @param userId  用户标识
     * @return 部门路径响应
     */
    @Override
    @GetMapping("${g2rain.principal.department.path-business:/principal_enrichment}")
    Result<String> getPrincipalEnrichment(@RequestParam("organId") Long organId, @RequestParam("userId") Long userId);
}
