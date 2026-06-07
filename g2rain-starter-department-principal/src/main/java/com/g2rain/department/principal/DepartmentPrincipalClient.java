package com.g2rain.department.principal;

import com.g2rain.common.model.Result;

/**
 * 部门 Principal 增强信息查询客户端抽象。
 */
public interface DepartmentPrincipalClient {

    /**
     * 获取 Principal 增强信息。
     *
     * @param organId 机构标识
     * @param userId  用户标识
     * @return 部门路径响应
     */
    Result<String> getPrincipalEnrichment(Long organId, Long userId);
}
