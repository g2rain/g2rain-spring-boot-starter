package com.g2rain.web.filters;


import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecFactory;
import com.g2rain.common.utils.Collections;
import com.g2rain.common.utils.MediaTypes;
import com.g2rain.common.utils.Strings;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.common.web.PrincipalHeaders;
import com.g2rain.web.HttpRequestWrapper;
import com.g2rain.web.HttpResponseWrapper;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>{@code AccessLogFilter} 是一个 Spring {@link OncePerRequestFilter} 实现类，用于记录 HTTP 请求和响应的访问日志。</p>
 *
 * <p>功能包括：</p>
 * <ul>
 *     <li>生成唯一请求 ID（如果不存在），并存储在 {@link PrincipalContextHolder} 中。</li>
 *     <li>记录请求的 HTTP 方法、路径、参数、请求体和请求头。</li>
 *     <li>记录响应结果内容（对于 JSON 响应，支持部分截断以避免日志过长）。</li>
 *     <li>记录请求处理耗时。</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * @Bean
 * public FilterRegistrationBean<AccessLogFilter> accessLogFilter() {
 *     FilterRegistrationBean<AccessLogFilter> registration = new FilterRegistrationBean<>();
 *     registration.setFilter(new AccessLogFilter());
 *     registration.addUrlPatterns("/*");
 *     return registration;
 * }
 * }</pre>
 *
 * <p>适用场景：微服务、Web 应用中需要统一记录访问日志，方便追踪请求和性能。</p>
 *
 * @author alpha
 * @since 2025/10/5
 */
@Slf4j
public class AccessLogFilter extends OncePerRequestFilter {
    private static final JsonCodec jsonCodec = JsonCodecFactory.instance();

    /**
     * 核心过滤方法，负责记录请求和响应日志
     *
     * <p><b>日志内容示例：</b></p>
     * <pre>{@code
     *  requestId:12345, method:POST, path:/api/user, param:{id=[1]}, body:{"name":"test"}, header:{Content-Type=application/json}
     *  requestId:12345, result:{"code":200,"data":...}
     *  requestId:12345, startTime:1696636800000, endTime:1696636800100, cost:100ms
     *  }</pre>
     *
     * @param request  ServletRequest 对象，HTTP 请求封装
     * @param response ServletResponse 对象，HTTP 响应封装
     * @param chain    FilterChain 对象，负责将请求传递到下一个过滤器或目标资源
     * @throws IOException      IO 错误
     * @throws ServletException Servlet 错误
     */
    @Override
    public void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        String requestId = PrincipalContextHolder.getRequestId();

        // 如果当前请求没有 requestId，则生成一个新的 UUID
        if (Strings.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
            PrincipalContextHolder.setRequestId(requestId);
        }

        String method = request.getMethod();
        String path = request.getServletPath();

        // 记录请求参数、请求体和请求头
        if (request instanceof HttpRequestWrapper httpRequestWrapper) {
            log.info("requestId:{}, method:{}, path:{}, param:{}, body:{}, header:{}",
                requestId, method, path,
                jsonCodec.obj2str(httpRequestWrapper.getParameterMap()),
                jsonCodec.obj2str(httpRequestWrapper.getBody()),
                headerMapForLogging(httpRequestWrapper.getHeaderMap())
            );
        }

        chain.doFilter(request, response);

        // 响应日志处理
        String contentType = response.getContentType();
        if (MediaTypes.isJson(contentType)) {
            if (response instanceof HttpResponseWrapper httpResponseWrapper) {
                String result = httpResponseWrapper.getBody();
                if (Objects.isNull(result)) {
                    result = "NULL";
                } else if (result.length() > 1000) {
                    result = "result length " + result.length();
                }

                log.info("requestId:{}, result:{}", requestId, result);
            } else {
                log.info("requestId:{}, result:{}, contentType:{}", requestId, "二进制流", contentType);
            }

            long endTime = System.currentTimeMillis();
            long cost = endTime - startTime;
            log.info("requestId:{}, startTime:{}, endTime:{}, cost:{}ms", requestId, startTime, endTime, cost);
        }
    }

    private Map<String, String> headerMapForLogging(Map<String, String> headers) {
        if (Collections.isEmpty(headers)) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>(headers.size());
        headers.forEach((name, value) -> result.put(name, safeDecode(name, value)));
        return result;
    }

    private String safeDecode(String key, String value) {
        if (Strings.isBlank(value)) {
            return null;
        }

        String keyLower = key.toLowerCase();
        String nameLower = PrincipalHeaders.NAME.getLower();
        String organNameLower = PrincipalHeaders.ORGAN_NAME.getLower();
        if (!nameLower.equals(keyLower) && !organNameLower.equals(keyLower)) {
            return value;
        }

        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
