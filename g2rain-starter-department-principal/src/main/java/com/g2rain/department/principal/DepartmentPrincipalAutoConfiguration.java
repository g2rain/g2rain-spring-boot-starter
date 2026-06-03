package com.g2rain.department.principal;

import com.g2rain.department.principal.support.DepartmentPrincipalOpenFeign;
import com.g2rain.department.principal.support.DepartmentPrincipalRestClient;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

/**
 * 部门 Principal 增强自动配置。
 *
 * <p>引入该 starter 后，会注册 {@link DepartmentPrincipalEnricher}，在登录态构建阶段通过部门服务
 * 查询当前用户的部门路径，并写入 Principal。</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(DepartmentPrincipalProperties.class)
@ConditionalOnProperty(prefix = "g2rain.principal.department", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration")
public class DepartmentPrincipalAutoConfiguration {

    /**
     * 负载均衡 RestClient 构建器配置。
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.cloud.client.loadbalancer.LoadBalanced")
    public static class RestClientLoadBalancerConfiguration {

        /**
         * 注册带负载均衡能力的 RestClient 构建器。
         *
         * @return RestClient.Builder
         */
        @Bean
        @LoadBalanced
        @ConditionalOnMissingBean(annotation = LoadBalanced.class)
        public RestClient.Builder restClientBuilder() {
            return RestClient.builder();
        }
    }

    /**
     * Feign 客户端激活配置。
     */
    @Configuration(proxyBeanMethods = false)
    @EnableFeignClients(clients = DepartmentPrincipalOpenFeign.class)
    @ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
    public static class FeignActivationConfiguration {

    }

    /**
     * 在未启用 Feign 时创建 HTTP Interface 客户端实现。
     *
     * @param builder     默认 RestClient 构建器
     * @param props       部门 Principal 增强配置
     * @param beanFactory Bean 工厂
     * @return 部门 Principal 增强客户端
     */
    @Bean
    @ConditionalOnMissingClass("org.springframework.cloud.openfeign.FeignClient")
    @ConditionalOnMissingBean(DepartmentPrincipalClient.class)
    public DepartmentPrincipalClient departmentPrincipalRestClient(RestClient.Builder builder,
                                                                  DepartmentPrincipalProperties props,
                                                                  ConfigurableBeanFactory beanFactory) {
        RestClient.Builder lbBuilder = null;
        try {
            lbBuilder = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
                beanFactory, RestClient.Builder.class,
                "org.springframework.cloud.client.loadbalancer.LoadBalanced"
            );
        } catch (Exception ignored) {
            // 没找到则保持为 null
        }

        RestClient.Builder finalBuilder = Objects.nonNull(lbBuilder) ? lbBuilder : builder;
        String baseAddress = StringUtils.hasText(props.getServiceUrl())
            ? props.getServiceUrl()
            : "http://" + props.getServiceName();
        String rootUrl = UriComponentsBuilder.fromUriString(baseAddress)
            .path(props.getServicePath())
            .toUriString();

        return HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(finalBuilder.baseUrl(rootUrl).build()))
            .embeddedValueResolver(beanFactory::resolveEmbeddedValue)
            .build()
            .createClient(DepartmentPrincipalRestClient.class);
    }

    /**
     * 部门 Principal 增强器。
     *
     * @param client 部门 Principal 增强客户端
     * @return PrincipalEnricher 实现
     */
    @Bean
    @ConditionalOnMissingBean(DepartmentPrincipalEnricher.class)
    public DepartmentPrincipalEnricher departmentPrincipalEnricher(DepartmentPrincipalClient client) {
        return new DepartmentPrincipalEnricher(client);
    }
}
