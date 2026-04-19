package com.g2rain.data.isolation.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>方法级数据隔离豁免注解，用于标记该方法不参与数据隔离逻辑。</p>
 * <p>通常与类型级 {@link DataIsolation} 配合使用：类上开启隔离，方法上可按需豁免。</p>
 * <p>使用示例：</p>
 * <pre>{@code
 * @IgnoreIsolation
 * public List<User> getUsers() {
 *     // 该方法不会自动注入租户隔离条件
 * }
 * }</pre>
 *
 * @author alpha
 * @since 2025/10/13
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IgnoreIsolation {

}
