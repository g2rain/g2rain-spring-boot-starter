package com.g2rain.data.isolation;

import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;

/**
 * 数据权限策略解析器。
 * <p>
 * 根据当前登录上下文与业务模型定位键，解析用户在该模型下的权限策略。
 * </p>
 */
public interface DataPermissionPolicyResolver {

    /**
     * 解析数据权限策略。
     *
     * @param organId    机构标识
     * @param moduleCode 模块编码
     * @param tableName  业务表名
     * @return 策略结果；无策略时返回 {@code null}
     */
    DataPermissionPolicyResolveResult resolve(Long organId, String moduleCode, String tableName);
}
