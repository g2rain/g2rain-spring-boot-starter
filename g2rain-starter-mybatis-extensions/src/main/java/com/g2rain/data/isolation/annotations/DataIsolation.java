package com.g2rain.data.isolation.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Mapper 参与数据隔离。
 * <p>
 * 标注即开启隔离；机构维度默认生效，用户/部门维度由字段是否配置决定。
 * 租户内细粒度权限通过 {@code permissionTableName} 关联 {@code data_permission_model.table_name}；
 * 模块编码 {@code module_code} 由当前微服务 {@code spring.application.name} 自动带入。
 * </p>
 * <p>使用示例：</p>
 * <pre>{@code
 * // application.yml: spring.application.name: g2rain-basis
 * @DataIsolation(
 *     organIdPropertyName = "organId",
 *     organIdColumnName = "organ_id",
 *     permissionTableName = "order",
 *     userIdColumnName = "user_id",
 *     deptPathColumnName = "dept_path"
 * )
 * public interface OrderDao { }
 * }</pre>
 *
 * @author alpha
 * @since 2025/10/13
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataIsolation {

    /**
     * 机构字段名（库表列），默认 {@code organ_id}。
     */
    String organIdColumnName() default "organ_id";

    /**
     * 机构字段名（实体属性），默认 {@code organId}。
     */
    String organIdPropertyName() default "organId";

    /**
     * 权限模型表名，对应 {@code data_permission_model.table_name}。
     * 为空表示仅做机构隔离，不关联动态权限策略。
     */
    String permissionTableName() default "";

    /**
     * 用户字段名（库表列）。为空则不启用用户维度隔离。
     */
    String userIdColumnName() default "";

    /**
     * 部门路径字段名（库表列）。为空则不启用部门维度隔离。
     */
    String deptPathColumnName() default "";
}
