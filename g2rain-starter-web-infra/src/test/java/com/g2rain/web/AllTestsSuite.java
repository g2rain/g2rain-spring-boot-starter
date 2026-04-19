package com.g2rain.web;

import com.g2rain.web.autoconfigure.WebAutoConfigurationTest;
import com.g2rain.web.filters.AccessLogFilterTest;
import com.g2rain.web.filters.HttpWrapperFilterTest;
import com.g2rain.web.filters.PrincipalContextFilterTest;
import com.g2rain.web.interceptors.IdentityParamInjectorTest;
import com.g2rain.web.interceptors.LoginGuardInterceptorTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Web基础设施测试套件")
@SelectClasses({
    // 过滤器测试
    HttpWrapperFilterTest.class,
    PrincipalContextFilterTest.class,
    AccessLogFilterTest.class,

    // 拦截器测试
    LoginGuardInterceptorTest.class,
    IdentityParamInjectorTest.class,

    // 自动配置测试
    WebAutoConfigurationTest.class
})
public class AllTestsSuite {
}
