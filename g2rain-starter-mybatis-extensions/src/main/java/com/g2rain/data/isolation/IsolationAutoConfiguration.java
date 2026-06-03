package com.g2rain.data.isolation;


import com.g2rain.data.isolation.processor.IsolationInsertProcessor;
import com.g2rain.data.isolation.processor.IsolationQueryProcessor;
import com.g2rain.data.isolation.support.CachedDataPermissionPolicyResolver;
import com.g2rain.data.isolation.support.CachedDataScopeExaminer;
import com.g2rain.data.isolation.support.DataPermissionPolicyOpenFeign;
import com.g2rain.data.isolation.support.DataPermissionPolicyRestClient;
import com.g2rain.data.isolation.support.DefaultDataPermissionPolicyResolver;
import com.g2rain.data.isolation.support.DefaultDataScopeExaminer;
import com.g2rain.data.isolation.support.OrganHierarchyOpenFeign;
import com.g2rain.data.isolation.support.OrganHierarchyRestClient;
import com.g2rain.mybatis.extension.ExecutorCompositeInterceptor;
import com.g2rain.mybatis.pagination.PaginationQueryProcessor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Objects;

/**
 * MyBatis 扩展能力自动装配入口。
 * <p>
 * 该自动配置围绕「分页 + 数据隔离」两类能力构建统一拦截器链，核心职责如下：
 * </p>
 * <ul>
 *     <li><b>分页能力装配</b>：注册 {@link PaginationQueryProcessor}，并接入 {@link ExecutorCompositeInterceptor}。</li>
 *     <li><b>数据隔离能力装配</b>：按条件注册查询、插入、更新/删除三类隔离处理器，
 *     分别在执行器阶段与 SQL prepare 阶段生效。</li>
 *     <li><b>组织层级客户端装配</b>：优先启用 OpenFeign 客户端；无 Feign 时自动回退到 RestClient HTTP Interface。</li>
 *     <li><b>数据范围校验链装配</b>：注册默认校验器 {@link DefaultDataScopeExaminer}，
 *     并通过 {@link CachedDataScopeExaminer} 提供本地缓存增强。</li>
 *     <li><b>Mapper 扫描增强</b>：注册 {@link MybatisMapperCustomizer}，
 *     用于替换 MyBatis 默认 MapperRegistry，实现数据隔离注解元信息缓存。</li>
 * </ul>
 *
 * <p>生效条件与顺序：</p>
 * <ul>
 *     <li>仅在类路径存在 {@link SqlSessionFactory} 时生效（MyBatis 环境）。</li>
 *     <li>通过 {@link AutoConfigureBefore} 保证在 {@link MybatisAutoConfiguration} 前完成关键 Bean 注册。</li>
 *     <li>数据隔离子配置受 {@code g2rain.data.isolation.enabled} 控制，默认开启。</li>
 * </ul>
 *
 * <p>设计目标：</p>
 * <ul>
 *     <li>在不侵入业务代码的前提下提供统一、可组合、可替换的插件化增强能力。</li>
 *     <li>通过 {@code @ConditionalOnMissingBean} 保证用户自定义实现优先，避免自动配置抢占。</li>
 * </ul>
 *
 * @author 孙兴宝
 * @version 1.0
 */
@AutoConfiguration
@ConditionalOnClass(SqlSessionFactory.class)
@AutoConfigureBefore(MybatisAutoConfiguration.class)
@EnableConfigurationProperties(IsolationProperties.class)
public class IsolationAutoConfiguration {

    /**
     * 分页查询处理器。
     *
     * @return 分页处理器实例
     */
    @Bean
    @ConditionalOnMissingBean(PaginationQueryProcessor.class)
    public PaginationQueryProcessor paginationQueryProcessor() {
        return new PaginationQueryProcessor(20000);
    }

    /**
     * 数据隔离能力子配置。
     * <p>
     * 该配置块在开启数据隔离时生效，负责装配组织范围校验、SQL 改写处理器、
     * 以及 StatementHandler 阶段拦截器。
     * </p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "g2rain.data.isolation", name = "enabled", havingValue = "true", matchIfMissing = true)
    @AutoConfigureAfter(name = "org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration")
    public static class DataIsolationConfiguration {

        /**
         * 负载均衡 RestClient 构建器配置。
         * <p>
         * 当环境具备 Spring Cloud LoadBalancer 时，为 RestClient 提供 {@link LoadBalanced}
         * 版本的构建器，供无 Feign 场景下的 HTTP Interface 客户端复用。
         * </p>
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
         * 默认组织层级客户端实现配置。
         * <p>
         * 仅在用户未自定义 {@link DataScopeExaminer} 时生效：
         * Feign 可用时启用 Feign 客户端；否则回退为 RestClient 客户端。
         * </p>
         */
        @Configuration(proxyBeanMethods = false)
        @Conditional(OnDefaultClientCondition.class)
        public static class DefaultClientImplementationConfiguration {

