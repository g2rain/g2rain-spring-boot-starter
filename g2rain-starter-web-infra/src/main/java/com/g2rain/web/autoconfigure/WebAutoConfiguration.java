package com.g2rain.web.autoconfigure;


import com.g2rain.common.exception.DefaultExceptionProcessor;
import com.g2rain.common.exception.ErrorMessageRegistry;
import com.g2rain.common.exception.ExceptionProcessor;
import com.g2rain.web.converters.G2rainJacksonHttpMessageConverter;
import com.g2rain.web.exception.GlobalExceptionHandler;
import com.g2rain.web.filters.AccessLogFilter;
import com.g2rain.web.filters.GlobalExceptionFilter;
import com.g2rain.web.filters.HttpWrapperFilter;
import com.g2rain.web.filters.PrincipalContextFilter;
import com.g2rain.web.filters.PrincipalContextScopeFilter;
import com.g2rain.web.interceptors.IdentityParamInjector;
import com.g2rain.web.interceptors.LoginGuardInterceptor;
import lombok.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Role;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

/**
 * G2rain Web 模块自动配置。
 * <p>
 * 该配置类会根据 {@link G2rainWebProperties} 中各个开关，注册 Filter、Interceptor、ExceptionProcessor
 * 以及自定义类型转换器和 Jackson Converter。
 * <p>
 * 使用说明：
 * <ul>
 *     <li>通过配置 {@code g2rain.web.enabled=false} 可以关闭整个 Web 模块</li>
 *     <li>各 Filter、Interceptor、ExceptionHandler、Converter 都有独立开关和顺序控制</li>
 * </ul>
 * <p>
 * order 值越小优先级越高。
 * </p>
 *
 * @author alpha
 * @since 2025/10/17
 */
