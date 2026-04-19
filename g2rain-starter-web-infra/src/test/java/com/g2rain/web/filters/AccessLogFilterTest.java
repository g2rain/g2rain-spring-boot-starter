package com.g2rain.web.filters;

import com.g2rain.common.json.JsonCodecBuilder;
import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.web.HttpRequestWrapper;
import com.g2rain.web.HttpResponseWrapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@DisplayName("访问日志过滤器测试")
public class AccessLogFilterTest {

    private AccessLogFilter accessLogFilter;

    @Mock
    private FilterChain mockFilterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        accessLogFilter = new AccessLogFilter();
    }

    @Test
    @DisplayName("测试过滤器处理JSON响应")
    void testDoFilterWithJsonResponse() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("POST");
        mockRequest.setRequestURI("/api/test");
        mockRequest.setServletPath("/api/test");
        mockRequest.setCharacterEncoding("UTF-8");

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        mockResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mockResponse.setCharacterEncoding("UTF-8");

        // 创建包装器
        var jsonCodec = JsonCodecBuilder.builder().build();
        HttpRequestWrapper requestWrapper = new HttpRequestWrapper(mockRequest, jsonCodec);
        HttpResponseWrapper responseWrapper = new HttpResponseWrapper(mockResponse);

        assertDoesNotThrow(() -> {
            accessLogFilter.doFilter(requestWrapper, responseWrapper, mockFilterChain);
        });

        // 验证过滤器链被调用
        verify(mockFilterChain).doFilter(requestWrapper, responseWrapper);
    }

    @Test
    @DisplayName("测试过滤器生成请求ID")
    void testDoFilterGeneratesRequestId() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setMethod("GET");
        mockRequest.setRequestURI("/api/test");
        mockRequest.setServletPath("/api/test");

        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        mockResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

        assertDoesNotThrow(() -> {
            accessLogFilter.doFilter(mockRequest, mockResponse, mockFilterChain);
        });

        // 验证过滤器链被调用
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    @DisplayName("测试过滤器处理已存在的请求ID")
    void testDoFilterWithExistingRequestId() {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                PrincipalContextHolder.setRequestId("existing-request-id");

                MockHttpServletRequest mockRequest = new MockHttpServletRequest();
                mockRequest.setMethod("GET");
                mockRequest.setRequestURI("/api/test");
                mockRequest.setServletPath("/api/test");

                MockHttpServletResponse mockResponse = new MockHttpServletResponse();
                mockResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

                assertEquals("existing-request-id", PrincipalContextHolder.getRequestId());

                accessLogFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

                verify(mockFilterChain).doFilter(mockRequest, mockResponse);
                assertEquals("existing-request-id", PrincipalContextHolder.getRequestId());
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
