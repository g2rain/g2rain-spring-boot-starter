package com.g2rain.web.autoconfigure;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * G2rain Web 模块配置属性。
 * <p>
 * 通过该配置可以开启或关闭各类 Filter、Interceptor、ExceptionHandler 以及自定义 Converter，
 * 并控制它们的执行顺序。order 值越小，优先级越高。
 * </p>
 * <p>
 * 属性分组：
 * <ul>
 *   <li>Filters：HTTP 请求/响应处理相关</li>
 *   <li>Interceptors：Spring MVC 拦截器</li>
 *   <li>Exception：全局异常处理开关</li>
 *   <li>Converters：自定义类型转换器</li>
 * </ul>
 * </p>
 *
 * @author alpha
 * @since 2025/10/17
 */
@Data
@ConfigurationProperties(prefix = "g2rain.web")
public class G2rainWebProperties {
    /**
     * Web 模块总开关
     */
    private boolean enabled = true;

    // ========================= Filters =========================

    /**
     * HTTP 请求响应包装器 Filter 是否启用
     */
    private boolean httpWrapperFilterEnabled = true;

    /**
     * HTTP 请求响应包装器 Filter 执行顺序
     */
    private int httpWrapperFilterOrder = 100;

    /**
     * 身份主体作用域 Filter 是否启用
     */
    private boolean principalContextScopeFilterEnabled = true;

    /**
     * 身份主体作用域 Filter 执行顺序
     */
    private int principalContextScopeFilterOrder = 120;

    /**
     * 全局异常 Filter 是否启用
     */
    private boolean globalExceptionFilterEnabled = true;

    /**
     * 是否在全局异常 Filter 中使用自定义 JsonCodec（MixIn 等复杂序列化）
     */
    private boolean globalExceptionFilterUseCustomJson = false;

    /**
     * 全局异常 Filter 执行顺序
     */
    private int globalExceptionFilterOrder = 150;

    /**
     * 身份主体上下文注入 Filter 是否启用
     */
    private boolean principalContextFilterEnabled = true;

    /**
     * 身份主体上下文注入 Filter 执行顺序
     */
    private int principalContextFilterOrder = 200;

    /**
     * 访问日志 Filter 是否启用
     */
    private boolean accessLogFilterEnabled = true;

    /**
     * 访问日志 Filter 执行顺序
     */
    private int accessLogFilterOrder = 300;


    // ========================= Interceptors =========================

    /**
     * 登录校验拦截器是否启用
     */
    private boolean loginGuardInterceptorEnabled = true;

    /**
     * 登录校验拦截器执行顺序
     */
    private int loginGuardInterceptorOrder = 400;

    /**
     * 身份参数注入拦截器是否启用
     */
    private boolean identityParamInjectorEnabled = true;

    /**
     * 身份参数注入拦截器执行顺序
     */
    private int identityParamInjectorOrder = 500;


    // ========================= Exception =========================

    /**
     * 全局异常处理器是否启用
     */
    private boolean globalExceptionHandlerEnabled = true;


    // ========================= Converters =========================

    /**
     * 是否启用 Result / BaseError MixIn 字段过滤
     */
    private boolean enableResultMixinFilter = false;
}
