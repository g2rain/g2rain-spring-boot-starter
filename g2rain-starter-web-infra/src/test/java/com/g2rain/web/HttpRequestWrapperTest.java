package com.g2rain.web;

import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.json.JsonCodecBuilder;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("HTTP请求包装器测试")
class HttpRequestWrapperTest {

    private JsonCodec jsonCodec;

    @BeforeEach
    void setUp() {
        jsonCodec = JsonCodecBuilder.builder().build();
    }

    @Test
    @DisplayName("测试为表单请求添加参数")
    void testAddParameterToFormRequest() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setContentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        mockRequest.setParameter("existingParam", "existingValue");

        HttpRequestWrapper wrapper = new HttpRequestWrapper(mockRequest, jsonCodec);

        // 测试添加新参数
        wrapper.addParameter("newParam", "newValue");

        // 验证参数已添加
        assertEquals("existingValue", wrapper.getParameter("existingParam"));
        assertEquals("newValue", wrapper.getParameter("newParam"));

        // 验证参数映射包含新参数
        Map<String, String[]> parameterMap = wrapper.getParameterMap();
        assertTrue(parameterMap.containsKey("newParam"));
        assertArrayEquals(new String[]{"newValue"}, parameterMap.get("newParam"));
    }

    @Test
    @DisplayName("测试为JSON请求添加参数")
    void testAddParameterToJsonRequest() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mockRequest.setContent("{\"name\":\"test\"}".getBytes());
        mockRequest.setCharacterEncoding("UTF-8");

        HttpRequestWrapper wrapper = new HttpRequestWrapper(mockRequest, jsonCodec);

        // 测试添加新参数
        wrapper.addParameter("userId", "12345");

        // 获取body验证参数已添加到JSON中
        String body = wrapper.getBody();
        assertTrue(body.contains("\"userId\":\"12345\""));
        assertTrue(body.contains("\"name\":\"test\""));
    }

    @Test
    @DisplayName("测试获取请求头映射")
    void testGetHeaderMap() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", "Bearer token");
        mockRequest.addHeader("Content-Type", "application/json");

        HttpRequestWrapper wrapper = new HttpRequestWrapper(mockRequest, jsonCodec);

        Map<String, String> headerMap = wrapper.getHeaderMap();
        assertEquals("Bearer token", headerMap.get("Authorization"));
        assertEquals("application/json", headerMap.get("Content-Type"));
    }

    @Test
    @DisplayName("测试获取请求体")
    void testGetBody() throws IOException, ServletException {
        String jsonContent = "{\"name\":\"test\",\"value\":123}";
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mockRequest.setContent(jsonContent.getBytes());
        mockRequest.setCharacterEncoding("UTF-8");

        HttpRequestWrapper wrapper = new HttpRequestWrapper(mockRequest, jsonCodec);

        String body = wrapper.getBody();
        assertEquals(jsonContent, body);
    }

    @Test
    @DisplayName("测试添加空名称参数")
    void testAddParameterWithBlankName() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        HttpRequestWrapper wrapper = new HttpRequestWrapper(mockRequest, jsonCodec);

        // 添加空名称参数不应该抛出异常
        assertDoesNotThrow(() -> wrapper.addParameter("", "value"));
        assertDoesNotThrow(() -> wrapper.addParameter(null, "value"));
    }

    @Test
    @DisplayName("测试获取多个参数值")
    void testParameterValues() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setParameter("multiParam", new String[]{"value1", "value2"});

        HttpRequestWrapper wrapper = new HttpRequestWrapper(mockRequest, jsonCodec);

        String[] values = wrapper.getParameterValues("multiParam");
        assertArrayEquals(new String[]{"value1", "value2"}, values);
    }

    @Test
    @DisplayName("测试获取参数名称")
    void testParameterNames() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setParameter("param1", "value1");
        mockRequest.setParameter("param2", "value2");

        HttpRequestWrapper wrapper = new HttpRequestWrapper(mockRequest, jsonCodec);

        var names = wrapper.getParameterNames();
        assertTrue(Arrays.asList("param1", "param2").containsAll(
            Arrays.asList(names.nextElement(), names.nextElement())
        ));
    }
}
