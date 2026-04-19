package com.g2rain.web.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class G2rainWebPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(Config.class);

    @Test
    void testDefaultProperties() {
        contextRunner.run(context -> {
            G2rainWebProperties properties = context.getBean(G2rainWebProperties.class);
            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getHttpWrapperFilterOrder()).isEqualTo(100);
            assertThat(properties.getPrincipalContextFilterOrder()).isEqualTo(200);
            assertThat(properties.getAccessLogFilterOrder()).isEqualTo(300);
            assertThat(properties.getLoginGuardInterceptorOrder()).isEqualTo(400);
            assertThat(properties.getIdentityParamInjectorOrder()).isEqualTo(500);
        });
    }

    @Test
    void testCustomProperties() {
        contextRunner
            .withPropertyValues(
                "g2rain.web.enabled=false",
                "g2rain.web.http-wrapper-filter-order=10",
                "g2rain.web.principal-context-filter-order=20",
                "g2rain.web.access-log-filter-order=30",
                "g2rain.web.login-guard-interceptor-order=40",
                "g2rain.web.identity-param-injector-order=50"
            )
            .run(context -> {
                G2rainWebProperties properties = context.getBean(G2rainWebProperties.class);
                assertThat(properties.isEnabled()).isFalse();
                assertThat(properties.getHttpWrapperFilterOrder()).isEqualTo(10);
                assertThat(properties.getPrincipalContextFilterOrder()).isEqualTo(20);
                assertThat(properties.getAccessLogFilterOrder()).isEqualTo(30);
                assertThat(properties.getLoginGuardInterceptorOrder()).isEqualTo(40);
                assertThat(properties.getIdentityParamInjectorOrder()).isEqualTo(50);
            });
    }

    @Configuration
    @EnableConfigurationProperties(G2rainWebProperties.class)
    static class Config {
    }
}
