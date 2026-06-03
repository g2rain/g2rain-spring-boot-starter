package com.g2rain.data.isolation.model;

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
}
