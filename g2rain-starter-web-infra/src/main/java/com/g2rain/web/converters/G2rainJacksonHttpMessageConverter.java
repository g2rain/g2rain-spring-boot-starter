package com.g2rain.web.converters;


import com.fasterxml.jackson.annotation.JsonView;
import com.g2rain.common.exception.BaseError;
import com.g2rain.common.json.ConditionalPropertyWriter;
import com.g2rain.common.json.FailIgnoreFieldMixIn;
import com.g2rain.common.json.SuccessIgnoreFieldMixIn;
import com.g2rain.common.model.Result;
import com.g2rain.common.web.PrincipalContextHolder;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.TypeUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.FilterProvider;
import tools.jackson.databind.ser.ValueSerializerModifier;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * G2rain 自定义 Jackson HTTP 消息转换器
 * </p>
 *
 * <p>
 * <b>类功能：</b>
 * <ul>
 *     <li>对 HTTP 响应体进行 JSON 序列化</li>
 *     <li>针对 {@link Result} 类型，区分成功和失败分别使用不同 {@link JsonMapper}，避免每次序列化重复构建</li>
 *     <li>成功响应会自动注入请求上下文信息，如 {@link PrincipalContextHolder#getRequestId()} 和 {@link PrincipalContextHolder#getRequestTime()}</li>
 *     <li>支持条件字段过滤，通过 {@link SuccessIgnoreFieldMixIn} 和 {@link FailIgnoreFieldMixIn} 控制成功和失败结果字段的显示</li>
 *     <li>支持 SSE（Server-Sent Events）响应类型，使用自定义 {@link PrettyPrinter} 格式化输出，符合 SSE 行前缀规范</li>
 *     <li>线程安全：使用 {@code successLock} 和 {@code failLock} 确保多线程环境下缓存 {@link JsonMapper} 的安全初始化</li>
 *     <li>可处理 JsonView、FilterProvider，以及容器类型（List、Map、Optional）序列化</li>
 *     <li>异常处理：捕获 Jackson 序列化异常，转换为 Spring HTTP 异常，便于统一异常处理</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *     <li>适用于 Spring Boot 4.x + Jackson 3.x 的 Web 项目</li>
 *     <li>返回 {@link Result} 类型的 REST API 响应，需要区分成功/失败处理</li>
 *     <li>需要 SSE 输出的接口</li>
 *     <li>需要在 JSON 输出中控制字段显示或隐藏的场景</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>线程安全说明：</b>
 * <ul>
 *     <li>successMapper 和 failMapper 是延迟初始化的单例缓存，分别由 successLock 和 failLock 保护，确保在高并发下只创建一次</li>
 *     <li>写入 HTTP 输出流时，每次创建新的 {@link JsonGenerator}，保证多线程写出安全</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>版本信息：</b> Spring Boot 4.0.0 + Jackson 3.x
 * </p>
 *
 * @author G2rain
 * @since 2025/12/13
 */
public class G2rainJacksonHttpMessageConverter extends JacksonJsonHttpMessageConverter {

    /**
     * 是否启用 Result / BaseError MixIn 字段过滤
     * <p>
     * 如果为 true：
     * <ul>
     *     <li>成功响应会应用 {@link SuccessIgnoreFieldMixIn}，隐藏不必要或敏感字段</li>
     *     <li>失败响应会应用 {@link FailIgnoreFieldMixIn}，对错误对象进行字段过滤</li>
     * </ul>
     * </p>
     */
    private final boolean enableResultMixinFilter;

    /**
     * JSON View 注解类名提示，用于 hints 解析
     */
    private static final String JSON_VIEW_HINT = JsonView.class.getName();

    /**
     * FilterProvider 提示，用于 hints 解析
     */
    private static final String FILTER_PROVIDER_HINT = FilterProvider.class.getName();

    /**
     * SSE 输出时使用的 PrettyPrinter
     * <p>
     * 用于将每行 JSON 数据前添加 "data:" 前缀，符合 Server-Sent Events 协议要求
     * </p>
     */
    private final PrettyPrinter ssePrettyPrinter;

    /**
     * 成功响应 JsonMapper 锁，保证缓存初始化线程安全
     */
    private final Object successLock = new Object();

    /**
     * 成功响应的 JsonMapper 缓存实例，避免重复构建
     */
    private JsonMapper successMapper;

    /**
     * 失败响应 JsonMapper 锁，保证缓存初始化线程安全
     */
    private final Object failLock = new Object();

    /**
     * 失败响应的 JsonMapper 缓存实例，避免重复构建
     */
    private JsonMapper failMapper;

    /**
     * <p>
     * 默认构造函数，使用父类默认 JsonMapper
     * </p>
     *
     * @param enableResultMixinFilter 是否启用 Result / BaseError 字段过滤
     */
    public G2rainJacksonHttpMessageConverter(boolean enableResultMixinFilter) {
        super();
        this.enableResultMixinFilter = enableResultMixinFilter;
        this.ssePrettyPrinter = initSsePrettyPrinter();
    }

    /**
     * <p>
     * 构造函数，使用自定义 JsonMapper.Builder
     * </p>
     *
     * @param builder                 自定义 {@link JsonMapper.Builder} 实例
     * @param enableResultMixinFilter 是否启用 Result / BaseError 字段过滤
     */
    public G2rainJacksonHttpMessageConverter(JsonMapper.Builder builder, boolean enableResultMixinFilter) {
        super(builder);
        this.enableResultMixinFilter = enableResultMixinFilter;
        this.ssePrettyPrinter = initSsePrettyPrinter();
    }

    /**
     * <p>
     * 构造函数，直接使用指定的 {@link JsonMapper}
     * </p>
     *
     * @param mapper                  指定的 {@link JsonMapper} 实例
     * @param enableResultMixinFilter 是否启用 Result / BaseError 字段过滤
     */
    public G2rainJacksonHttpMessageConverter(JsonMapper mapper, boolean enableResultMixinFilter) {
        super(mapper);
        this.enableResultMixinFilter = enableResultMixinFilter;
        this.ssePrettyPrinter = initSsePrettyPrinter();
    }

    /**
     * 初始化 SSE PrettyPrinter
     * <p>
     * SSE 输出要求每行前添加 "data:" 前缀，因此定制 {@link DefaultPrettyPrinter}
     * </p>
     *
     * @return 定制的 {@link PrettyPrinter} 实例
     */
    private PrettyPrinter initSsePrettyPrinter() {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        // 使用两空格缩进，每行前加 data: 换行前缀
        prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
        return prettyPrinter;
    }

    /**
     * 仅对 {@link Result} 及其子类声明可写，避免抢占其他返回类型的默认 JSON 转换器。
     *
     * <p>本转换器的定位是：专门负责 {@code Result} 类型的响应增强（如上下文注入、字段过滤），
     * 其他类型仍由框架内置的 JSON 转换器处理，以保持第三方组件和非 {@code Result} 接口的行为稳定。</p>
     */
    @Override
    public boolean canWrite(ResolvableType type, @NonNull Class<?> valueClass, @Nullable MediaType mediaType) {
        // 只处理 Result 及其子类，其余一律交给后面的默认 Jackson converter
        if (!Result.class.isAssignableFrom(type.toClass())) {
            return false;
        }

        return super.canWrite(type, valueClass, mediaType);
    }

    /**
     * 写入 HTTP 响应体的核心方法
     * <p>
     * 逻辑：
     * <ul>
     *     <li>非 Result 类型调用父类默认序列化逻辑</li>
     *     <li>Result 类型：
     *         <ul>
     *             <li>成功响应注入 requestId 和 requestTime</li>
     *             <li>根据成功/失败获取或创建对应的 JsonMapper</li>
     *             <li>处理 JsonView、FilterProvider</li>
     *             <li>SSE 输出时使用定制 PrettyPrinter</li>
     *             <li>写入输出流并刷新</li>
     *         </ul>
     *     </li>
     * </ul>
     * </p>
     *
     * @param object         待序列化对象
     * @param resolvableType Spring 可解析类型
     * @param outputMessage  HTTP 输出消息
     * @param hints          可选提示参数
     * @throws IOException                     IO 异常
     * @throws HttpMessageNotWritableException Jackson 序列化失败
     */
    @Override
    protected void writeInternal(@NonNull Object object, @NonNull ResolvableType resolvableType, @NonNull HttpOutputMessage outputMessage, @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException {
        // 非 Result 类型，直接调用父类逻辑写出
        if (!(object instanceof Result<?> result)) {
            super.writeInternal(object, resolvableType, outputMessage, hints);
            return;
        }

        // 如果是成功的响应, 需要注入请求标识和请求时间
        if (result.isSuccess()) {
            result.setRequestId(PrincipalContextHolder.getRequestId());
            result.setRequestTime(PrincipalContextHolder.getRequestTime());
        }

        // 解析当前响应类型（JSON 或 text/event-stream 等）
        MediaType contentType = outputMessage.getHeaders().getContentType();
        JsonEncoding encoding = getJsonEncoding(contentType);

        // 获取或创建缓存的 JsonMapper（区分成功/失败）
        Class<?> clazz = object.getClass();
        JsonMapper mapper = getCachedMapper(result, contentType);
        Assert.state(Objects.nonNull(mapper), () -> "No JsonMapper for " + clazz.getName());

        // 获取响应输出流（防止被关闭）
        OutputStream outputStream = StreamUtils.nonClosing(outputMessage.getBody());

        Class<?> jsonView = null;
        FilterProvider filters = null;
        JavaType javaType = null;

        Type type = resolvableType.getType();
        if (TypeUtils.isAssignable(type, clazz)) {
            javaType = getJavaType(type, null);
        }

        if (Objects.nonNull(hints)) {
            jsonView = (Class<?>) hints.get(JSON_VIEW_HINT);
            filters = (tools.jackson.databind.ser.FilterProvider) hints.get(FILTER_PROVIDER_HINT);
        }

        ObjectWriter objectWriter = (Objects.nonNull(jsonView) ? mapper.writerWithView(jsonView) : mapper.writer());

        if (Objects.nonNull(filters)) {
            objectWriter = objectWriter.with(filters);
        }

        if (Objects.nonNull(javaType) && (javaType.isContainerType() || javaType.isTypeOrSubTypeOf(Optional.class))) {
            objectWriter = objectWriter.forType(javaType);
        }

        SerializationConfig config = objectWriter.getConfig();
        if (Objects.nonNull(contentType) && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) && config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            objectWriter = objectWriter.with(this.ssePrettyPrinter);
        }

        objectWriter = customizeWriter(objectWriter, javaType, contentType);

        try (JsonGenerator generator = objectWriter.createGenerator(outputStream, encoding)) {
            // 可扩展前置逻辑
            writePrefix(generator, object);
            // 序列化输出
            objectWriter.writeValue(generator, object);
            // 可扩展后置逻辑
            writeSuffix(generator, object);
            generator.flush();
        } catch (InvalidDefinitionException ex) {
            throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
        } catch (JacksonException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getOriginalMessage(), ex);
        }
    }

    /**
     * 获取缓存的 {@link JsonMapper}
     * <p>
     * 成功和失败分别使用不同的缓存实例，避免每次序列化都重新构建
     * 线程安全：使用 successLock / failLock 确保单例初始化
     * </p>
     *
     * @param result          Result 对象
     * @param targetMediaType 响应媒体类型
     * @return 缓存的 JsonMapper
     */
    private JsonMapper getCachedMapper(Result<?> result, MediaType targetMediaType) {
        boolean success = result.isSuccess();
        // 根据 Result 状态选择缓存
        JsonMapper cached = success ? successMapper : failMapper;
        // 若已缓存，直接返回
        if (Objects.nonNull(cached)) {
            return cached;
        }

        // 不同状态使用不同锁，避免阻塞对方路径
        synchronized (success ? successLock : failLock) {
            // 二次检查，避免多线程重复初始化
            cached = success ? successMapper : failMapper;
            if (Objects.isNull(cached)) {
                // 根据当前响应结果的类型以及媒体类型选择对应的基础 JsonMapper，并创建副本
                JsonMapper.Builder builder = resolveJsonMapper(result.getClass(), targetMediaType).rebuild();
                // 如果响应成功
                if (success) {
                    // 为 Builder 添加自定义模块，并设置 ValueSerializerModifier 用于动态替换 BeanPropertyWriter
                    builder.addModule(new SimpleModule().setSerializerModifier(new ValueSerializerModifier() {
                        /**
                         * 在序列化过程中调用，用于修改 BeanPropertyWriter 列表, 这里我们将每个字段的 Writer 包装为 ConditionalPropertyWriter，实现动态条件序列化
                         */
                        @Override
                        public List<BeanPropertyWriter> changeProperties(SerializationConfig c, BeanDescription.Supplier s, List<BeanPropertyWriter> w) {
                            return w.stream().map(ConditionalPropertyWriter::new).collect(Collectors.toList());
                        }
                    }));

                    // 根据开关决定是否给 Result 类添加 MixIn, 实现特定字段忽略规则
                    if (enableResultMixinFilter) {
                        builder.addMixIn(Result.class, SuccessIgnoreFieldMixIn.class);
                    }

                    cached = builder.build();
                    // 写回缓存
                    successMapper = cached;
                } else {
                    // 根据开关决定是否给 Result 和 BaseError 类添加 MixIn, 实现特定字段忽略规则
                    if (enableResultMixinFilter) {
                        builder.addMixIn(Result.class, FailIgnoreFieldMixIn.class)
                            .addMixIn(BaseError.class, FailIgnoreFieldMixIn.class);
                    }

                    cached = builder.build();
                    // 写回缓存
                    failMapper = cached;
                }
            }
        }
        return cached;
    }

    /**
     * 根据目标类型和媒体类型解析合适的 {@link JsonMapper}
     * <p>
     * 支持多媒体类型映射，可根据实际类型选择对应 JsonMapper
     * 如果找不到匹配，返回默认 JsonMapper
     * </p>
     *
     * @param targetType      目标对象类型
     * @param targetMediaType 响应媒体类型
     * @return JsonMapper 实例
     */
    private JsonMapper resolveJsonMapper(Class<?> targetType, @Nullable MediaType targetMediaType) {
        // 如果未指定媒体类型，使用默认 Mapper
        if (Objects.isNull(targetMediaType)) {
            return this.defaultMapper;
        }

        // 获取目标类型对应的所有 JsonMapper 映射表
        Map<MediaType, JsonMapper> jsonMappers = getMappersForType(targetType);
        // 遍历映射表，查找与 targetMediaType 匹配的 Mapper
        for (Map.Entry<MediaType, JsonMapper> jsonMapperEntry : jsonMappers.entrySet()) {
            if (jsonMapperEntry.getKey().includes(targetMediaType)) {
                // 返回匹配的 Mapper
                return jsonMapperEntry.getValue();
            }
        }

        // 默认回退到系统默认 JsonMapper
        return this.defaultMapper;
    }
}
