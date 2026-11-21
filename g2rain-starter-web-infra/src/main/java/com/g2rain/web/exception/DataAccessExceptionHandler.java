package com.g2rain.web.exception;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.ExceptionProcessor;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.model.Result;
import com.g2rain.common.web.PrincipalContextHolder;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 数据库约束异常处理：按 Spring 统一后的异常类型返回可读的错误码，与具体数据库无关。
 *
 * <p>Spring 已把各数据库的错误码翻译为统一类型：唯一键冲突为 {@link DuplicateKeyException}，
 * 非空、长度、外键等其余约束为 {@link DataIntegrityViolationException}。数据库原始报错只写日志，不返回调用方。</p>
 *
 * <p>优先于 {@link GlobalExceptionHandler}，否则会被其 {@code Exception} 兜底先接住。</p>
 *
 * @author alpha
 * @since 2026/11/21
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "g2rain.web", name = {"enabled", "global-exception-handler-enabled"}, havingValue = "true", matchIfMissing = true)
public class DataAccessExceptionHandler {

    /**
     * 异常处理器，用于将 {@link BusinessException} 转换为 {@link Result} 响应。
     */
    private final ExceptionProcessor exceptionProcessor;

    /**
     * 构造函数
     *
     * @param exceptionProcessor 注入的异常处理器，用于统一转换异常
     */
    public DataAccessExceptionHandler(ExceptionProcessor exceptionProcessor) {
        this.exceptionProcessor = exceptionProcessor;
    }

    /**
     * 唯一键冲突：数据已存在。
     *
     * @param rsp 响应
     * @param ex  唯一键冲突异常
     * @return {@link SystemErrorCode#DATA_EXISTS} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result<Void>> handleDuplicateKey(HttpServletResponse rsp, DuplicateKeyException ex) {
        log.error("唯一键冲突异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return respond(rsp, SystemErrorCode.DATA_EXISTS);
    }

    /**
     * 其余约束违反：非空、长度、外键等。
     *
     * @param rsp 响应
     * @param ex  约束违反异常
     * @return {@link SystemErrorCode#DATA_CONSTRAINT_VIOLATED} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleIntegrityViolation(HttpServletResponse rsp,
                                                                 DataIntegrityViolationException ex) {
        log.error("数据约束异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return respond(rsp, SystemErrorCode.DATA_CONSTRAINT_VIOLATED);
    }

    /**
     * 按错误码组装响应。
     *
     * @param rsp       响应
     * @param errorCode 错误码
     * @return 统一响应
     */
    private ResponseEntity<Result<Void>> respond(HttpServletResponse rsp, SystemErrorCode errorCode) {
        Result<Void> result = exceptionProcessor.process(new BusinessException(errorCode),
            PrincipalContextHolder.getAcceptLanguage());
        return GlobalExceptionHandler.respond(rsp, result);
    }
}
