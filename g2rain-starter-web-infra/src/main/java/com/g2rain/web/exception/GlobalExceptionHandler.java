package com.g2rain.web.exception;


import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.exception.ExceptionProcessor;
import com.g2rain.common.exception.FieldError;
import com.g2rain.common.exception.SystemErrorCode;
import com.g2rain.common.model.Result;
import com.g2rain.common.web.PrincipalContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>{@code GlobalExceptionHandler} 是全局异常处理类，用于统一捕获和处理业务异常及请求校验异常。</p>
 * <p>
 * 当 {@code g2rain.web.enabled=true}（默认开启）时生效。
 * 它统一将各种异常转换为 {@link Result} 响应对象，并返回 HTTP 200 状态码。
 * </p>
 *
 * <p>功能包括：</p>
 * <ul>
 *     <li>捕获 {@link BusinessException} 并返回统一错误响应</li>
 *     <li>处理 {@code @Valid} 和 {@code @Validated} 校验失败，将 FieldError 转换为可读提示</li>
 *     <li>处理 BindException（表单绑定失败）</li>
 *     <li>处理请求参数类型不匹配 {@link TypeMismatchException}</li>
 *     <li>处理请求方法不支持 {@link HttpRequestMethodNotSupportedException}</li>
 *     <li>处理请求 Content-Type 或 Accept 不支持异常</li>
 *     <li>处理 Header / Cookie / PathVariable 缺失异常 {@link ServletRequestBindingException}</li>
 *     <li>兜底处理 {@link Exception}</li>
 * </ul>
 *
 * <p><b>使用示例：</b></p>
 * <pre>{@code
 * // 当业务逻辑中抛出 BusinessException 时，该处理器会自动捕获并处理
 * throw new BusinessException(SystemErrorCode.SOME_ERROR, "业务错误描述");
 *
 * // 返回的 HTTP 响应为：
 * // HTTP 200 OK
 * // Body: {
 * //     "code": "...",
 * //     "message": "...",
 * //     "data": null
 * // }
 * }</pre>
 *
 * @author alpha
 * @since 2025/10/5
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnProperty(prefix = "g2rain.web", name = {"enabled", "global-exception-handler-enabled"}, havingValue = "true", matchIfMissing = true)
public class GlobalExceptionHandler {

    /**
     * 异常处理器，用于将 {@link BusinessException} 转换为 {@link Result} 响应。
     */
    private final ExceptionProcessor exceptionProcessor;

    /**
     * 构造函数
     *
     * @param exceptionProcessor 注入的异常处理器，用于统一转换异常
     */
    public GlobalExceptionHandler(ExceptionProcessor exceptionProcessor) {
        this.exceptionProcessor = exceptionProcessor;
    }

