package com.g2rain.web.interceptors.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>{@code IdentityInject} 是一个自定义方法级注解，用于标记需要进行身份参数注入的方法。</p>
 *
 * <p>它通常配合 {@link com.g2rain.web.interceptors.IdentityParamInjector} 使用，在方法执行前自动注入所需的身份信息。</p>
 *
 * <p>属性说明：</p>
 * <ul>
 *     <li>{@code organIdRequire} — 是否需要注入组织 ID，默认为 {@code false}。</li>
 *     <li>{@code passportIdRequire} — 是否需要注入账号 ID，默认为 {@code false}。</li>
 *     <li>{@code userIdRequire} — 是否需要注入用户 ID，默认为 {@code false}。</li>
 *     <li>{@code applicationIdRequire} — 是否需要注入应用 ID，默认为 {@code false}。</li>
 *     <li>{@code applicationOrganIdRequire} — 是否需要注入应用组织 ID，默认为 {@code false}。</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @IdentityInject(
 *     organIdRequire = true,
 *     userIdRequire = true
 * )
 * public void processUserData() {
 *     // 方法执行时，organId 和 userId 将被自动注入
 * }
 * }</pre>
 *
 * <p>适用场景：适用于需要在方法调用时自动注入当前请求的身份信息，减少手动获取参数的重复代码。</p>
 *
 * @author alpha
 * @since 2025/10/2
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface IdentityInject {

    /**
     * 是否需要注入组织 ID。
     *
     * @return 默认 {@code false}
     */
    boolean organIdRequire() default false;

    /**
     * 是否需要注入账号 ID。
     *
     * @return 默认 {@code false}
     */
    boolean passportIdRequire() default false;

    /**
     * 是否需要注入用户 ID。
     *
     * @return 默认 {@code false}
     */
    boolean userIdRequire() default false;

    /**
     * 是否需要注入应用 ID。
     *
     * @return 默认 {@code false}
     */
    boolean applicationIdRequire() default false;

    /**
     * 是否需要注入应用组织 ID。
     *
     * @return 默认 {@code false}
     */
    boolean applicationOrganIdRequire() default false;
}
