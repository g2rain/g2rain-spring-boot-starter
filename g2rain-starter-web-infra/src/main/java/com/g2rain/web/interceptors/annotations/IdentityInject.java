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
 * <p>属性说明：各属性为注入参数名，非空时才会注入对应身份值，默认为空字符串（不注入）。</p>
 * <ul>
 *     <li>{@code organIdPropertyName} — 组织 ID 参数名。</li>
 *     <li>{@code passportIdPropertyName} — 账号 ID 参数名。</li>
 *     <li>{@code userIdPropertyName} — 用户 ID 参数名。</li>
 *     <li>{@code applicationIdPropertyName} — 应用 ID 参数名。</li>
 *     <li>{@code applicationOrganIdPropertyName} — 应用组织 ID 参数名。</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @IdentityInject(
 *     organIdPropertyName = "organId",
 *     userIdPropertyName = "userId"
 * )
 * public void processUserData(String organId, String userId) {
 *     // organId 和 userId 将被自动注入
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
     * 组织 ID 注入参数名。
     *
     * @return 默认空字符串，不注入
     */
    String organIdPropertyName() default "";

    /**
     * 账号 ID 注入参数名。
     *
     * @return 默认空字符串，不注入
     */
    String passportIdPropertyName() default "";

    /**
     * 用户 ID 注入参数名。
     *
     * @return 默认空字符串，不注入
     */
    String userIdPropertyName() default "";

    /**
     * 应用 ID 注入参数名。
     *
     * @return 默认空字符串，不注入
     */
    String applicationIdPropertyName() default "";

    /**
     * 应用组织 ID 注入参数名。
     *
     * @return 默认空字符串，不注入
     */
    String applicationOrganIdPropertyName() default "";
}
