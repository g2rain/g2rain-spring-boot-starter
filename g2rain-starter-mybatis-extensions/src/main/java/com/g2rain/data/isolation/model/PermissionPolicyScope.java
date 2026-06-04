package com.g2rain.data.isolation.model;

import com.g2rain.data.isolation.enums.PolicyInvalidationLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 数据权限策略缓存失效范围。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PermissionPolicyScope {

    private Long organId;

    private Long userId;

    private String deptPaths;

    private String moduleCode;

    private String tableName;

    private PolicyInvalidationLevel level;

    public static PermissionPolicyScope exact(Long organId, Long userId, String deptPaths, String moduleCode, String tableName) {
        return new PermissionPolicyScope(organId, userId, deptPaths, moduleCode, tableName, PolicyInvalidationLevel.EXACT);
    }

    public static PermissionPolicyScope organUser(Long organId, Long userId, String deptPaths) {
        return new PermissionPolicyScope(organId, userId, deptPaths, null, null, PolicyInvalidationLevel.ORGAN_USER);
    }

    public static PermissionPolicyScope organModel(Long organId, String moduleCode, String tableName) {
        return new PermissionPolicyScope(organId, null, null, moduleCode, tableName, PolicyInvalidationLevel.ORGAN_MODEL);
    }
}
