package com.g2rain.web.filters;


import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecBuilder;
import com.g2rain.web.HttpRequestWrapper;
import com.g2rain.web.HttpResponseWrapper;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * <p>{@code HttpWrapperFilter} 是一个 Spring {@link OncePerRequestFilter} 实现类，用于包装 HTTP 请求和响应对象。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *     <li>统一设置请求和响应的字符编码。</li>
 *     <li>将 {@link HttpServletRequest} 封装为 {@link HttpRequestWrapper}，以便支持请求参数、请求体的读取与追加。</li>
 *     <li>将 {@link HttpServletResponse} 封装为 {@link HttpResponseWrapper}，以便支持响应体的缓存与读取。</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @Bean
 * public FilterRegistrationBean<HttpWrapperFilter> httpWrapperFilterRegistration() {
 *     FilterRegistrationBean<HttpWrapperFilter> registration = new FilterRegistrationBean<>();
 *     registration.setFilter(new HttpWrapperFilter());
 *     registration.addUrlPatterns("/*");
 *     registration.addInitParameter("encode", "UTF-8");
 *     return registration;
 * }
 * }</pre>
 *
 * <p>适用场景：适用于需要读取请求体、修改请求参数或统一记录响应内容的 Web 应用。</p>
 *
 * @author alpha
 * @since 2025/10/5
 */
@NoArgsConstructor
public class HttpWrapperFilter extends OncePerRequestFilter {

    /**
     * JSON 编解码工具
     */
    private static final JsonCodec jsonCodec = JsonCodecBuilder.builder().build();

    /**
     * 核心过滤方法，将请求和响应进行包装。
     *
     * @param request  HttpServletRequest 对象
     * @param response HttpServletResponse 对象
     * @param chain    FilterChain 对象，继续调用下一个 Filter
     * @throws IOException      IO 错误
     * @throws ServletException Servlet 错误
     */
    @Override
    public void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) throws ServletException, IOException {
        // 封装请求，支持读取请求体和参数修改
        HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper(request, jsonCodec);
        httpRequestWrapper.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 封装响应，支持缓存响应内容
        HttpResponseWrapper httpResponseWrapper = new HttpResponseWrapper(response);
        httpResponseWrapper.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // 继续过滤链
        chain.doFilter(httpRequestWrapper, httpResponseWrapper);
    }
}
