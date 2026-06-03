package com.g2rain.data.isolation.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 数据权限策略解析结果（隔离拦截器使用）。
 * <p>
 * 字段与 g2rain-department 中 {@code DataPermissionPolicyVo} 对齐。
 * </p>
 */
@Getter
@Setter
@NoArgsConstructor
public class DataPermissionPolicyResolveResult {

    private Long metaId;

    private boolean groupRead;

    private boolean groupWrite;

    private boolean otherRead;

    private boolean otherWrite;

    private String otherPermRule;
}
