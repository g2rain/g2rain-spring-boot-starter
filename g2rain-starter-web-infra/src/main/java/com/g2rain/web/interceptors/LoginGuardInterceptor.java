package com.g2rain.web.interceptors;


import com.g2rain.common.enums.SessionType;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>{@code LoginGuardInterceptor} 是一个自定义 Spring MVC {@link HandlerInterceptor}，
 * 用于在请求处理前执行登录权限校验。</p>
 *
 * <p>该拦截器会根据 {@link LoginGuard} 注解或默认规则，判断当前请求是否具有有效会话，
 * 并决定是否允许继续处理请求。</p>
 *
 * <p><b>逻辑说明：</b></p>
 * <ul>
 *     <li>如果是微服务之间的后台调用，或非 Controller 方法调用，则直接放行。</li>
 *     <li>如果方法上标注了 {@link LoginGuard} 注解，可配置 {@code require} 和 {@code anonymous}。</li>
 *     <li>根据当前 {@link PrincipalContextHolder#getSessionType()} 判断会话类型并验证登录状态。</li>
 *     <li>若校验失败，则抛出 {@link BusinessException}，返回未认证错误。</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @LoginGuard(require = true, anonymous = false)
 * @GetMapping("/api/data")
 * public String getData() {
 *     return "secure data";
 * }
 * }</pre>
 *
 * <p>适用场景：适用于需要基于会话验证登录状态的微服务接口安全控制。</p>
 *
 * @author alpha
 * @since 2025/10/5
 */
public record LoginGuardInterceptor() implements HandlerInterceptor {

    /**
     * 请求处理前执行登录权限校验。
     *
     * @param req     当前请求对象
     * @param rsp     当前响应对象
     * @param handler 请求处理对象
     * @return {@code true} 继续处理请求，{@code false} 停止请求
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest req, @NonNull HttpServletResponse rsp, @NonNull Object handler) {
        // 判断是否为微服务间调用或非 Controller 方法，直接放行
        if (PrincipalContextHolder.isBackEnd() || !(handler instanceof HandlerMethod method)) {
            return true;
        }

        // 检查方法上的 @LoginGuard 注解
        LoginGuard loginGuard = method.getMethodAnnotation(LoginGuard.class);
        if (!Optional.ofNullable(loginGuard).map(LoginGuard::require).orElse(true)) {
            return true;
        }

        // 根据会话类型验证登录状态
        SessionType sessionType = PrincipalContextHolder.getSessionType();
        if (SessionType.isUser(sessionType) && Objects.nonNull(PrincipalContextHolder.getUserId())) {
            return true;
        }

        if (SessionType.isPassport(sessionType) && Objects.nonNull(PrincipalContextHolder.getPassportId())) {
            return true;
        }

        if (SessionType.isAnonymous(sessionType)) {
            return true;
        }

        throw new BusinessException(SystemErrorCode.UNAUTHENTICATED);
    }
}
