package com.g2rain.data.isolation.enums;

/**
 * 数据权限策略缓存失效粒度。
 */
public enum PolicyInvalidationLevel {

    /**
     * 精确匹配：organId + userId + deptPaths + moduleCode + tableName。
     */
    EXACT,

    /**
     * 机构用户级：organId + userId + deptPaths 下全部 module/table。
     */
    ORGAN_USER,

    /**
     * 机构模型级：organId + moduleCode + tableName 下全部用户。
     */
    ORGAN_MODEL
}
