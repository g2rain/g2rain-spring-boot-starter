package com.g2rain.web.interceptors;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.json.JsonCodecBuilder;
import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.web.HttpRequestWrapper;
import com.g2rain.web.interceptors.annotations.IdentityInject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("身份参数注入器测试")
public class IdentityParamInjectorTest {

    private IdentityParamInjector identityParamInjector;

    @BeforeEach
    void setUp() {
        identityParamInjector = new IdentityParamInjector();
    }

    @Test
    @DisplayName("测试后端访问预处理")
    void testPreHandleWithBackendAccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 设置后端访问标志
        PrincipalContextHolder.setBackEnd(true);

        Object handler = Mockito.mock(HandlerMethod.class);

        boolean result = identityParamInjector.preHandle(request, response, handler);

        // 后端访问应该总是允许通过
        assertTrue(result);
    }

    @Test
    @DisplayName("测试非处理器方法的预处理")
    void testPreHandleWithNonHandlerMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object handler = new Object(); // 非HandlerMethod对象

        boolean result = identityParamInjector.preHandle(request, response, handler);

        // 非HandlerMethod应该总是允许通过
        assertTrue(result);
    }

    @Test
    @DisplayName("测试管理员公司预处理")
    void testPreHandleWithAdminCompany() throws Exception {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                PrincipalContextHolder.setAdminCompany(true);

                TestController controller = new TestController();
                Method method = controller.getClass().getMethod("injectMethod");
                HandlerMethod handler = new HandlerMethod(controller, method);

                boolean result = identityParamInjector.preHandle(request, response, handler);

                assertTrue(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("测试无身份注入注解的预处理")
    void testPreHandleWithoutIdentityInjectAnnotation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("methodWithoutAnnotation");
        HandlerMethod handler = new HandlerMethod(controller, method);

        boolean result = identityParamInjector.preHandle(request, response, handler);

        // 没有@IdentityInject注解应该允许通过
        assertTrue(result);
    }

    @Test
    @DisplayName("测试带HTTP请求包装器的预处理")
    void testPreHandleWithHttpRequestWrapper() throws Exception {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                MockHttpServletRequest mockRequest = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                PrincipalContextHolder.setUserId(123L);
                PrincipalContextHolder.setOrganId(456L);
                PrincipalContextHolder.setPassportId(789L);
                PrincipalContextHolder.setApplicationId(1L);

                var jsonCodec = JsonCodecBuilder.builder().build();
                HttpRequestWrapper request = new HttpRequestWrapper(mockRequest, jsonCodec);

                TestController controller = new TestController();
                Method method = controller.getClass().getMethod("injectMethod");
                HandlerMethod handler = new HandlerMethod(controller, method);

                boolean result = identityParamInjector.preHandle(request, response, handler);

                assertTrue(result);

                // 注入值为 Long.toString()，且 HttpRequestWrapper 非 JSON 时写入 parameterMap，getParameter 可读到
                assertNotNull(request.getParameter("userId"), "userId 应已注入");
                assertEquals("123", request.getParameter("userId"));
                assertEquals("456", request.getParameter("organId"));
                assertEquals("789", request.getParameter("passportId"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("测试无HTTP请求包装器的预处理")
    void testPreHandleWithoutHttpRequestWrapper() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("injectMethod");
        HandlerMethod handler = new HandlerMethod(controller, method);

        // 应该抛出业务异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            identityParamInjector.preHandle(request, response, handler);
        });

        // 验证是系统内部错误
        assertNotNull(exception.getErrorCode());
    }

    // 测试用的控制器类
    static class TestController {
        @IdentityInject(
            userIdRequire = true,
            organIdRequire = true,
            passportIdRequire = true
        )
        public void injectMethod() {
            // 需要注入身份参数的方法
        }

        public void methodWithoutAnnotation() {
            // 没有注解的方法
        }
    }
}
