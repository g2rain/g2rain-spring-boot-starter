package com.g2rain.data.isolation.support;

import com.g2rain.common.model.Result;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataPermissionPolicyClient;
import com.g2rain.data.isolation.DataPermissionPolicyResolver;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import lombok.AllArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * 默认数据权限策略解析器，委托远程 department 服务。
 */
@AllArgsConstructor
public class DefaultDataPermissionPolicyResolver implements DataPermissionPolicyResolver {

    private final DataPermissionPolicyClient dataPermissionPolicyClient;

    @Override
    public DataPermissionPolicyResolveResult resolve(Long organId, String moduleCode, String tableName) {
        Long userId = PrincipalContextHolder.getUserId();
        String deptPath = PrincipalContextHolder.getDeptPath();

        if (Objects.isNull(userId) || !StringUtils.hasText(deptPath)) {
            return null;
        }

        Result<DataPermissionPolicyResolveResult> result = dataPermissionPolicyClient.resolveDataPermissionPolicy(
            organId, userId, deptPath, moduleCode, tableName
        );

        if (Objects.isNull(result) || !result.isSuccess()) {
            return null;
        }

        return result.getData();
    }
}
