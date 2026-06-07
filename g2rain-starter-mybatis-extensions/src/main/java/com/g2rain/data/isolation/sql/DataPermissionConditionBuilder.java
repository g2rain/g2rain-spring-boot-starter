package com.g2rain.data.isolation.sql;

import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 根据权限策略构建 SQL 过滤条件。
 */
public final class DataPermissionConditionBuilder {

    private DataPermissionConditionBuilder() {
    }

    public static Expression buildReadCondition(Table table, DataIsolationMeta meta, DataPermissionPolicyResolveResult policy) {
        return buildCondition(table, meta, policy, true);
    }

    public static Expression buildWriteCondition(Table table, DataIsolationMeta meta, DataPermissionPolicyResolveResult policy) {
        return buildCondition(table, meta, policy, false);
    }

    private static Expression buildCondition(Table table, DataIsolationMeta meta, DataPermissionPolicyResolveResult policy, boolean read) {
        if (!meta.hasDynamicPolicy()) {
            return null;
        }

        String tablePrefix = Objects.nonNull(table.getAlias()) ? table.getAlias().getName() + "." : "";
        List<Expression> parts = new ArrayList<>();

        if (Objects.nonNull(policy)) {
            if (meta.hasUserColumn()) {
                Long userId = PrincipalContextHolder.getUserId();
                if (Objects.nonNull(userId)) {
                    parts.add(new EqualsTo(new Column(tablePrefix + meta.getUserIdColumnName()), new LongValue(userId)));
                }
            }

            boolean groupAllowed = read ? policy.isGroupRead() : policy.isGroupWrite();
            boolean otherAllowed = read ? policy.isOtherRead() : policy.isOtherWrite();

            if (groupAllowed && meta.hasDeptColumn()) {
                String deptPaths = PrincipalContextHolder.getDeptPath();
                if (StringUtils.hasText(deptPaths)) {
                    for (String deptPath : deptPaths.split(",")) {
                        String trimmed = deptPath.trim();
                        if (!StringUtils.hasText(trimmed)) {
                            continue;
                        }

                        LikeExpression like = new LikeExpression();
                        like.setLeftExpression(new Column(tablePrefix + meta.getDeptPathColumnName()));
                        like.setRightExpression(new StringValue(trimmed + "%"));
                        parts.add(like);
                    }
                }
            }

            if (otherAllowed && StringUtils.hasText(policy.getOtherPermRule())) {
                try {
                    parts.add(CCJSqlParserUtil.parseCondExpression(policy.getOtherPermRule()));
                } catch (JSQLParserException ignored) {
                    // 规则无法解析则跳过该分支
                }
            }
        }

        Expression combined = null;
        for (Expression part : parts) {
            if (Objects.isNull(combined)) {
                combined = part;
                continue;
            }

            combined = new OrExpression(combined, part);
        }

        return combined;
    }
}
