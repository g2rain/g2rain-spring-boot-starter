package com.g2rain.feign;


import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.ExceptionConverter;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecFactory;
import com.g2rain.common.model.Result;
import com.g2rain.common.utils.Collections;
import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import feign.Contract;
import feign.MethodMetadata;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Role;
import org.springframework.http.HttpMethod;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;

/**
 * <h1>G2rain Feign 自动配置</h1>
 *
 * <p>该类提供了一套完整的 Feign 客户端增强方案，包括：</p>
 * <ul>
 *     <li>自定义 Contract，用于支持 GET 请求参数自动作为查询参数</li>
 *     <li>请求拦截器（RequestInterceptor），用于自动注入 {@link PrincipalContext} 的上下文 Header</li>
 *     <li>响应解码器（Decoder），支持 {@link Result} 封装的数据自动解析及异常转换</li>
 *     <li>错误解码器（ErrorDecoder），捕获 Feign 调用异常并统一包装业务异常</li>
 *     <li>请求和响应日志输出，支持在 debug 级别打印 JSON 格式的 Header 和 Body</li>
 * </ul>
 *
 * <p>该类保证 Feign 客户端调用的安全性和可追踪性，同时提供生产级别的容错与调试能力。</p>
 *
 * <p>实现特点：</p>
 * <ul>
 *     <li>使用 {@link PrincipalContextHolder} 实现请求上下文透传</li>
 *     <li>Decoder 根据泛型类型解析返回数据，并在业务失败时抛出 {@link ExceptionConverter}</li>
 *     <li>ErrorDecoder 在日志级别 debug 下打印原始响应内容，并统一抛出 {@link BusinessException}</li>
 * </ul>
 *
 * @author alpha
 * @since 2026/2/27
 */
@Slf4j
@AutoConfiguration
public class G2rainFeignAutoConfiguration {

