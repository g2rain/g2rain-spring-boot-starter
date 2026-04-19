package com.g2rain.data.isolation.support;


import com.g2rain.common.model.Result;
import com.g2rain.data.isolation.OrganHierarchyClient;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

/**
 * 基于 Spring HTTP Interface 的组织层级关系客户端实现。
 * <p>
 * 当工程未启用 OpenFeign 时，自动配置会回退为该实现。
 * </p>
 */
public interface OrganHierarchyRestClient extends OrganHierarchyClient {

    /**
     * 远程调用组织服务，判断 childId 是否在 parentId 的层级范围内。
     *
     * @param childId  待校验组织 ID
     * @param parentId 当前上下文组织 ID
     * @return 层级关系判断结果
     */
    @Override
    @GetExchange("${g2rain.data.isolation.path-business:/hierarchy/exists}")
    Result<Boolean> checkHierarchyRelation(@RequestParam("childId") Long childId, @RequestParam("parentId") Long parentId);
}
