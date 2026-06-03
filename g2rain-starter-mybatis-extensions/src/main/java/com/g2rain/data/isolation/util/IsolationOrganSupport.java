package com.g2rain.data.isolation.util;

import com.g2rain.common.enums.OrganType;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.enums.IsolationErrorCode;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.mybatis.extension.IsolationFieldExtractor;
import org.apache.ibatis.session.Configuration;

import java.util.Objects;
import java.util.Set;

/**
 * 组织隔离上下文解析辅助。
 */
public final class IsolationOrganSupport {

    private IsolationOrganSupport() {
    }

    public static Long resolveTargetOrganId(DataIsolationMeta meta, Configuration configuration, Object parameter) {
        Long targetOrganId = null;
        if (OrganType.isTenant(PrincipalContextHolder.getOrganType())) {
            targetOrganId = PrincipalContextHolder.getOrganId();
        }

        if (Objects.isNull(targetOrganId)) {
            String propertyName = meta.getOrganIdPropertyName();
            Set<Object> values = IsolationFieldExtractor.extractValues(configuration, parameter, propertyName);
            if (values.size() != 1) {
                throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NOT_EXIST, "tenantId");
            }

            Object val = values.iterator().next();
            targetOrganId = (val instanceof Number) ? ((Number) val).longValue() : null;
        }

        if (Objects.isNull(targetOrganId)) {
            throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NOT_EXIST, "tenantId");
        }

        return targetOrganId;
    }
}
