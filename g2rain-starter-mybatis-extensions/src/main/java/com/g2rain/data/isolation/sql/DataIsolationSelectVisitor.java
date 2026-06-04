package com.g2rain.data.isolation.sql;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;

import java.util.Objects;

/**
 * 数据隔离 SELECT 语句访问器。
 * <p>
 * 用于在查询 SQL 的 where 条件中注入组织过滤表达式，以及可选的数据权限策略表达式。
 * </p>
 *
 * @author alpha
 * @since 2026/3/9
 */
public class DataIsolationSelectVisitor extends SelectVisitorAdapter {

    /**
     * 组织字段名（如 tenant_id）。
     */
    private final String columnName;

    /**
     * 组织字段值（目标组织 ID）。
     */
    private final Long whereSegment;

    /**
     * 数据权限策略过滤表达式。
     */
    private final Expression permissionExpr;

    /**
     * 构造函数。
     *
     * @param columnName   组织字段名
     * @param whereSegment 组织字段值
     */
    public DataIsolationSelectVisitor(String columnName, Long whereSegment) {
        this(columnName, whereSegment, null);
    }

    /**
     * 构造函数。
     *
     * @param columnName     组织字段名
     * @param whereSegment   组织字段值
     * @param permissionExpr 数据权限策略过滤表达式
     */
    public DataIsolationSelectVisitor(String columnName, Long whereSegment, Expression permissionExpr) {
        this.columnName = columnName;
        this.whereSegment = whereSegment;
        this.permissionExpr = permissionExpr;
    }

    /**
     * 访问并改写单表查询语句的 where 条件。
     *
     * @param plainSelect JSqlParser 解析后的查询对象
     */
    @Override
    public void visit(PlainSelect plainSelect) {
        FromItem from = plainSelect.getFromItem();
        if (!(from instanceof Table table)) {
            return;
        }

        Expression combined = buildOrganExpression(table);
        if (Objects.nonNull(permissionExpr)) {
            combined = new AndExpression(
                new ParenthesedExpressionList<>(combined),
                new ParenthesedExpressionList<>(permissionExpr)
            );
        }

        Expression where = plainSelect.getWhere();
        if (Objects.isNull(where)) {
            plainSelect.setWhere(combined);
            return;
        }

        plainSelect.setWhere(new AndExpression(
            new ParenthesedExpressionList<>(where), combined
        ));
    }

    private Expression buildOrganExpression(Table table) {
        StringBuilder column = new StringBuilder();
        if (Objects.nonNull(table.getAlias())) {
            column.append(table.getAlias().getName()).append(".");
        }

        Column colName = new Column(column.append(columnName).toString());
        LongValue colValue = new LongValue(whereSegment);
        return new EqualsTo(colName, colValue);
    }
}
