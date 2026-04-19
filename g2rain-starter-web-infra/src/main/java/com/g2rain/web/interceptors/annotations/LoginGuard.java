package com.g2rain.web.interceptors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>{@code LoginGuard} 是一个自定义方法级注解，用于标记方法的登录权限校验规则。</p>
 *
 * <p>通常配合 {@link com.g2rain.web.interceptors.LoginGuardInterceptor} 使用，在方法执行前对登录状态进行检查。</p>
 *
 * <p>属性说明：</p>
 * <ul>
 *     <li>{@code require} — 是否强制进行登录校验，默认为 {@code true}。</li>
 *     <li>{@code anonymous} — 是否允许匿名访问，默认为 {@code false}。</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @LoginGuard(require = true)
 * public void secureMethod() {
 *     // 仅允许已登录用户访问
 * }
 *
 * @LoginGuard(require = false, anonymous = true)
 * public void openMethod() {
 *     // 允许匿名访问
 * }
 * }</pre>
 *
 * <p>适用场景：适用于需要基于登录状态控制访问权限的 Web 方法。</p>
 *
 * @author alpha
 * @since 2025/10/2
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginGuard {

    /**
     * 是否需要强制登录校验。
     *
     * @return 默认 {@code true}
     */
    boolean require() default true;
}
