package com.g2rain.web.filters;


import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <p>{@code PrincipalContextScopeFilter} 是一个 Spring {@link OncePerRequestFilter} 实现类，
 * 用于在一次 HTTP 请求范围内创建并绑定 {@link PrincipalContext} 作用域。</p>
 *
 * <p>主要功能：</p>
 * <ul>
 *     <li>在请求进入时创建新的 {@link PrincipalContext} 实例。</li>
 *     <li>通过 {@link PrincipalContextHolder} 将该实例绑定到当前线程。</li>
 *     <li>保证整个请求处理链路中共享同一个上下文实例。</li>
 *     <li>请求结束后自动解除绑定，防止线程复用导致的上下文污染。</li>
 * </ul>
 *
 * <p><b>设计说明：</b></p>
 * <ul>
 *     <li>该 Filter 仅负责作用域生命周期管理，不负责填充具体身份信息。</li>
 *     <li>通常应在 {@link PrincipalContextFilter} 之前执行，以确保上下文已建立。</li>
 *     <li>通过 {@code callWith} 统一处理绑定与释放逻辑，避免手动清理遗漏。</li>
 * </ul>
 *
 * <p>适用场景：适用于基于线程绑定模型的上下文传递机制，例如用户身份信息、租户信息、
 * 请求级扩展参数等。</p>
 *
 * @author alpha
 * @since 2026/2/27
 */
@Slf4j
public class PrincipalContextScopeFilter extends OncePerRequestFilter {

    /**
     * 在当前请求线程内创建并绑定 {@link PrincipalContext} 作用域。
     *
     * <p>执行流程：</p>
     * <ul>
     *     <li>创建新的 {@link PrincipalContext} 实例。</li>
     *     <li>通过 {@link PrincipalContextHolder#callWith} 绑定到当前线程。</li>
     *     <li>执行后续 Filter 链。</li>
     *     <li>请求完成后自动解除绑定，避免线程复用导致的上下文污染。</li>
     * </ul>
     *
     * <p>异常处理策略：</p>
     * <ul>
     *     <li>{@link IOException} 与 {@link ServletException} 原样抛出。</li>
     *     <li>其他异常统一包装为 {@link ServletException}。</li>
     * </ul>
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param chain    过滤器链
     * @throws ServletException Servlet 规范异常
     * @throws IOException      IO 异常
     */
    @Override
    public void doFilterInternal(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain chain) throws ServletException, IOException {
        try {
            PrincipalContextHolder.callWith(PrincipalContext.of(), () -> {
                chain.doFilter(request, response);
                return null;
            });
        } catch (Exception e) {
            if (e instanceof IOException io) {
                throw io;
            }
            if (e instanceof ServletException se) {
                throw se;
            }
            throw new ServletException(e);
        }
    }
}
