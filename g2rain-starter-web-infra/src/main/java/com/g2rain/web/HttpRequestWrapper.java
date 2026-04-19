package com.g2rain.web;


import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.json.JsonCodec;
import com.g2rain.common.utils.Collections;
import com.g2rain.common.utils.Constants;
import com.g2rain.common.utils.Strings;
import jakarta.annotation.Nonnull;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.Part;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>{@code HttpRequestWrapper} 是 {@link HttpServletRequestWrapper} 的自定义实现类。</p>
 * <p>
 * 主要功能是：
 * <ul>
 *     <li>支持读取和缓存请求体内容，避免多次读取 InputStream 导致流关闭的问题。</li>
 *     <li>支持将 multipart/form-data 与 application/json 请求参数统一解析。</li>
 *     <li>允许动态添加请求参数（包括 JSON 请求体内的参数）。</li>
 *     <li>提供对请求体和请求头的便捷访问方法。</li>
 * </ul>
 * </p>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * HttpRequestWrapper wrapper = new HttpRequestWrapper(request, jsonCodec);
 * String param = wrapper.getParameter("name");
 * String body = wrapper.getBody();
 * wrapper.addParameter("newKey", "newValue");
 * byte[] bodyBytes = wrapper.asBytes();
 * Map<String, String> headers = wrapper.getHeaderMap();
 * }</pre>
 *
 * <p>适用场景包括但不限于：
 * <ul>
 *     <li>需要修改或添加请求参数的过滤器</li>
 *     <li>需要多次读取请求体的场景</li>
 *     <li>统一处理 JSON 请求体的参数扩展</li>
 * </ul>
 * </p>
 *
 * @author alpha
 * @since 2025/10/5
 */
public class HttpRequestWrapper extends HttpServletRequestWrapper {

    /**
     * 请求参数映射，包含原始请求参数及新增参数
     */
    private final Map<String, String[]> parameterMap;

    /**
     * JSON 编解码工具
     */
    private final JsonCodec jsonCodec;

    /**
     * 额外的 JSON 请求体参数
     */
    private Map<String, String> extraJsonBody;

    /**
     * 缓存的请求体字节数组
     */
    private byte[] body = Constants.EMPTY_BYTE;

    /**
     * 标记请求是否为 JSON 类型
     */
    private boolean isJson = false;

    /**
     * 构造 HttpRequestWrapper
     *
     * @param request   原始 HttpServletRequest
     * @param jsonCodec JSON 编解码工具
     * @throws IOException      IO 异常
     * @throws ServletException Servlet 异常
     */
    public HttpRequestWrapper(HttpServletRequest request, JsonCodec jsonCodec) throws IOException, ServletException {
        super(request);
        this.jsonCodec = jsonCodec;
        // 涵盖了queryString和APPLICATION_FORM_URLENCODED_VALUE场景
        this.parameterMap = new HashMap<>(super.getParameterMap());

        String contentType = request.getContentType();
        if (Strings.isBlank(contentType)) {
            return;
        }

        // MULTIPART_FORM_DATA在没进行解析Part的情况下, 可能不存在, 需单独提取
        MediaType mediaType = MediaType.parseMediaType(contentType);
        if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType)) {
            parseMultipartParameters(request);
            return;
        }

        // APPLICATION_JSON 单独提取
        if (MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            isJson = true;
            this.body = toByteArray(request.getInputStream());
        }
    }

    /**
     * 解析 multipart/form-data 请求的参数
     *
     * @param request 原始请求
     * @throws ServletException Servlet 异常
     * @throws IOException      IO 异常
     */
    private void parseMultipartParameters(HttpServletRequest request) throws ServletException, IOException {
        for (Part part : request.getParts()) {
            if (Strings.isNotBlank(part.getSubmittedFileName())) {
                continue;
            }

            byte[] bytes = part.getInputStream().readAllBytes();
            String value = new String(bytes, StandardCharsets.UTF_8);
            addParameter(part.getName(), value);
        }
    }

    /**
     * 将 InputStream 转换为字节数组
     *
     * @param input 输入流
     * @return 字节数组
     * @throws IOException IO 异常
     */
    private byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        mergeJsonBody();

        if (Collections.isEmpty(this.body)) {
            return super.getInputStream();
        }

        final ByteArrayInputStream input = new ByteArrayInputStream(this.body);

        return new ServletInputStream() {
            @Override
            public int available() {
                return input.available();
            }

            @Override
            public int read() {
                return input.read();
            }

            @Override
            public int read(@Nonnull byte[] b, int off, int len) {
                return input.read(b, off, len);
            }

            @Override
            public boolean isFinished() {
                return input.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                throw new BusinessException(SystemErrorCode.SYSTEM_INTERNAL_ERROR);
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameterMap.get(name);
        return Collections.isNotEmpty(values) ? values[0] : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return java.util.Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return java.util.Collections.unmodifiableMap(parameterMap);
    }

    /**
     * 添加或修改请求参数
     *
     * @param name  参数名
     * @param value 参数值
     */
    public void addParameter(String name, String value) {
        if (Strings.isBlank(name)) {
            return;
        }

        if (isJson) {
            if (Objects.isNull(extraJsonBody)) {
                extraJsonBody = new HashMap<>();
            }
            extraJsonBody.put(name, value);
            return;
        }

        String[] oldValues = parameterMap.get(name);
        if (Collections.isEmpty(oldValues)) {
            parameterMap.put(name, new String[]{value});
            return;
        }

        if (!Arrays.asList(oldValues).contains(value)) {
            String[] newValues = Arrays.copyOf(oldValues, oldValues.length + 1);
            newValues[newValues.length - 1] = value;
            parameterMap.put(name, newValues);
        }
    }

    /**
     * 获取请求头映射
     *
     * @return 请求头的键值映射
     */
    public Map<String, String> getHeaderMap() {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = this.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, this.getHeader(name));
        }
        return headers;
    }

    /**
     * 获取请求体字符串
     *
     * @return 请求体字符串
     */
    public String getBody() {
        mergeJsonBody();
        return new String(this.body, StandardCharsets.UTF_8);
    }

    /**
     * 获取请求体字节数组
     *
     * @return 请求体字节数组
     */
    public byte[] asBytes() {
        mergeJsonBody();
        return this.body;
    }

    /**
     * 合并额外的 JSON 请求参数到原始请求体中
     */
    private void mergeJsonBody() {
        if (!isJson || Collections.isEmpty(extraJsonBody)) {
            return;
        }

        JsonNode originalNode = jsonCodec.byte2node(this.body);
        if (Objects.isNull(originalNode) || !originalNode.isObject()) {
            return;
        }

        ObjectNode objectNode = (ObjectNode) originalNode;
        extraJsonBody.forEach(objectNode::putPOJO);
        extraJsonBody = null;
        this.body = jsonCodec.obj2byte(objectNode);
    }
}
