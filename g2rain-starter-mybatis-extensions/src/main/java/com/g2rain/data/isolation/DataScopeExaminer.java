package com.g2rain.data.isolation;

/**
 * 数据范围校验器。
 * <p>
 * 用于判断目标组织（租户）是否在当前登录组织的可访问范围内。
 * 查询、插入、更新、删除等数据隔离处理器会通过该接口进行权限校验。
 * </p>
 *
 * @author alpha
 * @since 2025/10/13
 */
public interface DataScopeExaminer {

    /**
     * 校验目标组织是否可被当前上下文访问。
     *
     * @param tenantId 目标组织标识
     * @return {@code true} 表示在访问范围内，{@code false} 表示不在范围内
     */
    boolean isOrganInScope(Long tenantId);
}
