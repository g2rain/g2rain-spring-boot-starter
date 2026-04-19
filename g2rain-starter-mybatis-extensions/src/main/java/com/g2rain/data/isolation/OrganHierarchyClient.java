package com.g2rain.data.isolation;


import com.g2rain.common.model.Result;

/**
 * 组织层级关系查询客户端抽象。
 * <p>
 * 该接口用于统一封装远程组织服务调用，不关心底层是 OpenFeign 还是 RestClient。
 * </p>
 */
public interface OrganHierarchyClient {

    /**
     * 判断 childId 是否属于 parentId 的层级范围内。
     *
     * @param childId  待校验的子组织标识
     * @param parentId 当前上下文组织标识（父级范围）
     * @return 布尔结果包装对象
     */
    Result<Boolean> checkHierarchyRelation(Long childId, Long parentId);
}
