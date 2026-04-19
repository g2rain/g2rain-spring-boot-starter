package com.g2rain.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("HTTP响应包装器测试")
class HttpResponseWrapperTest {

    private MockHttpServletResponse mockResponse;
    private HttpResponseWrapper responseWrapper;

    @BeforeEach
    void setUp() {
        mockResponse = new MockHttpServletResponse();
        responseWrapper = new HttpResponseWrapper(mockResponse);
    }

    @Test
    @DisplayName("测试获取输出流")
    void testGetOutputStream() throws IOException {
        var outputStream = responseWrapper.getOutputStream();
        assertNotNull(outputStream);

        // 写入数据
        String testData = "test data";
        outputStream.write(testData.getBytes());
        outputStream.flush();

        // 验证数据被写入到包装器中
        assertEquals(testData, responseWrapper.getBody());
    }

    @Test
    @DisplayName("测试获取写入器")
    void testGetWriter() throws IOException {
        PrintWriter writer = responseWrapper.getWriter();
        assertNotNull(writer);

        // 写入数据
        String testData = "test writer data";
        writer.write(testData);
        writer.flush();

        // 验证数据被写入到包装器中
        assertEquals(testData, responseWrapper.getBody());
    }

    @Test
    @DisplayName("测试获取响应体")
    void testGetBody() throws IOException {
        PrintWriter writer = responseWrapper.getWriter();
        String testData = "response body content";
        writer.write(testData);
        writer.flush();

        // 验证getBody返回正确的内容
        assertEquals(testData, responseWrapper.getBody());
    }

    @Test
    @DisplayName("测试获取空内容的响应体")
    void testGetBodyWithEmptyContent() {
        // 验证空内容情况
        assertEquals("", responseWrapper.getBody());
    }
}
