package com.g2rain.department.principal.support;

import com.g2rain.common.model.Result;
import com.g2rain.department.principal.DepartmentPrincipalClient;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

/**
 * 基于 Spring HTTP Interface 的部门 Principal 增强客户端实现。
 */
public interface DepartmentPrincipalRestClient extends DepartmentPrincipalClient {

    /**
     * 获取 Principal 增强信息。
     *
     * @param organId 机构标识
     * @param userId  用户标识
     * @return 部门路径响应
     */
    @Override
    @GetExchange("${g2rain.principal.department.path-business:/principal_enrichment}")
    Result<String> getPrincipalEnrichment(@RequestParam("organId") Long organId, @RequestParam("userId") Long userId);
}