            /**
             * Feign 客户端激活配置。
             * <p>
             * 当类路径存在 Feign 时，启用 {@link OrganHierarchyOpenFeign} 作为默认组织关系客户端。
             * </p>
             */
            @Configuration(proxyBeanMethods = false)
            @EnableFeignClients(clients = OrganHierarchyOpenFeign.class)
            @ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
            public static class FeignActivationConfiguration {

            }

            /**
             * 在未启用 Feign 时创建 HTTP Interface 客户端实现。
             *
             * @param builder     默认 RestClient 构建器
             * @param props       隔离配置
             * @param beanFactory Bean 工厂
             * @return 组织层级关系客户端
             */
            @Bean
            @ConditionalOnMissingClass("org.springframework.cloud.openfeign.FeignClient")
            public OrganHierarchyClient organHierarchyRestClient(RestClient.Builder builder,
                                                                 IsolationProperties props,
                                                                 ConfigurableBeanFactory beanFactory) {

                // 1. 尝试寻找带负载均衡标记的 Builder
                RestClient.Builder lbBuilder = null;
                try {
                    lbBuilder = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
                        beanFactory, RestClient.Builder.class,
                        "org.springframework.cloud.client.loadbalancer.LoadBalanced"
                    );
                } catch (Exception ignored) {
                    // 没找到则保持为 null
                }

                // 2. 【关键】如果找到了就用 lbBuilder，没找到（返回 null）就用方法参数里的默认 builder
                // 这样在没有负载均衡环境时，依然可以根据 serviceUrl 正常工作
                RestClient.Builder finalBuilder = Objects.nonNull(lbBuilder) ? lbBuilder : builder;

                // noinspection
                String baseAddress = StringUtils.hasText(props.getServiceUrl())
                    ? props.getServiceUrl()
                    : "http://" + props.getServiceName();

                // 2. 使用你源码中确认存在的 fromUriString 方法, 它会自动解析 scheme, host, port 等信息
                String rootUrl = UriComponentsBuilder.fromUriString(baseAddress)
                    .path(props.getServicePath()) // 智能拼接 servicePath
                    .toUriString();

                // 3. 创建代理工厂并激活占位符解析
                return HttpServiceProxyFactory
                    .builderFor(RestClientAdapter.create(finalBuilder.baseUrl(rootUrl).build()))
                    .embeddedValueResolver(beanFactory::resolveEmbeddedValue)
                    .build()
                    .createClient(OrganHierarchyRestClient.class);
            }

