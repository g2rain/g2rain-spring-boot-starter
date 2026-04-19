package com.g2rain.web.filters;


import com.g2rain.common.utils.Strings;
import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.common.web.PrincipalHeaders;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * <p>{@code PrincipalContextFilter} 是一个 Spring {@link OncePerRequestFilter} 实现类，用于在请求处理过程中注入和管理 {@link PrincipalContext}。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *     <li>从 HTTP 请求头中提取与 {@link PrincipalHeaders} 定义的键对应的值。</li>
 *     <li>将提取的值设置到当前线程的 {@link PrincipalContext} 中，供后续处理使用。</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @Bean
 * public FilterRegistrationBean<PrincipalContextFilter> principalContextFilterRegistration() {
 *     FilterRegistrationBean<PrincipalContextFilter> registration = new FilterRegistrationBean<>();
 *     registration.setFilter(new PrincipalContextFilter());
 *     registration.addUrlPatterns("/*");
 *     return registration;
 * }
 * }</pre>
 *
 * <p>适用场景：适用于需要在整个请求生命周期中传递用户身份信息、权限信息或上下文参数的 Web 应用。</p>
 *
 * @author alpha
 * @since 2025/10/5
 */
public class PrincipalContextFilter extends OncePerRequestFilter {
    private static final String DEBUG_PRINT_LEVEL = "DEBUG_PRINT_LEVEL";

    /**
     * 从请求头中读取 {@link PrincipalHeaders} 定义的值，并将其注入到 {@link PrincipalContext}。
     * <p>
     * 请求处理结束后，会清理当前线程的 {@link PrincipalContext}。
     * </p>
     *
     * @param request  ServletRequest 对象
     * @param response ServletResponse 对象
     * @param chain    FilterChain 对象
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) throws ServletException, IOException {
        PrincipalContext context = PrincipalContextHolder.get();
        for (PrincipalHeaders headerKey : PrincipalHeaders.values()) {
            String headerValue = getHeaderValue(request, headerKey);
            if (Strings.isBlank(headerValue)) {
                continue;
            }
            context.setValue(headerKey, headerValue);
        }

        try {
            if (context.isDebug()) {
                MDC.put(DEBUG_PRINT_LEVEL, "TRUE");
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove(DEBUG_PRINT_LEVEL);
        }
    }

    /**
     * 获取请求头的值，优先使用大写键名，其次使用小写键名。
     *
     * @param request HTTP 请求对象
     * @param key     {@link PrincipalHeaders} 枚举值
     * @return 请求头中对应键的值，若不存在则返回 {@code null}
     */
    private String getHeaderValue(HttpServletRequest request, PrincipalHeaders key) {
        if (Objects.isNull(request)) {
            return null;
        }

        String result = request.getHeader(key.getUpper());
        if (Strings.isNotBlank(result)) {
            return result;
        }

        return request.getHeader(key.getLower());
    }
}
