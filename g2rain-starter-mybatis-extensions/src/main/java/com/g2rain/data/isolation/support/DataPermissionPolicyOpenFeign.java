package com.g2rain.data.isolation.support;

import com.g2rain.common.model.Result;
import com.g2rain.data.isolation.DataPermissionPolicyClient;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 基于 OpenFeign 的数据权限策略客户端实现。
 * <p>
 * 该接口继承通用客户端抽象 {@link DataPermissionPolicyClient}，用于在引入 Feign 场景下
 * 自动装配远程策略解析能力。
 * </p>
 */
@FeignClient(
    name = "${g2rain.data.isolation.policy-service-name:g2rain-department}",
    contextId = "dataPermissionPolicyClient",
    url = "${g2rain.data.isolation.policy-service-url:}",
    path = "${g2rain.data.isolation.policy-service-path:data_permission_meta}"
)
public interface DataPermissionPolicyOpenFeign extends DataPermissionPolicyClient {

    @Override
    @GetMapping("${g2rain.data.isolation.policy-resolve-path:/policy_resolve}")
    Result<DataPermissionPolicyResolveResult> resolveDataPermissionPolicy(
        @RequestParam("organId") Long organId,
        @RequestParam("userId") Long userId,
        @RequestParam("deptPaths") String deptPaths,
        @RequestParam("moduleCode") String moduleCode,
        @RequestParam("tableName") String tableName
    );
}
