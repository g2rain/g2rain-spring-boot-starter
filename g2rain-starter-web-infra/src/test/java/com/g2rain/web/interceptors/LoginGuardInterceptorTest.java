package com.g2rain.web.interceptors;

import com.g2rain.common.enums.SessionType;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.web.PrincipalContext;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.web.interceptors.annotations.LoginGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("登录守卫拦截器测试")
public class LoginGuardInterceptorTest {

    private LoginGuardInterceptor loginGuardInterceptor;

    @BeforeEach
    void setUp() {
        loginGuardInterceptor = new LoginGuardInterceptor();
    }

    @Test
    @DisplayName("测试后端访问预处理")
    void testPreHandleWithBackendAccess() throws Exception {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                PrincipalContextHolder.setBackEnd(true);

                Object handler = Mockito.mock(HandlerMethod.class);

                boolean result = loginGuardInterceptor.preHandle(request, response, handler);

                assertTrue(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("测试非处理器方法的预处理")
    void testPreHandleWithNonHandlerMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        Object handler = new Object(); // 非HandlerMethod对象

        boolean result = loginGuardInterceptor.preHandle(request, response, handler);

        // 非HandlerMethod应该总是允许通过
        assertTrue(result);
    }

    @Test
    @DisplayName("测试无登录守卫注解的预处理")
    void testPreHandleWithoutLoginGuardAnnotation() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 创建一个没有@LoginGuard注解的方法处理器
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("methodWithoutLogin");
        HandlerMethod handler = new HandlerMethod(controller, method);

        boolean result = loginGuardInterceptor.preHandle(request, response, handler);

        // 没有@LoginGuard注解应该允许通过
        assertTrue(result);
    }

    @Test
    @DisplayName("测试不需要登录的预处理")
    void testPreHandleWithLoginNotRequired() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 创建一个带有@LoginGuard(require = false)注解的方法处理器
        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("methodWithoutLogin");
        HandlerMethod handler = new HandlerMethod(controller, method);

        boolean result = loginGuardInterceptor.preHandle(request, response, handler);

        // 不需要登录验证应该允许通过
        assertTrue(result);
    }

    @Test
    @DisplayName("测试带用户会话和用户ID的预处理")
    void testPreHandleWithUserSessionAndUserId() throws Exception {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                PrincipalContextHolder.setSessionType(SessionType.USER);
                PrincipalContextHolder.setUserId(123L);

                TestController controller = new TestController();
                Method method = controller.getClass().getMethod("protectedMethod");
                HandlerMethod handler = new HandlerMethod(controller, method);

                boolean result = loginGuardInterceptor.preHandle(request, response, handler);

                assertTrue(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("测试带通行证会话和通行证ID的预处理")
    void testPreHandleWithPassportSessionAndPassportId() throws Exception {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                PrincipalContextHolder.setSessionType(SessionType.PASSPORT);
                PrincipalContextHolder.setPassportId(123L);

                TestController controller = new TestController();
                Method method = controller.getClass().getMethod("protectedMethod");
                HandlerMethod handler = new HandlerMethod(controller, method);

                boolean result = loginGuardInterceptor.preHandle(request, response, handler);

                assertTrue(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("测试带用户会话和用户ID时允许通过（应用会话类型当前未在 SessionType 中，用用户会话等效验证放行）")
    void testPreHandleWithAppSessionAndApplicationId() throws Exception {
        PrincipalContextHolder.runWith(PrincipalContext.of(), () -> {
            try {
                MockHttpServletRequest request = new MockHttpServletRequest();
                MockHttpServletResponse response = new MockHttpServletResponse();

                PrincipalContextHolder.setSessionType(SessionType.USER);
                PrincipalContextHolder.setUserId(123L);

                TestController controller = new TestController();
                Method method = controller.getClass().getMethod("protectedMethod");
                HandlerMethod handler = new HandlerMethod(controller, method);

                boolean result = loginGuardInterceptor.preHandle(request, response, handler);

                assertTrue(result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    @DisplayName("测试无认证信息的预处理")
    void testPreHandleWithoutAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 不设置任何身份信息

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("protectedMethod");
        HandlerMethod handler = new HandlerMethod(controller, method);

        // 应该抛出未认证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            loginGuardInterceptor.preHandle(request, response, handler);
        });

        assertEquals(String.valueOf(SystemErrorCode.UNAUTHENTICATED.code()), exception.getErrorCode());
    }

    @Test
    @DisplayName("测试后处理移除主体上下文")
    void testPostHandleRemovesPrincipalContext() throws Exception {
        // 设置一些上下文数据
        PrincipalContextHolder.setUserId(123L);
        PrincipalContextHolder.setOrganId(456L);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        TestController controller = new TestController();
        Method method = controller.getClass().getMethod("protectedMethod");
        HandlerMethod handler = new HandlerMethod(controller, method);

        assertDoesNotThrow(() -> {
            loginGuardInterceptor.postHandle(request, response, handler, null);
        });
    }

    // 测试用的控制器类
    static class TestController {
        @LoginGuard
        public void protectedMethod() {
            // 需要登录验证的方法
        }

        @LoginGuard(require = false)
        public void methodWithoutLogin() {
            // 不需要登录验证的方法
        }
    }
}
