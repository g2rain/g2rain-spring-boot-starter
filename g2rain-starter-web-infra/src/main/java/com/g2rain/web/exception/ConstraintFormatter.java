package com.g2rain.web.exception;


import com.g2rain.common.utils.Moments;
import com.g2rain.common.utils.Strings;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Negative;
import jakarta.validation.constraints.NegativeOrZero;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.CreditCardNumber;
import org.hibernate.validator.constraints.EAN;
import org.hibernate.validator.constraints.LuhnCheck;
import org.hibernate.validator.constraints.Mod10Check;
import org.hibernate.validator.constraints.Mod11Check;
import org.hibernate.validator.constraints.Range;
import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.constraints.UUID;
import org.springframework.validation.FieldError;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * <p>{@code ConstraintFormatter} 是一个用于将 Spring 验证框架（JSR 380 / Hibernate Validator）中的 {@link FieldError} 转换为可读提示信息的工具类。</p>
 * <p>
 * 它支持处理常见验证注解类型：
 * <ul>
 *     <li>Size / Range / Min / Max / DecimalMin / DecimalMax / Positive / Negative / PositiveOrZero / NegativeOrZero 等数字范围约束</li>
 *     <li>NotNull / NotEmpty / NotBlank 等空值约束</li>
 *     <li>Past / PastOrPresent / Future / FutureOrPresent 等日期约束</li>
 *     <li>Pattern / Email / CreditCardNumber / URL / UUID / LuhnCheck / Mod10Check / Mod11Check / EAN 等格式校验</li>
 * </ul>
 * </p>
 *
 * <p>示例用法：</p>
 * <pre>{@code
 * FieldError fieldError = ...; // 从 BindingResult 中获取
 * // 构建数字范围提示
 * String rangeMsg = ConstraintFormatter.buildRangeMessage(fieldError);
 * // 构建长度提示
 * String sizeMsg = ConstraintFormatter.buildSizeMessage(fieldError);
 * // 构建空值提示
 * boolean isBlankRequired = ConstraintFormatter.buildBlankMessage(fieldError);
 * // 构建日期约束提示
 * String dateMsg = ConstraintFormatter.buildDateMessage(fieldError);
 * // 构建正则/格式约束提示
 * String patternMsg = ConstraintFormatter.buildPatternMessage(fieldError);
 * // 提取属性名称
 * String property = ConstraintFormatter.extractProperty(fieldError.getField());
 * }</pre>
 *
 * <p>本类为工具类，禁止实例化。</p>
 *
 * @author alpha
 * @since 2025/10/12
 */
public class ConstraintFormatter {
    /**
     * 私有构造，禁止实例化
     */
    private ConstraintFormatter() {

    }

    /**
     * 定义每种数字或范围约束注解对应的处理函数。
     * 函数统一返回 [min, max] 数组：
     * <ul>
     *     <li>索引 0 对应最小值</li>
     *     <li>索引 1 对应最大值</li>
     * </ul>
     */
    @SuppressWarnings("ConstantConditions")
    private static final Map<String, Function<FieldError, Long[]>> rangMappers = Map.of(
        Min.class.getSimpleName(), fe -> {
            Object[] arguments = fe.getArguments();
            if (Objects.isNull(arguments) || arguments.length == 0) {
                return new Long[]{null, null};
            }

            return new Long[]{(Long) arguments[arguments.length - 1], null};
        },
        PositiveOrZero.class.getSimpleName(), fe -> new Long[]{0L, null},
        DecimalMin.class.getSimpleName(), fe -> {
            Object[] arguments = fe.getArguments();
            if (Objects.isNull(arguments) || arguments.length == 0) {
                return new Long[]{null, null};
            }

            return new Long[]{(Long) arguments[arguments.length - 1], null};
        },
        Max.class.getSimpleName(), fe -> {
            Object[] arguments = fe.getArguments();
            if (Objects.isNull(arguments) || arguments.length == 0) {
                return new Long[]{null, null};
            }

            return new Long[]{null, (Long) arguments[arguments.length - 1]};
        },
        NegativeOrZero.class.getSimpleName(), fe -> new Long[]{null, 0L},
        DecimalMax.class.getSimpleName(), fe -> {
            Object[] arguments = fe.getArguments();
            if (Objects.isNull(arguments) || arguments.length == 0) {
                return new Long[]{null, null};
            }

            return new Long[]{null, (Long) arguments[arguments.length - 1]};
        },
        Range.class.getSimpleName(), fe -> {
            Object[] arguments = fe.getArguments();
            if (Objects.isNull(arguments) || arguments.length == 0) {
                return new Long[]{null, null};
            }

            return new Long[]{(Long) arguments[arguments.length - 1], (Long) arguments[arguments.length - 2]};
        }
    );

    /**
     * 构建 {@code @Size} 注解的范围提示信息。
     *
     * @param fieldError 验证错误对象
     * @return 可读范围字符串，例如 "[1-10]"，若不可用返回空字符串
     */
    @SuppressWarnings("ConstantConditions")
    public static String buildSizeMessage(FieldError fieldError) {
        String code = fieldError.getCode();
        if (Strings.isBlank(code)) {
            return null;
        }

        if (!Size.class.getSimpleName().equals(code)) {
            return null;
        }

        Object[] arguments = fieldError.getArguments();
        if (Objects.isNull(arguments) || arguments.length == 0) {
            return "";
        }

        Number minNum = (Number) arguments[arguments.length - 1];
        Number maxNum = (Number) arguments[arguments.length - 2];
        return "[" + minNum + "-" + maxNum + "]";
    }

