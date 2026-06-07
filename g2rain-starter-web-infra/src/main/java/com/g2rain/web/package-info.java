/**
 * <h1>G2rain Web Starter</h1>
 *
 * <p>
 * 本 package 提供 Web 框架核心能力的自动化配置与请求链路增强能力，覆盖请求包装、上下文作用域管理、
 * 身份注入、日志记录以及全局异常处理等功能。
 * </p>
 *
 * <ol>
 *     <li><b>全局 HTTP 请求封装</b>
 *         <ul>
 *             <li>{@link com.g2rain.web.filters.HttpWrapperFilter} - 对请求和响应进行包装，提供请求体缓存、参数增强等能力。</li>
 *             <li>{@link com.g2rain.web.HttpRequestWrapper} / {@link com.g2rain.web.HttpResponseWrapper} - 支持多次读取请求体及响应内容修改。</li>
 *         </ul>
 *     </li>
 *
 *     <li><b>上下文作用域与身份管理</b>
 *         <ul>
 *             <li>{@link com.g2rain.web.filters.PrincipalContextScopeFilter} - 为每次请求创建并绑定 {@link com.g2rain.common.web.PrincipalContext} 作用域，确保线程隔离。</li>
 *             <li>{@link com.g2rain.web.filters.PrincipalContextFilter} - 从请求头提取身份信息并注入到 {@link com.g2rain.common.web.PrincipalContextHolder}。</li>
 *             <li>{@link com.g2rain.web.interceptors.IdentityParamInjector} - 根据 {@link com.g2rain.web.interceptors.annotations.IdentityInject} 注解自动注入身份参数。</li>
 *             <li>{@link com.g2rain.web.interceptors.LoginGuardInterceptor} - 根据 {@link com.g2rain.web.interceptors.annotations.LoginGuard} 注解执行登录校验。</li>
 *         </ul>
 *     </li>
 *
 *     <li><b>日志与异常处理</b>
 *         <ul>
 *             <li>{@link com.g2rain.web.filters.AccessLogFilter} - 统一记录请求访问日志。</li>
 *             <li>{@link com.g2rain.web.filters.GlobalExceptionFilter} - 捕获未处理异常并输出标准化错误响应。</li>
 *         </ul>
 *     </li>
 *
 *     <li><b>自动化配置</b>
 *         <ul>
 *             <li>{@link com.g2rain.web.autoconfigure.WebAutoConfiguration} - 启用并按需装配所有 Web 组件。</li>
 *         </ul>
 *     </li>
 * </ol>
 *
 * <h2>使用示例</h2>
 *
 * <pre>{@code
 * g2rain:
 *   web:
 *     enabled: true
 *
 * @RestController
 * public class DemoController {
 *
 *     @LoginGuard(require = true, anonymous = false)
 *     @IdentityInject(userIdPropertyName = "userId")
 *     @GetMapping("/api/demo")
 *     public Result<String> demo(@RequestParam String param) {
 *         return Result.success("Hello World");
 *     }
 * }
 * }</pre>
 *
 * <h2>功能效果</h2>
 * <ul>
 *     <li>请求级 PrincipalContext 作用域自动创建与释放，避免线程污染</li>
 *     <li>身份信息自动注入，降低 Controller 层样板代码</li>
 *     <li>统一请求与响应封装，支持请求体重复读取</li>
 *     <li>全局异常标准化输出，提升接口一致性</li>
 *     <li>统一访问日志记录，增强链路可观测性</li>
 * </ul>
 *
 * <h2>注意事项</h2>
 * <ul>
 *     <li>需开启 {@code g2rain.web.enabled} 才会加载 Starter 功能</li>
 *     <li>身份相关注解仅在 Spring MVC Controller 方法中生效</li>
 * </ul>
 *
 * @author alpha
 * @since 2025/10/5
 */
package com.g2rain.web;
