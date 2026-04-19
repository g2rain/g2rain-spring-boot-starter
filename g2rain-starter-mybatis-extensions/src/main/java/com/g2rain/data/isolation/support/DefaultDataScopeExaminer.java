package com.g2rain.data.isolation.support;

import com.g2rain.common.model.Result;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataScopeExaminer;
import com.g2rain.data.isolation.OrganHierarchyClient;
import lombok.AllArgsConstructor;

import java.util.Objects;

/**
 * 默认的数据范围校验器实现。
 * <p>
 * 通过组织服务远程接口校验目标组织是否在当前组织层级范围内。
 * </p>
 *
 * @author alpha
 * @since 2025/10/13
 */
@AllArgsConstructor
public class DefaultDataScopeExaminer implements DataScopeExaminer {

    /**
     * 组织层级关系远程客户端。
     */
    private final OrganHierarchyClient organHierarchyClient;

    /**
     * 执行远程层级校验。
     *
     * @param tenantId 目标组织标识
     * @return 是否在访问范围内
     */
    @Override
    public boolean isOrganInScope(Long tenantId) {
        Result<Boolean> result = organHierarchyClient.checkHierarchyRelation(
            tenantId, PrincipalContextHolder.getOrganId()
        );

        if (Objects.isNull(result)) {
            return false;
        }

        if (!result.isSuccess()) {
            return false;
        }

        return result.getData();
    }
}