@AutoConfiguration
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(G2rainWebProperties.class)
@ConditionalOnProperty(prefix = "g2rain.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebAutoConfiguration implements WebMvcConfigurer {

    /**
     * Web 模块配置属性
     */
    private final G2rainWebProperties properties;

    /**
     * 构造函数
     *
     * @param properties Web 配置属性
     */
    public WebAutoConfiguration(G2rainWebProperties properties) {
        this.properties = properties;
    }

    /**
     * 注册 {@link HttpWrapperFilter} Bean。
     *
     * @return HttpWrapperFilter 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(HttpWrapperFilter.class)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "http-wrapper-filter-enabled", havingValue = "true", matchIfMissing = true)
    public HttpWrapperFilter httpWrapperFilter() {
        return new HttpWrapperFilter();
    }

    /**
     * 注册 {@link HttpWrapperFilter} 的 {@link FilterRegistrationBean}。
     *
     * @param filter HttpWrapperFilter 实例
     * @return FilterRegistrationBean
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "http-wrapper-filter-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<@NonNull HttpWrapperFilter> httpWrapperFilterRegistration(HttpWrapperFilter filter) {
        FilterRegistrationBean<@NonNull HttpWrapperFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + properties.getHttpWrapperFilterOrder());
        registration.addUrlPatterns("/*"); // 默认全部路径，可扩展
        return registration;
    }

    /**
     * 注册 {@link PrincipalContextScopeFilter} Bean。
     *
     * @return PrincipalContextScopeFilter 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(PrincipalContextScopeFilter.class)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "principal-context-scope-filter-enabled", havingValue = "true", matchIfMissing = true)
    public PrincipalContextScopeFilter principalContextScopeFilter() {
        return new PrincipalContextScopeFilter();
    }

    /**
     * 注册 {@link PrincipalContextScopeFilter} 的 {@link FilterRegistrationBean}。
     *
     * @param filter PrincipalContextScopeFilter 实例
     * @return FilterRegistrationBean
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "principal-context-scope-filter-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<@NonNull PrincipalContextScopeFilter> principalContextScopeFilterRegistration(PrincipalContextScopeFilter filter) {
        FilterRegistrationBean<@NonNull PrincipalContextScopeFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + properties.getPrincipalContextScopeFilterOrder());
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 注册 {@link DefaultExceptionProcessor} Bean。
     *
     * @return MicroExceptionProcessor 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(ExceptionProcessor.class)
    public ExceptionProcessor defaultExceptionProcessor(ObjectProvider<@NonNull ErrorMessageRegistry> provider) {
        return new DefaultExceptionProcessor(provider.getIfAvailable());
    }

    /**
     * 注册 {@link GlobalExceptionFilter} Bean。
     *
     * @return GlobalHandlerExceptionResolver 实例
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(GlobalExceptionFilter.class)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "global-exception-filter-enabled", havingValue = "true", matchIfMissing = true)
    public GlobalExceptionFilter globalExceptionFilter(ExceptionProcessor exceptionProcessor) {
        return new GlobalExceptionFilter(exceptionProcessor, properties);
    }

    /**
     * 注册全局异常拦截 Filter。
     * <p>
     * 该 Filter 拦截所有请求（/*），用于捕获和处理整个请求链路中未被捕获的异常，
     * 并统一返回 JSON 响应或进行相应处理。
     * <p>
     * 设置为最高优先级（Ordered.HIGHEST_PRECEDENCE），确保在其他 Filter 之前执行。
     *
     * @param filter 已创建的 GlobalExceptionFilter 实例
     * @return 配置好的 FilterRegistrationBean 对象，用于注册全局异常 Filter
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "global-exception-filter-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<@NonNull GlobalExceptionFilter> exceptionFilterRegistration(GlobalExceptionFilter filter) {
        FilterRegistrationBean<@NonNull GlobalExceptionFilter> exceptionFilterRegistration = new FilterRegistrationBean<>(filter);
        exceptionFilterRegistration.setOrder(Ordered.HIGHEST_PRECEDENCE + properties.getGlobalExceptionFilterOrder());
        exceptionFilterRegistration.addUrlPatterns("/*");
        return exceptionFilterRegistration;
    }

    /**
     * 注册 {@link PrincipalContextFilter} Bean。
     *
     * @return PrincipalContextFilter 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(PrincipalContextFilter.class)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "principal-context-filter-enabled", havingValue = "true", matchIfMissing = true)
    public PrincipalContextFilter principalContextFilter() {
        return new PrincipalContextFilter();
    }

    /**
     * 注册 {@link PrincipalContextFilter} 的 {@link FilterRegistrationBean}。
     *
     * @param filter PrincipalContextFilter 实例
     * @return FilterRegistrationBean
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "principal-context-filter-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<@NonNull PrincipalContextFilter> principalContextFilterRegistration(PrincipalContextFilter filter) {
        FilterRegistrationBean<@NonNull PrincipalContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + properties.getPrincipalContextFilterOrder());
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 注册 {@link AccessLogFilter} Bean。
     *
     * @return AccessLogFilter 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(AccessLogFilter.class)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "access-log-filter-enabled", havingValue = "true", matchIfMissing = true)
    public AccessLogFilter accessLogFilter() {
        return new AccessLogFilter();
    }

    /**
     * 注册 {@link AccessLogFilter} 的 {@link FilterRegistrationBean}。
     *
     * @param filter AccessLogFilter 实例
     * @return FilterRegistrationBean
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "access-log-filter-enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<@NonNull AccessLogFilter> accessLogFilterRegistration(AccessLogFilter filter) {
        FilterRegistrationBean<@NonNull AccessLogFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + properties.getAccessLogFilterOrder());
        registration.addUrlPatterns("/*");
        return registration;
    }

    /**
     * 注册 {@link LoginGuardInterceptor} Bean。
     *
     * @return LoginGuardInterceptor 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(LoginGuardInterceptor.class)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "login-guard-interceptor-enabled", havingValue = "true", matchIfMissing = true)
    public LoginGuardInterceptor loginGuardInterceptor() {
        return new LoginGuardInterceptor();
    }

    /**
     * 注册 {@link IdentityParamInjector} Bean。
     *
     * @return IdentityParamInjector 实例
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(IdentityParamInjector.class)
    @ConditionalOnProperty(prefix = "g2rain.web", name = "identity-param-injector-enabled", havingValue = "true", matchIfMissing = true)
    public IdentityParamInjector identityParamInjector() {
        return new IdentityParamInjector();
    }

    /**
     * 注册 Spring MVC 拦截器。
     * <p>
     * 1. loginGuardInterceptor：用于校验用户登录状态，优先级由 properties 配置偏移量控制，
     * 默认放在最前面（HIGHEST_PRECEDENCE + offset）。
     * 2. identityParamInjector：用于注入身份相关参数，同样通过 properties 控制顺序。
     *
     * @param registry InterceptorRegistry，用于注册拦截器
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        if (properties.isLoginGuardInterceptorEnabled()) {
            registry.addInterceptor(loginGuardInterceptor()).order(
                Ordered.HIGHEST_PRECEDENCE + properties.getLoginGuardInterceptorOrder()
            );
        }

        if (properties.isIdentityParamInjectorEnabled()) {
            registry.addInterceptor(identityParamInjector()).order(
                Ordered.HIGHEST_PRECEDENCE + properties.getIdentityParamInjectorOrder()
            );
        }
    }

    /**
     * 注册自定义 {@link G2rainJacksonHttpMessageConverter}，替代默认的 MappingJackson2HttpMessageConverter。
     *
     * <p>通过注入 Spring Boot 全局 {@link JsonMapper} 构造 G2rainConverter，
     * 保证全局配置生效，包括模块、日期格式、序列化规则等。</p>
     *
     * <p>使用 {@link Primary} 注解确保本 Converter 优先被 Spring MVC 使用，
     * 完全替代默认 Converter，无需再操作 converters 列表。</p>
     *
     * @param mapper Spring Boot 容器中已有的全局 JsonMapper
     * @return 已初始化的 {@link G2rainJacksonHttpMessageConverter} Bean
     */
    @Bean
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(G2rainJacksonHttpMessageConverter.class)
    public G2rainJacksonHttpMessageConverter g2rainJacksonHttpMessageConverter(JsonMapper mapper) {
        return new G2rainJacksonHttpMessageConverter(mapper, properties.isEnableResultMixinFilter());
    }
}
