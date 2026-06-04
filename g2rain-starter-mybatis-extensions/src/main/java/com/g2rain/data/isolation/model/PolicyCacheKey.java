package com.g2rain.data.isolation.model;

import com.g2rain.data.isolation.enums.PolicyInvalidationLevel;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 数据权限策略本地缓存键。
 */
public record PolicyCacheKey(Long organId, Long userId, String deptPaths, String moduleCode, String tableName) {

    public PolicyCacheKey {
        Objects.requireNonNull(organId, "organId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(deptPaths, "deptPaths");
        Objects.requireNonNull(moduleCode, "moduleCode");
        Objects.requireNonNull(tableName, "tableName");
    }

    public static PolicyCacheKey of(Long organId, Long userId, String deptPaths, String moduleCode, String tableName) {
        return new PolicyCacheKey(organId, userId, deptPaths, moduleCode, tableName);
    }

    public boolean matches(PermissionPolicyScope scope) {
        PolicyInvalidationLevel level = scope.getLevel();
        if (Objects.isNull(level)) {
            level = PolicyInvalidationLevel.EXACT;
        }

        return switch (level) {
            case EXACT -> organId.equals(scope.getOrganId())
                && userId.equals(scope.getUserId())
                && deptPaths.equals(scope.getDeptPaths())
                && moduleCode.equals(scope.getModuleCode())
                && tableName.equals(scope.getTableName());
            case ORGAN_USER -> organId.equals(scope.getOrganId())
                && userId.equals(scope.getUserId())
                && deptPaths.equals(scope.getDeptPaths());
            case ORGAN_MODEL -> organId.equals(scope.getOrganId())
                && moduleCode.equals(scope.getModuleCode())
                && tableName.equals(scope.getTableName());
        };
    }

    public static boolean isValidScope(PermissionPolicyScope scope) {
        if (Objects.isNull(scope) || Objects.isNull(scope.getOrganId())) {
            return false;
        }

        PolicyInvalidationLevel level = scope.getLevel();
        if (Objects.isNull(level)) {
            level = PolicyInvalidationLevel.EXACT;
        }

        return switch (level) {
            case EXACT -> Objects.nonNull(scope.getUserId())
                && StringUtils.hasText(scope.getDeptPaths())
                && StringUtils.hasText(scope.getModuleCode())
                && StringUtils.hasText(scope.getTableName());
            case ORGAN_USER -> Objects.nonNull(scope.getUserId())
                && StringUtils.hasText(scope.getDeptPaths());
            case ORGAN_MODEL -> StringUtils.hasText(scope.getModuleCode())
                && StringUtils.hasText(scope.getTableName());
        };
    }
}