    /**
     * 处理业务异常 {@link BusinessException}。
     *
     * @param ex 捕获的业务异常
     * @return 包含错误码和消息的统一 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<@NonNull Result<Void>> handleException(BusinessException ex) {
        log.error("业务异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        Result<Void> result = exceptionProcessor.process(ex,
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * <p>处理请求体 DTO {@code @Valid} 校验失败异常 {@link MethodArgumentNotValidException}。</p>
     * <p>将每个字段错误转换为可读的 {@link FieldError}，并封装到 {@link Result} 中返回。</p>
     * <p>针对{@code @RequestBody} 和{@code @ModelAttribute} 有效</p>
     *
     * @param ex 捕获的校验异常
     * @return 包含字段错误信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<@NonNull Result<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("参数验证失败异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        List<org.springframework.validation.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<FieldError> subErrors = new ArrayList<>(fieldErrors.size());

        fieldErrors.forEach(fieldError -> {
            String field = fieldError.getField();
            if (ConstraintFormatter.buildBlankMessage(fieldError)) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_REQUIRED, field));
                return;
            }

            String paramRange = ConstraintFormatter.buildRangeMessage(fieldError);
            if (Objects.nonNull(paramRange)) {
                Object paramValue = fieldError.getRejectedValue();
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_RANGE,
                    paramValue, paramRange
                ));
                return;
            }

            String paramSize = ConstraintFormatter.buildSizeMessage(fieldError);
            if (Objects.nonNull(paramSize)) {
                Object paramValue = fieldError.getRejectedValue();
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_SIZE,
                    paramValue, paramSize
                ));
                return;
            }

            String paramDate = ConstraintFormatter.buildDateMessage(fieldError);
            if (Objects.nonNull(paramDate)) {
                Object paramValue = fieldError.getRejectedValue();
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_RANGE,
                    paramValue, paramDate
                ));
                return;
            }

            String paramPattern = ConstraintFormatter.buildPatternMessage(fieldError);
            if (Objects.nonNull(paramPattern)) {
                Object paramValue = fieldError.getRejectedValue();
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_INVALID_FORMAT,
                    paramValue, paramPattern
                ));
            }
        });

        Result<Void> result = exceptionProcessor.process(
            new BusinessException(SystemErrorCode.PARAM_INVALID, subErrors),
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 处理单参数 {@code @Validated} 校验失败异常 {@link ConstraintViolationException}。
     * 将 {@link ConstraintViolation} 转换为 {@link FieldError} 并返回。
     *
     * @param ex 捕获的约束违规异常
     * @return 包含字段错误信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<@NonNull Result<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("参数校验失败异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        List<FieldError> subErrors = new ArrayList<>(violations.size());

        violations.forEach(violation -> {
            String field = ConstraintFormatter.extractProperty(violation.getPropertyPath().toString());
            Object invalidValue = violation.getInvalidValue();
            String code = violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();

            // 构造一个虚拟 FieldError 以复用 ConstraintFormatter 逻辑
            org.springframework.validation.FieldError fakeFieldError = new org.springframework.validation.FieldError(
                violation.getRootBeanClass().getSimpleName(), field, invalidValue, false, new String[]{code}, null, violation.getMessage()
            );

            if (ConstraintFormatter.buildBlankMessage(fakeFieldError)) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_REQUIRED, field));
                return;
            }

            String paramRange = ConstraintFormatter.buildRangeMessage(fakeFieldError);
            if (Objects.nonNull(paramRange)) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_RANGE, invalidValue, paramRange));
                return;
            }

            String paramSize = ConstraintFormatter.buildSizeMessage(fakeFieldError);
            if (Objects.nonNull(paramSize)) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_SIZE, invalidValue, paramSize));
                return;
            }

            String paramDate = ConstraintFormatter.buildDateMessage(fakeFieldError);
            if (Objects.nonNull(paramDate)) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_RANGE, invalidValue, paramDate));
                return;
            }

            String paramPattern = ConstraintFormatter.buildPatternMessage(fakeFieldError);
            if (Objects.nonNull(paramPattern)) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_INVALID_FORMAT, invalidValue, paramPattern));
                return;
            }

            // 默认兜底
            subErrors.add(new FieldError(field, SystemErrorCode.PARAM_INVALID, invalidValue, violation.getMessage()));
        });

        Result<Void> result = exceptionProcessor.process(
            new BusinessException(SystemErrorCode.PARAM_INVALID, subErrors),
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 处理表单绑定异常 {@link BindException}。
     * 将字段绑定错误转换为 {@link FieldError} 并返回。
     *
     * @param ex 捕获的绑定异常
     * @return 包含字段错误信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<@NonNull Result<Void>> handleBindException(BindException ex) {
        log.error("参数绑定失败异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        List<org.springframework.validation.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        List<FieldError> subErrors = new ArrayList<>(fieldErrors.size());

        fieldErrors.forEach(fieldError -> {
            String field = fieldError.getField();
            Object value = fieldError.getRejectedValue();

            if (ConstraintFormatter.buildBlankMessage(fieldError)) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_REQUIRED, field));
                return;
            }

            String range = ConstraintFormatter.buildRangeMessage(fieldError);
            if (range != null) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_RANGE, value, range));
                return;
            }

            String size = ConstraintFormatter.buildSizeMessage(fieldError);
            if (size != null) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_SIZE, value, size));
                return;
            }

            String date = ConstraintFormatter.buildDateMessage(fieldError);
            if (date != null) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_EXCEEDS_RANGE, value, date));
                return;
            }

            String pattern = ConstraintFormatter.buildPatternMessage(fieldError);
            if (pattern != null) {
                subErrors.add(new FieldError(field, SystemErrorCode.PARAM_INVALID_FORMAT, value, pattern));
                return;
            }

            subErrors.add(new FieldError(field, SystemErrorCode.PARAM_INVALID, value, fieldError.getDefaultMessage()));
        });

        Result<Void> result = exceptionProcessor.process(
            new BusinessException(SystemErrorCode.PARAM_INVALID, subErrors),
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 处理请求参数类型不匹配异常 {@link TypeMismatchException}。
     *
     * @param ex 捕获的类型不匹配异常
     * @return 包含字段类型错误信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<@NonNull Result<Void>> handleTypeMismatchException(TypeMismatchException ex) {
        log.error("参数类型不匹配异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        String field = ex.getPropertyName();
        Object value = ex.getValue();

        Class<?> requiredTypeClazz = ex.getRequiredType();
        String requiredType = Objects.nonNull(requiredTypeClazz) ? requiredTypeClazz.getSimpleName() : "unknown";

        BusinessException be = new BusinessException(SystemErrorCode.PARAM_INVALID);
        be.addFieldError(new FieldError(field, SystemErrorCode.PARAM_TYPE_MISMATCH, value, requiredType));
        Result<Void> result = exceptionProcessor.process(be,
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 处理请求方法不支持异常 {@link HttpRequestMethodNotSupportedException} (405)。
     *
     * @param request 请求对象
     * @param ex      捕获的异常
     * @return 包含方法不支持信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @SuppressWarnings("ConstantConditions")
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<@NonNull Result<Void>> handleHttpRequestMethodNotSupportedException(HttpServletRequest request, HttpRequestMethodNotSupportedException ex) {
        log.error("不支持的HTTP方法异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        String uri = request.getRequestURI(); // 获取请求 URI
        String method = ex.getMethod();
        String[] supportedMethods = ex.getSupportedMethods();
        String supported = String.join(", ", Objects.nonNull(supportedMethods) ? supportedMethods : new String[]{});

        Result<Void> result = exceptionProcessor.process(
            new BusinessException(SystemErrorCode.METHOD_NOT_SUPPORTED, uri, method, supported),
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 处理请求 Content-Type 不支持异常 {@link HttpMediaTypeNotSupportedException} (415)。
     *
     * @param request 请求对象
     * @param ex      捕获的异常
     * @return 包含 Content-Type 不支持信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<@NonNull Result<Void>> handleHttpMediaTypeNotSupportedException(HttpServletRequest request, HttpMediaTypeNotSupportedException ex) {
        log.error("不支持的请求格式异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        String uri = request.getRequestURI();
        MediaType mediaType = ex.getContentType();
        String contentType = Objects.nonNull(mediaType) ? mediaType.toString() : "unknown";
        String supported = ex.getSupportedMediaTypes().stream().map(Object::toString).collect(Collectors.joining(", "));

        Result<Void> result = exceptionProcessor.process(
            new BusinessException(SystemErrorCode.MEDIA_TYPE_NOT_SUPPORTED, uri, contentType, supported),
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 处理响应 Accept 不支持异常 {@link HttpMediaTypeNotAcceptableException} (406)。
     *
     * @param request 请求对象
     * @param ex      捕获的异常
     * @return 包含 Accept 不支持信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<@NonNull Result<Void>> handleHttpMediaTypeNotAcceptableException(HttpServletRequest request, HttpMediaTypeNotAcceptableException ex) {
        log.error("客户端Accept类型不支持异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        String uri = request.getRequestURI();
        String supported = ex.getSupportedMediaTypes().stream().map(Object::toString).collect(Collectors.joining(", "));

        Result<Void> result = exceptionProcessor.process(
            new BusinessException(SystemErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE, uri, supported),
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 处理请求 Header、PathVariable、参数缺失异常 {@link ServletRequestBindingException}。
     *
     * @param request 请求对象
     * @param ex      捕获的异常
     * @return 包含缺失信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<@NonNull Result<Void>> handleServletRequestBindingException(HttpServletRequest request, ServletRequestBindingException ex) {
        log.error("请求参数绑定异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        String uri = request.getRequestURI();
        String type;
        String name;

        switch (ex) {
            case MissingRequestHeaderException headerEx -> {
                type = "Header";
                name = headerEx.getHeaderName();
            }
            case MissingPathVariableException pathEx -> {
                type = "PathVariable";
                name = pathEx.getVariableName();
            }
            case MissingServletRequestParameterException paramEx -> {
                type = "参数";
                name = paramEx.getParameterName();
            }
            default -> {
                type = "参数";
                name = "未知";
            }
        }

        Result<Void> result = exceptionProcessor.process(
            new BusinessException(SystemErrorCode.REQUEST_BINDING_ERROR, uri, type, name),
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    /**
     * 全局兜底异常处理。
     *
     * @param ex 捕获的未知异常
     * @return 包含系统内部错误信息的 {@link Result} 响应，HTTP 状态码 200
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NonNull Result<Void>> handleException(Exception ex) {
        log.error("全局异常处理-{}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        Result<Void> result = exceptionProcessor.process(
            new BusinessException(SystemErrorCode.SYSTEM_INTERNAL_ERROR, ex.getMessage()),
            PrincipalContextHolder.getAcceptLanguage()
        );
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
