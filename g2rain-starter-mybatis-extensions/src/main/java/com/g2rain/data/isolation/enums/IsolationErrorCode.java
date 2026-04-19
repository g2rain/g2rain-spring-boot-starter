package com.g2rain.data.isolation.enums;


import com.g2rain.common.exception.ErrorCode;

/**
 * 数据隔离模块错误码定义。
 *
 * @author alpha
 * @since 2025/10/13
 */
public enum IsolationErrorCode implements ErrorCode {
    /**
     * 租户条件不存在
     */
    ISOLATION_TENANT_NOT_EXIST("isolation.50001", "租户条件不存在: {0:tenantId} 参数未提供"),

    /**
     * 租户不在可访问范围内
     */
    ISOLATION_TENANT_NON_SCOPE("isolation.50002", "租户条件不在允许范围: {0:tenantId} 当前租户不可访问");

    /**
     * 错误码。
     */
    private final String code;

    /**
     * 错误消息模板。
     */
    private final String messageTemplate;

    IsolationErrorCode(String code, String messageTemplate) {
        this.code = code;
        this.messageTemplate = messageTemplate;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String messageTemplate() {
        return messageTemplate;
    }
}
