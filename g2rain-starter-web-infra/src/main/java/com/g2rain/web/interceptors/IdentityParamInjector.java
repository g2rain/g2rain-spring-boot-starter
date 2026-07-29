package com.g2rain.web.interceptors;


import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.utils.Strings;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.web.HttpRequestWrapper;
import com.g2rain.web.interceptors.annotations.IdentityInject;
import jakarta.servlet.DispatcherType;
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
 * <p>ASYNC / ERROR 派发直接跳过注入（见 {@link #preHandle} 内说明）。</p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @IdentityInject(userIdPropertyName = "userId", organIdPropertyName = "organId")
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
        /*
         * 为何会出现 ASYNC：
         * 与 LoginGuardInterceptor 相同。SseEmitter 等异步返回值在 complete 后，容器会 ASYNC 再派发，
         * 以便补跑 postHandle / afterCompletion；该派发会再次进入 preHandle，但不会再次执行 Controller。
         *
         * 为何要跳过注入：
         * 身份参数已在首次 REQUEST 派发时注入。ASYNC 时 OncePerRequestFilter（含 HttpWrapperFilter、
         * PrincipalContextScopeFilter）默认不再执行，请求未必仍是 HttpRequestWrapper，身份上下文也可能已清空；
         * 若此处继续注入，轻则无意义，重则因非 HttpRequestWrapper 抛出 SYSTEM_INTERNAL_ERROR。
         * ERROR 派发同理，无需再注入业务参数。
         *
         * 对后续业务的影响：
         * 无影响。Controller 参数解析只发生在 REQUEST 阶段；ASYNC 收尾不进 Controller，跳过注入不会漏参。
         */
        DispatcherType dispatcherType = request.getDispatcherType();
        if (dispatcherType == DispatcherType.ASYNC || dispatcherType == DispatcherType.ERROR) {
            return true;
        }

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
        injectParam(req, identityInject.passportIdPropertyName(), PrincipalContextHolder::getPassportId);
        injectParam(req, identityInject.userIdPropertyName(), PrincipalContextHolder::getUserId);
        injectParam(req, identityInject.organIdPropertyName(), PrincipalContextHolder::getOrganId);
        injectParam(req, identityInject.applicationIdPropertyName(), PrincipalContextHolder::getApplicationId);
        injectParam(req, identityInject.applicationOrganIdPropertyName(), PrincipalContextHolder::getApplicationOrganId);
        return true;
    }

    /**
     * 根据条件向请求中添加参数。
     *
     * @param req          请求包装对象
     * @param propertyName 注入参数名，为空时不注入
     * @param supplier     获取参数值的函数
     */
    private void injectParam(HttpRequestWrapper req, String propertyName, Supplier<Long> supplier) {
        if (Strings.isBlank(propertyName)) {
            return;
        }

        Long value = supplier.get();
        if (Objects.isNull(value)) {
            return;
        }

        req.addParameter(propertyName, value.toString());
    }
}
