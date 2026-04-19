package com.g2rain.web.filters;

import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.web.HttpRequestWrapper;
import com.g2rain.web.HttpResponseWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("HTTP包装器过滤器测试")
public class HttpWrapperFilterTest {

    private HttpWrapperFilter httpWrapperFilter;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockFilterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        httpWrapperFilter = new HttpWrapperFilter();
    }

    @Test
    @DisplayName("测试过滤器处理有效的请求和响应")
    void testDoFilterWithValidRequestResponse() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        httpWrapperFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // 验证chain.doFilter被调用
        verify(mockFilterChain).doFilter(any(HttpRequestWrapper.class), any(HttpResponseWrapper.class));
    }

    @Test
    @DisplayName("测试过滤器处理非HTTP Servlet对象时抛出异常")
    void testDoFilterWithNonHttpServletObjects() {
        jakarta.servlet.ServletRequest request = mock(jakarta.servlet.ServletRequest.class);
        jakarta.servlet.ServletResponse response = mock(jakarta.servlet.ServletResponse.class);

        // OncePerRequestFilter 仅支持 HTTP 请求，非 HTTP 会抛出 ServletException
        assertThrows(ServletException.class, () ->
                httpWrapperFilter.doFilter(request, response, mockFilterChain));
    }

    @Test
    @DisplayName("测试初始化带编码参数")
    void testInitWithEncodingParameter() {
        var filterConfig = mock(jakarta.servlet.FilterConfig.class);
        when(filterConfig.getInitParameter("encode")).thenReturn("UTF-8");
        when(filterConfig.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());

        assertDoesNotThrow(() -> httpWrapperFilter.init(filterConfig));
    }

    @Test
    @DisplayName("测试初始化不带编码参数")
    void testInitWithoutEncodingParameter() {
        var filterConfig = mock(jakarta.servlet.FilterConfig.class);
        when(filterConfig.getInitParameter("encode")).thenReturn(null);
        when(filterConfig.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());

        assertDoesNotThrow(() -> httpWrapperFilter.init(filterConfig));
    }
}