    /**
     * 创建并返回一个自定义的 Feign Contract Bean，用于处理 Feign 客户端请求时的参数注解。
     * <p>
     * 通过继承 `SpringMvcContract` 并重写 `processAnnotationsOnParameter`，我们可以定制 GET 请求时
     * 参数的处理方式。该方法能够在没有显式注解的情况下，将方法参数自动作为查询参数处理。
     * </p>
     *
     * <p>
     * 该方法的核心逻辑是：当请求是 GET 请求且方法参数没有注解时，将参数注册为查询参数，并进行
     * 字符串转换处理（避免 null 值抛出异常）。如果条件不满足，则继续使用默认的参数处理逻辑。
     * </p>
     *
     * @return 自定义的 `Contract` 实现，用于替代默认的 `SpringMvcContract`
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(name = "feignPlusContract")
    @ConditionalOnClass({Contract.class, SpringMvcContract.class})
    public Contract feignPlusContract() {
        return new SpringMvcContract() {

            /**
             * 重写 `processAnnotationsOnParameter` 方法，处理方法参数上的注解。
             * <p>
             * 该方法会检查每个方法参数，判断是否为 GET 请求以及是否缺少注解。如果是 GET 请求并且
             * 没有注解，就会将参数作为查询参数处理。如果满足条件，将参数名标记为查询参数，并
             * 转换为字符串形式。否则，继续使用默认的处理逻辑。
             * </p>
             *
             * @param data 当前方法的元数据，包含了请求类型、请求路径等信息
             * @param ats 当前方法参数上的注解，可能包含 `@RequestParam` 等
             * @param paramIndex 当前参数在方法参数列表中的索引
             * @return `true` 表示该参数已被处理为查询参数，不会被当作请求体（Body）；`false` 表示继续执行父类的处理逻辑
             */
            @Override
            public boolean processAnnotationsOnParameter(MethodMetadata data, Annotation[] ats, int paramIndex) {
                // 不增强的条件是：非 GET 请求 或者 形参存在注解
                String expectedValue = HttpMethod.GET.name();
                String actualValue = data.template().method();
                if (!expectedValue.equalsIgnoreCase(actualValue) || Collections.isNotEmpty(ats)) {
                    return super.processAnnotationsOnParameter(data, ats, paramIndex);
                }

                // 1. 标记参数名, 获取当前方法参数的名称，并将其添加到方法元数据中
                String name = data.method().getParameters()[paramIndex].getName();
                super.nameParam(data, name, paramIndex);

                // 2. 注册到 query 参数列表, 通过调用 queryMapIndex 将该参数注册为 GET 请求的查询参数
                data.queryMapIndex(paramIndex); // <- 核心，告诉 Contract 这是 query 参数
                // 3. 将该参数值转换为字符串, 使用 `Objects.toString(value, null)` 确保参数为 null 时不会抛出异常
                data.indexToExpander().put(paramIndex, value ->
                    Objects.toString(value, null)
                );

                // 关键：返回 true, 表示该参数已经作为查询参数处理, 且不会被当作请求体(Body)
                return true;
            }
        };
    }

    /**
     * <h1>Feign 请求拦截器 Bean</h1>
     *
     * <p>该 Bean 用于在 Feign 客户端调用时，自动将当前线程的 {@link PrincipalContext} 中的
     * 所有透传 Header 注入到请求中，实现请求级别的上下文透传。</p>
     *
     * <p>特性：</p>
     * <ul>
     *     <li>自动获取 {@link PrincipalContextHolder#get()} 当前线程上下文</li>
     *     <li>通过 {@link feign.RequestTemplate#headers(java.util.Map)} 一次性设置所有需要透传的 Header</li>
     *     <li>支持在不存在上下文时自动跳过，避免空指针异常</li>
     * </ul>
     *
     * <p>条件注解说明：</p>
     * <ul>
     *     <li>{@link ConditionalOnClass}：仅在类路径存在 {@code feign.RequestInterceptor} 时生效</li>
     *     <li>{@link ConditionalOnMissingBean}：如果上下文中已经存在同名 Bean，则不再创建</li>
     * </ul>
     *
     * @return 一个 Feign {@link RequestInterceptor} 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnClass(name = "feign.RequestInterceptor")
    @ConditionalOnMissingBean(name = "principalContextFeignInterceptor")
    public RequestInterceptor principalContextFeignInterceptor() {
        return template -> {
            PrincipalContext principalContext = PrincipalContextHolder.get();
            if (Objects.nonNull(principalContext)) {
                template.headers(principalContext.getHeaders());
            }

            if (log.isDebugEnabled()) {
                byte[] body = template.body();
                String bodyStr = Objects.nonNull(body) ? new String(body) : null;
                JsonCodec jsonCodec = JsonCodecFactory.instance();
                String headerStr = jsonCodec.obj2str(template.headers());
                log.debug("[Feign][Request] url={} headers={} body={}",
                    template.url(), headerStr, bodyStr
                );
            }
        };
    }

    /**
     * <h2>Feign 响应解码器</h2>
     * <p>自动将 HTTP 响应反序列化为目标对象，并在业务失败时抛出 {@link ExceptionConverter}</p>
     *
     * @param mapper {@link JsonMapper} 实例
     * @return Feign {@link Decoder} Bean
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnClass(name = "feign.codec.Decoder")
    @ConditionalOnMissingBean(name = "feignPlusDecoder")
    public Decoder feignPlusDecoder(JsonMapper mapper) {
        return (response, type) -> {
            if (Objects.isNull(response) || Objects.isNull(response.body())) {
                return null;
            }

            // 使用 try-with-resources 确保 InputStream 被关闭
            try (InputStream is = response.body().asInputStream()) {
                Object result = mapper.readValue(is, mapper.getTypeFactory().constructType(type));

                if (log.isDebugEnabled()) {
                    JsonCodec jsonCodec = JsonCodecFactory.instance();
                    String headerStr = jsonCodec.obj2str(response.headers());
                    String bodyStr = jsonCodec.obj2str(result);
                    log.debug("[Feign][Response] status={} headers={} body={}",
                        response.status(), headerStr, bodyStr
                    );
                }

                if (!(result instanceof Result<?> r)) {
                    return result;
                }

                if (!r.isSuccess()) {
                    throw ExceptionConverter.of(r);
                }

                if (type instanceof ParameterizedType pt && pt.getRawType().equals(Result.class)) {
                    return r;
                }

                return r.getData();
            } catch (IOException e) {
                throw new BusinessException(SystemErrorCode.SYSTEM_INTERNAL_ERROR, "Failed to decode feign response body");
            }
        };
    }
}
