package com.g2rain.data.isolation.model;


import com.g2rain.data.isolation.annotations.DataIsolation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * 数据隔离元信息，由 {@link DataIsolation} 注解解析生成，供拦截器运行时使用。
 *
 * @author alpha
 * @since 2025/10/13
 */
@Getter
@Setter
@NoArgsConstructor
public class DataIsolationMeta {

    /**
     * 机构字段名（库表列）。
     */
    private String organIdColumnName;

    /**
     * 机构字段名（实体属性）。
     */
    private String organIdPropertyName;

    /**
     * 权限模型表名，对应 {@code data_permission_model.table_name}。
     */
    private String permissionTableName;

    /**
     * 用户字段名（库表列），为空表示不启用用户维度。
     */
    private String userIdColumnName;

    /**
     * 部门路径字段名（库表列），为空表示不启用部门维度。
     */
    private String deptPathColumnName;

    /**
     * 根据 {@link DataIsolation} 注解生成元信息。
     *
     * @param dataIsolation 数据隔离注解
     * @return 数据隔离元信息
     */
    public static DataIsolationMeta genObjectByAnnotation(DataIsolation dataIsolation) {
        DataIsolationMeta meta = new DataIsolationMeta();
        meta.setPermissionTableName(dataIsolation.permissionTableName());
        meta.setOrganIdColumnName(dataIsolation.organIdColumnName());
        meta.setOrganIdPropertyName(dataIsolation.organIdPropertyName());
        meta.setPermissionTableName(dataIsolation.permissionTableName());
        meta.setUserIdColumnName(dataIsolation.userIdColumnName());
        meta.setDeptPathColumnName(dataIsolation.deptPathColumnName());
        return meta;
    }

    /**
     * 是否启用动态权限策略（已配置 {@code permissionTableName}）
     */
    public boolean hasDynamicPolicy() {
        return StringUtils.hasText(permissionTableName);
    }

    /**
     * 是否配置了用户维度字段。
     */
    public boolean hasUserColumn() {
        return StringUtils.hasText(userIdColumnName);
    }

    /**
     * 是否配置了部门维度字段。
     */
    public boolean hasDeptColumn() {
        return StringUtils.hasText(deptPathColumnName);
    }
}
