package com.g2rain.web;


import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * HttpResponseWrapper 对 HttpServletResponse 进行包装，实现双流输出。
 * <p>
 * 功能说明：
 * <ul>
 *     <li>主流写入原始响应，立即推送客户端</li>
 *     <li>分支流写入内存缓存，用于日志、调试或异常分析</li>
 *     <li>支持 PrintWriter 和 ServletOutputStream 的双写</li>
 * </ul>
 * <p>
 * 适用场景：
 * <ul>
 *     <li>需要获取响应 body 内容</li>
 *     <li>异常路径立即返回 JSON</li>
 *     <li>日志记录响应内容</li>
 * </ul>
 *
 * @author alpha
 * @since 2025/10/5
 */
public class HttpResponseWrapper extends HttpServletResponseWrapper {

    /**
     * 内存缓存响应字节流，用于存储写入响应的内容
     */
    private final ByteArrayOutputStream bos;

    /**
     * 分支 PrintWriter，写入内存缓存 bos，用于日志或获取响应 body
     */
    private final PrintWriter branchWriter;

    /**
     * 构造方法，初始化包装响应对象
     *
     * @param response 原始 HttpServletResponse
     */
    public HttpResponseWrapper(HttpServletResponse response) {
        super(response);
        this.bos = new ByteArrayOutputStream();
        this.branchWriter = new PrintWriter(new OutputStreamWriter(
            bos, StandardCharsets.UTF_8
        ), true);
    }

    /**
     * 获取包装的 PrintWriter。
     * <p>
     * 双写逻辑：
     * <ol>
     *     <li>写入原始 response 的 PrintWriter（立即推送客户端）</li>
     *     <li>写入 branchWriter（内存缓存，用于日志或异常分析）</li>
     * </ol>
     *
     * @return 包装后的 PrintWriter
     * @throws IOException 如果获取原始 PrintWriter 失败
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriterWrapper(super.getWriter(), branchWriter);
    }

    /**
     * 获取原始 ServletOutputStream。
     *
     * @return 原始 HttpServletResponse 的 ServletOutputStream
     * @throws IOException 如果获取原始输出流失败
     */
    private ServletOutputStream getSuperOutputStream() throws IOException {
        return super.getOutputStream();
    }

    /**
     * 获取包装的 ServletOutputStream。
     * <p>
     * 双写逻辑：
     * <ol>
     *     <li>写入内存缓存 bos</li>
     *     <li>写入原始 response 输出流（立即推送客户端）</li>
     * </ol>
     *
     * @return 包装后的 ServletOutputStream
     */
    @Override
    public ServletOutputStream getOutputStream() {
        return new ServletOutputStreamWrapper(this);
    }

    /**
     * 获取内存缓存中的响应 body 字符串。
     * <p>
     * 会 flush branchWriter 以确保缓存完整，然后按当前编码转换为字符串。
     *
     * @return 响应 body 字符串，如果编码不支持返回 null
     */
    public String getBody() {
        branchWriter.flush();
        return bos.toString(StandardCharsets.UTF_8);
    }

    /**
     * ServletOutputStream 包装类。
     * <p>
     * 每次 write 都写入：
     * <ol>
     *     <li>内存缓存 bos</li>
     *     <li>原始 ServletOutputStream（立即推送客户端）</li>
     * </ol>
     */
    private static class ServletOutputStreamWrapper extends ServletOutputStream {

        /**
         * 被包装的 HttpResponseWrapper
         */
        private final HttpResponseWrapper response;

        /**
         * 构造方法
         *
         * @param response 被包装的 HttpResponseWrapper
         */
        ServletOutputStreamWrapper(HttpResponseWrapper response) {
            this.response = response;
        }

        /**
         * 写单个字节。
         *
         * @param b 写入的字节
         * @throws IOException 如果写入失败
         */
        @Override
        public void write(int b) throws IOException {
            response.bos.write(b);
            response.getSuperOutputStream().write(b);
        }

        /**
         * 标识输出流是否准备好写入数据。
         * <p>
         * 该方法用于 Servlet 异步/非阻塞输出场景，容器会在调用 write 之前
         * 检查该方法，如果返回 false，则意味着输出流暂时不可写，
         * 需要等待 {@link WriteListener} 通知再写入。
         * <p>
         * 在当前包装中，我们始终返回 true，因为我们不使用异步写，
         * 所有写操作都是同步阻塞立即写入。
         *
         * @return true 表示输出流随时可写
         */
        @Override
        public boolean isReady() {
            return true;
        }

        /**
         * 设置异步写监听器，用于 Servlet 3.1+ 非阻塞输出。
         * <p>
         * 在非阻塞模式下，容器会在输出流可写时调用监听器的回调方法
         * {@link WriteListener#onWritePossible()} 或 {@link WriteListener#onError(Throwable)}。
         * <p>
         * 当前包装类不支持异步写，因此此方法为空实现，不做任何处理。
         * 所有写操作都是同步执行，立即写入原始响应流和缓存流。
         *
         * @param writeListener 异步写监听器，非阻塞输出模式下由容器调用
         */
        @Override
        public void setWriteListener(WriteListener writeListener) {
            // 不处理异步写
        }
    }

    /**
     * PrintWriter 包装类。
     * <p>
     * 每次 write 都写入：
     * <ol>
     *     <li>原始 response PrintWriter（立即推送客户端）</li>
     *     <li>branchWriter（内存缓存，用于日志或异常分析）</li>
     * </ol>
     */
    private static class PrintWriterWrapper extends PrintWriter {

        /**
         * 内存缓存分支流
         */
        private final PrintWriter branch;

        /**
         * 构造方法
         *
         * @param main   原始 response PrintWriter
         * @param branch 内存缓存 PrintWriter
         */
        public PrintWriterWrapper(PrintWriter main, PrintWriter branch) {
            super(main, true);
            this.branch = branch;
        }

        /**
         * 写字符数组。
         *
         * @param buf 字符数组
         * @param off 起始偏移
         * @param len 写入长度
         */
        @Override
        public void write(@Nonnull char[] buf, int off, int len) {
            super.write(buf, off, len);
            super.flush();
            branch.write(buf, off, len);
            branch.flush();
        }

        /**
         * 写字符串。
         *
         * @param s   字符串
         * @param off 起始偏移
         * @param len 写入长度
         */
        @Override
        public void write(@Nonnull String s, int off, int len) {
            super.write(s, off, len);
            super.flush();
            branch.write(s, off, len);
            branch.flush();
        }

        /**
         * 写单个字符。
         *
         * @param c 字符
         */
        @Override
        public void write(int c) {
            super.write(c);
            super.flush();
            branch.write(c);
            branch.flush();
        }

        /**
         * flush 输出流。
         * <p>
         * 同时 flush 原始流和分支流，确保内容都写入。
         */
        @Override
        public void flush() {
            super.flush();
            branch.flush();
        }
    }
}
