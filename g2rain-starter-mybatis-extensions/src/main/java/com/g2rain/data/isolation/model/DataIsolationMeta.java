package com.g2rain.data.isolation.model;


import com.g2rain.data.isolation.annotations.DataIsolation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>数据隔离元信息类，用于封装租户隔离和应用隔离的配置信息。</p>
 * <p>通常通过 {@link DataIsolation} 注解生成对象，可用于数据隔离拦截器或服务中。</p>
 * <p>使用示例：</p>
 * <pre>{@code
 * DataIsolation dataIsolationAnnotation = ...;
 * DataIsolationMeta meta = DataIsolationMeta.genObjectByAnnotation(dataIsolationAnnotation);
 * }</pre>
 *
 * @author alpha
 * @since 2025/10/13
 */
@Getter
@Setter
@NoArgsConstructor
public class DataIsolationMeta {

    /**
     * <p>租户隔离标志。</p>
     * <p>{@code true} 表示启用租户隔离，{@code false} 表示不启用。</p>
     */
    private boolean organIdIsolation;

    /**
     * <p>租户列名。</p>
     * <p>对应数据库表中的租户字段名，例如 {@code "tenant_id"}。</p>
     */
    private String organIdColumnName;

    /**
     * <p>租户属性。</p>
     * <p>对应实体类中的租户属性名，例如 {@code "tenantId"}。</p>
     */
    private String organIdPropertyName;

    /**
     * <p>根据 {@link DataIsolation} 注解生成 {@code DataIsolationMeta} 对象。</p>
     * <p>从注解中提取各字段的配置信息，用于后续数据隔离处理。</p>
     * <p>使用示例：</p>
     * <pre>{@code
     * DataIsolationMeta meta = DataIsolationMeta.genObjectByAnnotation(dataIsolation);
     * }</pre>
     *
     * @param dataIsolation {@link DataIsolation} 注解实例
     * @return 封装的 {@link DataIsolationMeta} 对象
     */
    public static DataIsolationMeta genObjectByAnnotation(DataIsolation dataIsolation) {
        DataIsolationMeta meta = new DataIsolationMeta();
        meta.setOrganIdColumnName(dataIsolation.organIdColumnName());
        meta.setOrganIdIsolation(dataIsolation.organIdIsolation());
        meta.setOrganIdPropertyName(dataIsolation.organIdPropertyName());
        return meta;
    }
}
