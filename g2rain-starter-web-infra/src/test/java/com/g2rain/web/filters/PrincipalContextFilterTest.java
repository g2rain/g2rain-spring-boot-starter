package com.g2rain.web.filters;

import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

@DisplayName("主体上下文过滤器测试")
public class PrincipalContextFilterTest {

    private PrincipalContextFilter principalContextFilter;

    @Mock
    private FilterChain mockFilterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        principalContextFilter = new PrincipalContextFilter();
    }

    @Test
    @DisplayName("测试过滤器设置主体上下文")
    void testDoFilterSetsPrincipalContext() {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                MockHttpServletRequest mockRequest = new MockHttpServletRequest();
                MockHttpServletResponse mockResponse = new MockHttpServletResponse();

                mockRequest.addHeader("X-USER-ID", "user123");
                mockRequest.addHeader("X-ORGAN-ID", "organ456");
                mockRequest.addHeader("X-REQUEST-ID", "request789");

                principalContextFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

                verify(mockFilterChain).doFilter(mockRequest, mockResponse);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("测试过滤器移除主体上下文")
    void testDoFilterRemovesPrincipalContext() {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                MockHttpServletRequest mockRequest = new MockHttpServletRequest();
                MockHttpServletResponse mockResponse = new MockHttpServletResponse();

                principalContextFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

                verify(mockFilterChain).doFilter(mockRequest, mockResponse);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
