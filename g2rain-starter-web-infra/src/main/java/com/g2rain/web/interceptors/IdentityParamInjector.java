package com.g2rain.web.interceptors;


import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.web.HttpRequestWrapper;
import com.g2rain.web.interceptors.annotations.IdentityInject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * <p>{@code IdentityParamInjector} 是一个自定义 Spring MVC {@link HandlerInterceptor}，用于在请求处理前
 * 自动注入身份相关的参数到请求中。</p>
 *
 * <p>该拦截器主要用于在方法级别上根据 {@link IdentityInject} 注解，自动填充以下身份参数：</p>
 * <ul>
 *     <li>{@code passportId}</li>
 *     <li>{@code userId}</li>
 *     <li>{@code organId}</li>
 *     <li>{@code applicationId}</li>
 *     <li>{@code applicationOrganId}</li>
 * </ul>
 *
 * <p>只有当请求不是来自后台调用且方法上标注了 {@link IdentityInject} 注解时，才会执行参数注入逻辑。</p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @IdentityInject(userIdRequire = true, organIdRequire = true)
 * @GetMapping("/api/data")
 * public String getData(String userId, String organId) {
 *     // userId 和 organId 会自动注入，无需前端传参
 *     return "Data for " + userId + "@" + organId;
 * }
 * }</pre>
 *
 * <p>适用场景：适用于需要基于身份参数进行业务处理的微服务方法拦截与参数自动注入。</p>
 *
 * @author alpha
 * @since 2025/10/5
 */
public record IdentityParamInjector() implements HandlerInterceptor {

    /**
     * 在请求处理前进行身份参数注入。
     *
     * @param request  当前请求对象
     * @param response 当前响应对象
     * @param handler  请求处理对象
     * @return {@code true} 继续处理请求，{@code false} 停止请求
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // 判断是否是微服务之间调用或非 Controller 方法，跳过注入
        if (PrincipalContextHolder.isBackEnd() || !(handler instanceof HandlerMethod method)) {
            return true;
        }

        // 检查方法是否有 @IdentityInject 注解
        IdentityInject identityInject = method.getMethodAnnotation(IdentityInject.class);
        if (PrincipalContextHolder.isAdminCompany() || Objects.isNull(identityInject)) {
            return true;
        }

        if (!(request instanceof HttpRequestWrapper req)) {
            throw new BusinessException(SystemErrorCode.SYSTEM_INTERNAL_ERROR);
        }

        // 根据注解要求注入参数
        injectParam(req, "passportId", identityInject.passportIdRequire(), PrincipalContextHolder::getPassportId);
        injectParam(req, "userId", identityInject.userIdRequire(), PrincipalContextHolder::getUserId);
        injectParam(req, "organId", identityInject.organIdRequire(), PrincipalContextHolder::getOrganId);
        injectParam(req, "applicationId", identityInject.applicationIdRequire(), PrincipalContextHolder::getApplicationId);
        injectParam(req, "applicationOrganId", identityInject.applicationOrganIdRequire(), PrincipalContextHolder::getApplicationOrganId);
        return true;
    }

    /**
     * 根据条件向请求中添加参数。
     *
     * @param req      请求包装对象
     * @param name     参数名
     * @param require  是否需要注入
     * @param supplier 获取参数值的函数
     */
    @SuppressWarnings("java:S4276")
    private void injectParam(HttpRequestWrapper req, String name, boolean require, Supplier<Long> supplier) {
        if (!require) {
            return;
        }

        Long value = supplier.get();
        if (Objects.isNull(value)) {
            return;
        }

        req.addParameter(name, value.toString());
    }
}
