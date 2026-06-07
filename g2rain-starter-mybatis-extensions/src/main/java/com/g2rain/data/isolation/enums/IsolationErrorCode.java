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
    ISOLATION_TENANT_NON_SCOPE("isolation.50002", "租户条件不在允许范围: {0:tenantId} 当前租户不可访问"),

    /**
     * 权限策略上下文不完整
     */
    ISOLATION_POLICY_CONTEXT_INCOMPLETE("isolation.50003", "数据权限上下文不完整: module={0:moduleCode}, table={1:tableName}"),

    /**
     * 权限策略不存在
     */
    ISOLATION_POLICY_NOT_FOUND("isolation.50004", "数据权限策略不存在: module={0:moduleCode}, table={1:tableName}"),

    /**
     * 权限策略拒绝读取
     */
    ISOLATION_POLICY_READ_DENIED("isolation.50005", "数据权限策略拒绝读取: module={0:moduleCode}, table={1:tableName}"),

    /**
     * 权限策略拒绝写入
     */
    ISOLATION_POLICY_WRITE_DENIED("isolation.50006", "数据权限策略拒绝写入: module={0:moduleCode}, table={1:tableName}"),

    /**
     * Other 规则 SQL 无法解析
     */
    ISOLATION_POLICY_RULE_INVALID("isolation.50007", "数据权限 Other 规则无法解析: {0:rule}");

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
