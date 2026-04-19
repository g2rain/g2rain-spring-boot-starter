package com.g2rain.web.filters;


import com.g2rain.common.exception.BaseError;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.ExceptionConverter;
import com.g2rain.common.exception.ExceptionProcessor;
import com.g2rain.common.json.FailIgnoreFieldMixIn;
import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecBuilder;
import com.g2rain.common.json.JsonCodecFactory;
import com.g2rain.common.model.Result;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.web.autoconfigure.G2rainWebProperties;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 全局异常过滤器（GlobalExceptionFilter）。
 *
 * <p>该 Filter 用于捕获整个 Servlet Filter 链中未被捕获的异常，
 * 并将异常统一转换为标准 JSON 响应返回给客户端。
 * 它依赖 {@link ExceptionProcessor} 对异常进行封装和本地化处理。
 * </p>
 *
 * <p>特性：</p>
 * <ul>
 *     <li>捕获 {@link BusinessException} 并转换为统一 {@link Result} 响应。</li>
 *     <li>捕获任意其他异常，将其转换为默认业务异常再封装返回。</li>
 *     <li>响应尚未提交时，直接以 JSON 格式输出异常信息。</li>
 *     <li>支持配置开关 {@code G2rainWebProperties.globalExceptionFilterUseCustomJson} 选择 JSON 序列化器：
 *         <ul>
 *             <li>false：使用轻量默认 {@link JsonCodecFactory#instance()}。</li>
 *             <li>true：使用自定义 {@link JsonCodecBuilder} 并支持 MixIn 配置。</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p>注意事项：</p>
 * <ul>
 *     <li>应与 {@link HttpWrapperFilter} 配合使用，以保证响应缓存和延迟写出行为。</li>
 *     <li>若 JSON 编码异常，将直接抛出 {@link IOException}。</li>
 *     <li>Filter 链执行顺序需优先于业务逻辑执行，以捕获全链路异常。</li>
 * </ul>
 *
 * @author alpha
 * @since 2025/10/5
 */
@Slf4j
public class GlobalExceptionFilter extends OncePerRequestFilter {

    /**
     * 异常处理器，用于将捕获的异常封装成统一的 {@link Result} 响应。
     * 依赖 {@link ExceptionProcessor#process(BusinessException, String)} 方法。
     */
    private final ExceptionProcessor exceptionProcessor;

    /**
     * JSON 编解码工具，用于将 {@link Result} 对象序列化为 JSON 字符串。
     * <p>
     * 可通过 {@code G2rainWebProperties.globalExceptionFilterUseCustomJson} 配置选择：
     * <ul>
     *     <li>默认实例 {@link JsonCodecFactory#instance()}。</li>
     *     <li>自定义实例 {@link JsonCodecBuilder} 并支持 MixIn 配置 {@link FailIgnoreFieldMixIn}。</li>
     * </ul>
     * </p>
     */
    private final JsonCodec jsonCodec;

    /**
     * 构造函数。
     *
     * @param exceptionProcessor 注入的异常处理器
     * @param properties         Web 模块配置属性，用于控制是否使用自定义 JSON 编解码器
     */
    public GlobalExceptionFilter(ExceptionProcessor exceptionProcessor, G2rainWebProperties properties) {
        this.exceptionProcessor = exceptionProcessor;
        // 根据开关选择序列化器
        if (properties.isGlobalExceptionFilterUseCustomJson()) {
            this.jsonCodec = JsonCodecBuilder.builder().withDefaults().withConfig(jsonMapper -> {
                jsonMapper.addMixIn(Result.class, FailIgnoreFieldMixIn.class);
                jsonMapper.addMixIn(BaseError.class, FailIgnoreFieldMixIn.class);
            }).build();
        } else {
            this.jsonCodec = JsonCodecFactory.instance();
        }
    }

    /**
     * 核心过滤方法。
     * <p>
     * 1. 执行下游 Filter 链。
     * 2. 捕获 {@link BusinessException} 与其他异常。
     * 3. 将异常转换为标准 {@link Result} 对象。
     * 4. 响应未提交时，将 JSON 异常信息写回客户端。
     * </p>
     *
     * @param request  Servlet 请求对象
     * @param response Servlet 响应对象
     * @param chain    Filter 链对象
     * @throws IOException 如果响应输出发生 IO 错误
     */
    @Override
    public void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) throws IOException {
        Result<Void> fail = null;
        try {
            // 执行下游 Filter 链
            chain.doFilter(request, response);
        } catch (BusinessException e) {
            log.error("业务异常处理-{}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            // 捕获业务异常并转换为标准返回结果
            fail = exceptionProcessor.process(e, PrincipalContextHolder.getAcceptLanguage());
        } catch (Exception e) {
            log.error("系统异常处理-{}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            // 捕获其他异常，转换为默认业务异常，再封装返回结果
            BusinessException be = ExceptionConverter.findBusinessExceptionOrDefault(e);
            fail = exceptionProcessor.process(be, PrincipalContextHolder.getAcceptLanguage());
        } finally {
            // 响应未提交时写回异常信息 JSON
            if (Objects.nonNull(fail) && !response.isCommitted()) {
                // 1. 设置状态码和 Header
                response.setStatus(HttpServletResponse.SC_OK);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                // 2. 使用二进制流写入，防止 getWriter() 冲突
                byte[] jsonBytes = jsonCodec.obj2byte(fail);
                // 明确告知浏览器大小
                response.setContentLength(jsonBytes.length);
                response.getOutputStream().write(jsonBytes);
                response.getOutputStream().flush();
            }
        }
    }
}