            /**
             * 默认数据范围校验器。
             *
             * @param client 组织层级关系客户端
             * @return 数据范围校验器
             */
            @Bean
            @ConditionalOnBean(OrganHierarchyClient.class)
            public DataScopeExaminer defaultDataScopeExaminer(OrganHierarchyClient client) {
                return new DefaultDataScopeExaminer(client);
            }
        }

        /**
         * 数据权限策略远程客户端配置（仿组织层级客户端，独立装配）。
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnMissingBean(DataPermissionPolicyClient.class)
        public static class PolicyClientConfiguration {

            @Configuration(proxyBeanMethods = false)
            @EnableFeignClients(clients = DataPermissionPolicyOpenFeign.class)
            @ConditionalOnClass(name = "org.springframework.cloud.openfeign.FeignClient")
            public static class FeignPolicyActivationConfiguration {

            }

            @Bean
            @ConditionalOnMissingClass("org.springframework.cloud.openfeign.FeignClient")
            public DataPermissionPolicyClient dataPermissionPolicyRestClient(
                RestClient.Builder builder,
                IsolationProperties props,
                ConfigurableBeanFactory beanFactory
            ) {
                RestClient.Builder lbBuilder = null;
                try {
                    lbBuilder = BeanFactoryAnnotationUtils.qualifiedBeanOfType(
                        beanFactory, RestClient.Builder.class,
                        "org.springframework.cloud.client.loadbalancer.LoadBalanced"
                    );
                } catch (Exception ignored) {
                    // ignore
                }

                RestClient.Builder finalBuilder = Objects.nonNull(lbBuilder) ? lbBuilder : builder;
                // noinspection
                String baseAddress = StringUtils.hasText(props.getPolicyServiceUrl())
                    ? props.getPolicyServiceUrl()
                    : "http://" + props.getPolicyServiceName();

                String rootUrl = UriComponentsBuilder.fromUriString(baseAddress)
                    .path(props.getPolicyServicePath())
                    .toUriString();

                return HttpServiceProxyFactory
                    .builderFor(RestClientAdapter.create(finalBuilder.baseUrl(rootUrl).build()))
                    .embeddedValueResolver(beanFactory::resolveEmbeddedValue)
                    .build()
                    .createClient(DataPermissionPolicyRestClient.class);
            }
        }

        /**
         * 默认数据权限策略解析器。
         */
        @Bean
        @ConditionalOnBean(DataPermissionPolicyClient.class)
        @ConditionalOnMissingBean(DataPermissionPolicyResolver.class)
        public DataPermissionPolicyResolver defaultDataPermissionPolicyResolver(DataPermissionPolicyClient client) {
            return new DefaultDataPermissionPolicyResolver(client);
        }

        /**
         * 缓存增强的数据权限策略解析器。
         */
        @Bean
        @ConditionalOnBean(DataPermissionPolicyResolver.class)
        @ConditionalOnMissingBean(CachedDataPermissionPolicyResolver.class)
        public CachedDataPermissionPolicyResolver cachedDataPermissionPolicyResolver(DataPermissionPolicyResolver dataPermissionPolicyResolver) {
            return new CachedDataPermissionPolicyResolver(dataPermissionPolicyResolver);
        }

        /**
         * 缓存增强的数据范围校验器。
         *
         * @param dataScopeExaminer 原始校验器
         * @return 缓存校验器
         */
        @Bean
        @ConditionalOnMissingBean(CachedDataScopeExaminer.class)
        public CachedDataScopeExaminer cachedDataScopeExaminer(DataScopeExaminer dataScopeExaminer) {
            return new CachedDataScopeExaminer(dataScopeExaminer);
        }

        /**
         * 查询隔离处理器。
         *
         * @param cachedDataScopeExaminer            缓存校验器
         * @param cachedDataPermissionPolicyResolver 缓存策略解析器
         * @return 查询隔离处理器
         */
        @Bean
        @ConditionalOnMissingBean(IsolationQueryProcessor.class)
        @ConditionalOnBean({CachedDataScopeExaminer.class, CachedDataPermissionPolicyResolver.class})
        public IsolationQueryProcessor isolationQueryProcessor(CachedDataScopeExaminer cachedDataScopeExaminer, CachedDataPermissionPolicyResolver cachedDataPermissionPolicyResolver) {
            return new IsolationQueryProcessor(cachedDataScopeExaminer, cachedDataPermissionPolicyResolver, 10000);
        }

        /**
         * 插入隔离处理器。
         *
         * @param cachedDataScopeExaminer 缓存校验器
         * @return 插入隔离处理器
         */
        @Bean
        @ConditionalOnMissingBean(IsolationInsertProcessor.class)
        @ConditionalOnBean({CachedDataScopeExaminer.class, CachedDataPermissionPolicyResolver.class})
        public IsolationInsertProcessor isolationInsertProcessor(CachedDataScopeExaminer cachedDataScopeExaminer, CachedDataPermissionPolicyResolver cachedDataPermissionPolicyResolver) {
            return new IsolationInsertProcessor(cachedDataScopeExaminer, cachedDataPermissionPolicyResolver, 10000);
        }

        /**
         * MyBatis 配置定制器（替换 mapperRegistry）。
         *
         * @return 配置定制器
         */
        @Bean
        @ConditionalOnMissingBean(MybatisMapperCustomizer.class)
        public ConfigurationCustomizer mybatisMapperCustomizer() {
            return new MybatisMapperCustomizer();
        }
    }

    /**
     * 注册 Executor 组合拦截器，并按顺序装配执行器阶段处理器。
     * <p>
     * 固定包含分页处理器；数据隔离处理器按可用性动态挂载，
     * 以适配启用/禁用数据隔离或用户覆盖默认 Bean 的场景。
     * </p>
     *
     * @param paginationQueryProcessor         分页处理器
     * @param isolationInsertProcessorProvider 插入隔离处理器提供者
     * @param isolationQueryProcessorProvider  查询隔离处理器提供者
     * @return Executor 组合拦截器
     */
    @Bean
    @ConditionalOnMissingBean(ExecutorCompositeInterceptor.class)
    public ExecutorCompositeInterceptor executorCompositeInterceptor(
        PaginationQueryProcessor paginationQueryProcessor,
        ObjectProvider<IsolationInsertProcessor> isolationInsertProcessorProvider,
        ObjectProvider<IsolationQueryProcessor> isolationQueryProcessorProvider) {

        ExecutorCompositeInterceptor interceptor = new ExecutorCompositeInterceptor();
        interceptor.addPluginProcessor(paginationQueryProcessor);
        isolationInsertProcessorProvider.ifAvailable(interceptor::addPluginProcessor);
        isolationQueryProcessorProvider.ifAvailable(interceptor::addPluginProcessor);
        return interceptor;
    }

    /**
     * 默认客户端生效条件定义。
     * <p>
     * 仅当容器中不存在用户自定义 {@link DataScopeExaminer} 时，
     * 才允许自动配置装配默认客户端与默认校验器链，避免覆盖业务侧实现。
     * </p>
     */
    static class OnDefaultClientCondition extends AllNestedConditions {
        /**
         * 条件构造器。
         * <p>在 Bean 注册阶段评估条件。</p>
         */
        OnDefaultClientCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        /**
         * 用户未提供自定义数据范围校验器的条件。
         */
        @ConditionalOnMissingBean(DataScopeExaminer.class)
        static class NoUserExaminer {

        }
    }
}
