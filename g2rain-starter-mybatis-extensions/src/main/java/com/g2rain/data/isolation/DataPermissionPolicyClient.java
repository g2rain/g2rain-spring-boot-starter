package com.g2rain.data.isolation;

import com.g2rain.common.model.Result;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;

/**
 * 数据权限策略远程客户端抽象。
 * <p>
 * 统一封装远程策略解析调用，不关心底层是 OpenFeign 还是 RestClient。
 * </p>
 */
public interface DataPermissionPolicyClient {

    /**
     * 解析数据权限策略。
     *
     * @param organId    组织标识
     * @param userId     用户标识
     * @param deptPaths  部门路径
     * @param moduleCode 模块编码
     * @param tableName  表名
     * @return 策略结果
     */
    Result<DataPermissionPolicyResolveResult> resolveDataPermissionPolicy(Long organId, Long userId, String deptPaths, String moduleCode, String tableName);
}
