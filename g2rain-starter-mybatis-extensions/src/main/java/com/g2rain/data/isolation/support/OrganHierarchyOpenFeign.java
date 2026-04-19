package com.g2rain.data.isolation.support;


import com.g2rain.common.model.Result;
import com.g2rain.data.isolation.OrganHierarchyClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


/**
 * 基于 OpenFeign 的组织层级关系客户端实现。
 * <p>
 * 该接口继承通用客户端抽象 {@link OrganHierarchyClient}，用于在引入 Feign 场景下
 * 自动装配远程组织关系查询能力。
 * </p>
 */
@FeignClient(name = "${g2rain.data.isolation.service-name:g2rain-basis}", contextId = "organIsolationClient", url = "${g2rain.data.isolation.service-url:}", path = "${g2rain.data.isolation.service-path:organ}")
public interface OrganHierarchyOpenFeign extends OrganHierarchyClient {

    /**
     * 远程调用组织服务，判断 childId 是否在 parentId 的层级范围内。
     *
     * @param childId  待校验组织 ID
     * @param parentId 当前上下文组织 ID
     * @return 层级关系判断结果
     */
    @Override
    @GetMapping(value = "${g2rain.data.isolation.path-business:/hierarchy/exists}")
    Result<Boolean> checkHierarchyRelation(@RequestParam("childId") Long childId, @RequestParam("parentId") Long parentId);
}
