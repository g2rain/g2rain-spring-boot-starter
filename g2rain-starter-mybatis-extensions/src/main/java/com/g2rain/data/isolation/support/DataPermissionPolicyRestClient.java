package com.g2rain.data.isolation.support;

import com.g2rain.common.model.Result;
import com.g2rain.data.isolation.DataPermissionPolicyClient;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

/**
 * 基于 Spring HTTP Interface 的数据权限策略客户端实现。
 * <p>
 * 当工程未启用 OpenFeign 时，自动配置会回退为该实现。
 * </p>
 */
public interface DataPermissionPolicyRestClient extends DataPermissionPolicyClient {

    @Override
    @GetExchange("${g2rain.data.isolation.policy-resolve-path:/policy_resolve}")
    Result<DataPermissionPolicyResolveResult> resolveDataPermissionPolicy(
        @RequestParam("organId") Long organId,
        @RequestParam("userId") Long userId,
        @RequestParam("deptPaths") String deptPaths,
        @RequestParam("moduleCode") String moduleCode,
        @RequestParam("tableName") String tableName
    );
}
