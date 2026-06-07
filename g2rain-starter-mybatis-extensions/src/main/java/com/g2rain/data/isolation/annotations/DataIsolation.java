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
 * {@code isolationModule}、{@code isolationTable} 用于定位隔离策略，与权限模型注册键一致。
 * </p>
 * <p>使用示例：</p>
 * <pre>{@code
 * @DataIsolation(
 *     isolationModule = "order",
 *     isolationTable = "order",
 *     organIdPropertyName = "organId",
 *     organIdColumnName = "organ_id",
 *     userIdPropertyName = "ownerUserId",
 *     userIdColumnName = "owner_user_id",
 *     deptPathPropertyName = "deptPath",
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
     * 隔离注册模块编码，对应 {@code data_permission_model.module_code}。
     * 为空表示仅做机构隔离，不关联动态权限策略。
     */
    String isolationModule() default "";

    /**
     * 隔离注册表名，对应 {@code data_permission_model.table_name}。
     * 为空表示仅做机构隔离，不关联动态权限策略。
     */
    String isolationTable() default "";

    /**
     * 机构字段名（库表列），默认 {@code organ_id}。
     */
    String organIdColumnName() default "organ_id";

    /**
     * 机构字段名（实体属性），默认 {@code organId}。
     */
    String organIdPropertyName() default "organId";

    /**
     * 用户字段名（库表列）。为空则不启用用户维度隔离。
     */
    String userIdColumnName() default "";

    /**
     * 部门路径字段名（库表列）。为空则不启用部门维度隔离。
     */
    String deptPathColumnName() default "";
}