    /**
     * 构建数字或范围类约束的提示信息。
     *
     * @param fieldError 验证错误对象
     * @return 可读范围提示，例如 "[0-100]"、">=0"、"&lt;=10" 或 ">0" / "&lt;0"，若不可解析返回 null
     */
    @SuppressWarnings("ConstantConditions")
    public static String buildRangeMessage(FieldError fieldError) {
        String code = fieldError.getCode();
        if (Strings.isBlank(code)) {
            return null;
        }

        if (Positive.class.getSimpleName().equals(code)) {
            return ">0";
        }

        if (Negative.class.getSimpleName().equals(code)) {
            return "<0";
        }

        if (Digits.class.getSimpleName().equals(code)) {
            Object[] arguments = fieldError.getArguments();
            if (Objects.isNull(arguments) || arguments.length == 0) {
                return "";
            }
            Object integer = arguments[arguments.length - 1];
            Object fraction = arguments[arguments.length - 2];
            return "integer <= " + integer + " && fraction <= " + fraction;
        }

        Function<FieldError, Long[]> func = rangMappers.get(code);
        if (Objects.isNull(func)) {
            return null;
        }

        // 构建可读范围字符串
        Long[] r = func.apply(fieldError);
        if (Objects.nonNull(r[0]) && Objects.nonNull(r[1])) {
            return "[" + r[0] + "-" + r[1] + "]";
        }

        if (Objects.nonNull(r[0])) {
            return ">= " + r[0];
        }

        if (Objects.nonNull(r[1])) {
            return "<= " + r[1];
        }

        return null;
    }

    /**
     * 判断字段是否存在空值约束。
     *
     * @param fieldError 验证错误对象
     * @return true 表示存在 NotNull / NotEmpty / NotBlank 等约束，false 表示不存在
     */
    public static boolean buildBlankMessage(FieldError fieldError) {
        String code = fieldError.getCode();
        if (Strings.isBlank(code)) {
            return false;
        }

        if (NotNull.class.getSimpleName().equals(code)) {
            return true;
        }

        if (NotEmpty.class.getSimpleName().equals(code)) {
            return true;
        }

        return NotBlank.class.getSimpleName().equals(code);
    }

    /**
     * 构建日期类型约束的提示信息。
     *
     * @param fieldError 验证错误对象
     * @return 可读日期约束提示，例如 "&lt;2025-10-13T12:00:00"、">=2025-10-13T12:00:00" 等
     */
    public static String buildDateMessage(FieldError fieldError) {
        String code = fieldError.getCode();
        if (Strings.isBlank(code)) {
            return null;
        }

        if (Past.class.getSimpleName().equals(code)) {
            return "<" + Moments.nowString();
        }

        if (PastOrPresent.class.getSimpleName().equals(code)) {
            return "<=" + Moments.nowString();
        }

        if (Future.class.getSimpleName().equals(code)) {
            return ">" + Moments.nowString();
        }

        if (FutureOrPresent.class.getSimpleName().equals(code)) {
            return ">=" + Moments.nowString();
        }

        return null;
    }

    /**
     * 构建正则或格式约束的提示信息。
     *
     * @param fieldError 验证错误对象
     * @return 可读提示，例如正则表达式字符串或 "Email"/"UUID"/"URL" 等
     */
    @SuppressWarnings("ConstantConditions")
    public static String buildPatternMessage(FieldError fieldError) {
        String code = fieldError.getCode();
        if (Strings.isBlank(code)) {
            return null;
        }

        if (Pattern.class.getSimpleName().equals(code) || Email.class.getSimpleName().equals(code)) {
            Object[] arguments = fieldError.getArguments();
            if (Objects.isNull(arguments) || arguments.length == 0) {
                return "";
            }

            return Objects.toString(arguments[arguments.length - 1], "");
        }

        if (CreditCardNumber.class.getSimpleName().equals(code)) {
            return "CreditCardNumber";
        }

        if (URL.class.getSimpleName().equals(code)) {
            return "URL";
        }

        if (UUID.class.getSimpleName().equals(code)) {
            return "UUID";
        }

        if (LuhnCheck.class.getSimpleName().equals(code)) {
            return "LuhnCheck";
        }

        if (Mod10Check.class.getSimpleName().equals(code)) {
            return "Mod10Check";
        }

        if (Mod11Check.class.getSimpleName().equals(code)) {
            return "Mod11Check";
        }

        if (EAN.class.getSimpleName().equals(code)) {
            return "EAN";
        }

        return null;
    }

    /**
     * 从字段路径中提取属性名称。
     * 例如 "user.address.street" → "street"
     *
     * @param propertyPath 字段路径
     * @return 属性名称，若为空或无 '.' 则返回原字符串或空字符串
     */
    public static String extractProperty(String propertyPath) {
        if (Strings.isBlank(propertyPath)) {
            return "";
        }

        int dot = propertyPath.lastIndexOf('.');
        return dot >= 0 ? propertyPath.substring(dot + 1) : propertyPath;
    }
}
