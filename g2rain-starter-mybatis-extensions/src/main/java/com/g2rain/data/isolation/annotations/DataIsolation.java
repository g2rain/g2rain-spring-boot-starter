package com.g2rain.data.isolation.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据隔离注解，用于标记需要进行数据隔离的实体类或业务类。
 * 支持租户（Tenant）隔离方式。
 * <p>
 * 使用示例：
 * <pre>
 * {@code
 * @DataIsolation(organIdIsolation = true)
 * public class User {
 *     private String tenantId;
 *     private String name;
 * }
 * }
 * </pre>
 *
 * @author alpha
 * @since 2025/10/13
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataIsolation {

    /**
     * 是否启用租户隔离，默认为 true。
     * 如果为 true，查询或操作时会自动加上租户条件。
     *
     * @return 租户隔离标志
     */
    boolean organIdIsolation() default true;

    /**
     * 租户对应的数据库字段名，默认为 "tenant_id"。
     *
     * @return 租户字段名
     */
    String organIdColumnName() default "tenant_id";

    /**
     * 租户对应的实体属性名，默认为 "tenantId"。
     *
     * @return 租户属性名
     */
    String organIdPropertyName() default "tenantId";
}
