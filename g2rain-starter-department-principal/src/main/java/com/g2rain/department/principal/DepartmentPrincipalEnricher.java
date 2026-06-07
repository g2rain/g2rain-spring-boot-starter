package com.g2rain.department.principal;

import com.g2rain.common.model.Result;
import com.g2rain.common.web.BasePrincipal;
import com.g2rain.common.web.PrincipalEnricher;

import java.util.Objects;

/**
 * 基于部门服务的 Principal 增强器。
 */
public class DepartmentPrincipalEnricher implements PrincipalEnricher {

    private final DepartmentPrincipalClient client;

    public DepartmentPrincipalEnricher(DepartmentPrincipalClient client) {
        this.client = client;
    }

    @Override
    public void enrich(BasePrincipal principal) {
        if (Objects.isNull(principal) || Objects.isNull(principal.getOrganId()) || Objects.isNull(principal.getUserId())) {
            return;
        }

        Result<String> result = client.getPrincipalEnrichment(principal.getOrganId(), principal.getUserId());
        if (Objects.isNull(result) || !result.isSuccess()) {
            return;
        }

        principal.setDeptPath(result.getData());
    }
}
