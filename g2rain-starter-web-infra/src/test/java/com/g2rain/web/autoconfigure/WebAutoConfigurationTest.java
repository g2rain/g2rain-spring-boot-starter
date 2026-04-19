package com.g2rain.web.autoconfigure;

import com.g2rain.common.exception.DefaultExceptionProcessor;
import com.g2rain.web.converters.G2rainJacksonHttpMessageConverter;
import com.g2rain.web.filters.AccessLogFilter;
import com.g2rain.web.filters.GlobalExceptionFilter;
import com.g2rain.web.filters.HttpWrapperFilter;
import com.g2rain.web.filters.PrincipalContextFilter;
import com.g2rain.web.interceptors.IdentityParamInjector;
import com.g2rain.web.interceptors.LoginGuardInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Web自动配置测试")
public class WebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WebAutoConfiguration.class))
        // 添加测试配置以提供JsonMapper Bean
        .withUserConfiguration(TestConfig.class);

    @Configuration
    static class TestConfig {
        @Bean
        public JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }
    }

    @Test
    @DisplayName("测试自动配置加载")
    void testAutoConfigurationLoaded() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WebAutoConfiguration.class);
            assertThat(context).hasSingleBean(G2rainWebProperties.class);
        });
    }

    @Test
    @DisplayName("测试HTTP包装器过滤器Bean")
    void testHttpWrapperFilterBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HttpWrapperFilter.class);
            assertThat(context).hasBean("httpWrapperFilterRegistration");
            assertThat(context).getBean("httpWrapperFilterRegistration", FilterRegistrationBean.class);
        });
    }

    @Test
    @DisplayName("测试主体上下文过滤器Bean")
    void testPrincipalContextFilterBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(PrincipalContextFilter.class);
            assertThat(context).hasBean("principalContextFilterRegistration");
            assertThat(context).getBean("principalContextFilterRegistration", FilterRegistrationBean.class);
        });
    }

    @Test
    @DisplayName("测试访问日志过滤器Bean")
    void testAccessLogFilterBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AccessLogFilter.class);
            assertThat(context).hasBean("accessLogFilterRegistration");
            assertThat(context).getBean("accessLogFilterRegistration", FilterRegistrationBean.class);
        });
    }

    @Test
    @DisplayName("测试拦截器Bean")
    void testInterceptorBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(LoginGuardInterceptor.class);
            assertThat(context).hasSingleBean(IdentityParamInjector.class);
        });
    }

    @Test
    @DisplayName("测试异常处理Bean")
    void testExceptionHandlingBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DefaultExceptionProcessor.class);
            assertThat(context).hasSingleBean(GlobalExceptionFilter.class);
            assertThat(context).hasBean("exceptionFilterRegistration");
            assertThat(context).getBean("exceptionFilterRegistration", FilterRegistrationBean.class);
        });
    }

    @Test
    @DisplayName("测试转换器Bean")
    void testConverterBeans() {
        contextRunner.withPropertyValues(
                "g2rain.web.g2rain-jackson-converter-enabled=true")
            .run(context -> {
                assertThat(context).hasSingleBean(WebMvcConfigurer.class);
                assertThat(context).hasSingleBean(G2rainJacksonHttpMessageConverter.class);
            });
    }

    @Test
    @DisplayName("测试带属性的配置")
    void testConfigurationWithProperties() {
        contextRunner
            .withPropertyValues(
                "g2rain.web.enabled=true",
                "g2rain.web.http-wrapper-filter-order=50",
                "g2rain.web.principal-context-filter-order=100",
                "g2rain.web.global-exception-filter-order=150"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(WebAutoConfiguration.class);
            });
    }

    @Test
    @DisplayName("测试禁用配置")
    void testConfigurationDisabled() {
        contextRunner
            .withPropertyValues("g2rain.web.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(WebAutoConfiguration.class);
            });
    }
}
