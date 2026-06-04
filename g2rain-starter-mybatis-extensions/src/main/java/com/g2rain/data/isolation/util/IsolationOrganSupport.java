package com.g2rain.data.isolation.util;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.enums.IsolationErrorCode;

import java.util.Objects;

/**
 * 组织隔离上下文解析辅助。
 */
public final class IsolationOrganSupport {

    private IsolationOrganSupport() {
    }

    public static Long resolveTargetOrganId() {
        Long targetOrganId = PrincipalContextHolder.getOrganId();
        if (Objects.nonNull(targetOrganId)) {
            return targetOrganId;

        }

        throw new BusinessException(
            IsolationErrorCode.ISOLATION_TENANT_NOT_EXIST,
            "tenantId"
        );
    }
}
