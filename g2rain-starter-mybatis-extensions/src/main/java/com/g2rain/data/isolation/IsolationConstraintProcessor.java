package com.g2rain.data.isolation;

import com.g2rain.common.enums.OrganType;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.enums.IsolationErrorCode;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.mybatis.extension.InvocationContext;
import com.g2rain.mybatis.extension.IsolationFieldExtractor;
import com.g2rain.mybatis.extension.PrepareProcessor;
import com.g2rain.mybatis.extension.SqlHelper;
import com.g2rain.mybatis.extension.SqlParserDelegate;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

/**
 * 数据隔离约束处理器。
 * <p>
 * 处理 UPDATE / DELETE 场景，在 SQL 的 WHERE 条件中自动追加组织隔离约束，
 * 防止越权修改或删除非授权组织数据。
 * </p>
 *
 * @author alpha
 * @since 2026/3/8
 */
public class IsolationConstraintProcessor extends PrepareProcessor {

    /**
     * 数据范围校验器。
     */
    private final DataScopeExaminer dataScopeExaminer;

    /**
     * 拦截器顺序
     */
    private final int order;

    /**
     * 构造函数
     *
     * @param order 拦截器执行顺序
     */
    public IsolationConstraintProcessor(DataScopeExaminer dataScopeExaminer, int order) {
        this.dataScopeExaminer = dataScopeExaminer;
        this.order = order;
    }

    /**
     * 只操作修改和删除
     *
     * @param context 当前调用上下文
     * @return 是否需要拦截
     */
    @Override
    public boolean shouldIntercept(InvocationContext context) {
        // 后端发起请求不进行拦截
        if (PrincipalContextHolder.isBackEnd()) {
            return false;
        }

        // 运营公司不进行拦截
        return !PrincipalContextHolder.isAdminCompany();
    }

    @Override
    protected void onPrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) throws SQLException {
        SqlHelper.StatementContext statementContext = SqlHelper.statement(sh);
        MappedStatement mappedStatement = statementContext.mappedStatement();
        SqlCommandType sct = mappedStatement.getSqlCommandType();
        // 拦截修改和删除数据操作
        if (sct != SqlCommandType.UPDATE && sct != SqlCommandType.DELETE) {
            return;
        }

        // 拦截被数据隔离注解标识的接口, 并且没有设置忽略隔离的方法
        DataIsolationMeta meta = DataIsolationCache.getMeta(mappedStatement.getId());
        if (Objects.isNull(meta)) {
            return;
        }

        // 获取 ScopedValue 的调用链上下文操作目标组织标识
        Long targetOrganId = PrincipalContextHolder.getTargetOrganId();

        // 如果是租户直接用租户的组织标识
        if (Objects.isNull(targetOrganId) && OrganType.isTenant(PrincipalContextHolder.getOrganType())) {
            targetOrganId = PrincipalContextHolder.getOrganId();
        }

        // 获取执行 Mapper 接口方法的参数中的目标组织标识
        if (Objects.isNull(targetOrganId)) {
            // 1. 获取configuration
            Configuration configuration = statementContext.configuration();

            /*
             * 2. 通过 BoundSql 对象（它持有原始 SQL 和 绑定的参数）提取 parameterObject
             * 这个对象就是 MyBatis 准备设置到 PreparedStatement 中的原始参数（实体、Map 或 基础类型）
             */
            Object parameter = statementContext.boundSql().getParameterObject();

            // 3. 获取数据隔离的实际参数名称
            String propertyName = meta.getOrganIdPropertyName();

            Set<Object> values = IsolationFieldExtractor.extractValues(configuration, parameter, propertyName);
            if (values.size() != 1) {
                throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NOT_EXIST, "tenantId");
            }

            Object val = values.iterator().next();
            targetOrganId = (val instanceof Number) ? ((Number) val).longValue() : null;
        }

        if (Objects.isNull(targetOrganId)) {
            throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NOT_EXIST, "tenantId");
        }

        if (!dataScopeExaminer.isOrganInScope(targetOrganId)) {
            throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NON_SCOPE, targetOrganId);
        }

        try {
            SqlHelper.SqlContext sqlContext = statementContext.sqlContext();
            Statement statement = SqlParserDelegate.parse(sqlContext.sql());
            if (statement instanceof Update update) {
                update.setWhere(this.andExpression(
                    update.getTable(),
                    update.getWhere(),
                    meta.getOrganIdColumnName(),
                    targetOrganId
                ));
            } else if (statement instanceof Delete delete) {
                delete.setWhere(this.andExpression(
                    delete.getTable(),
                    delete.getWhere(),
                    meta.getOrganIdColumnName(),
                    targetOrganId
                ));
            }

            sqlContext.sql(statement.toString());
        } catch (JSQLParserException e) {
            throw new SQLException(e);
        }
    }

    /**
     * 返回处理器执行顺序。
     *
     * @return 顺序值
     */
    @Override
    public int order() {
        return this.order;
    }

    /**
     * 生成并拼接组织隔离条件。
     *
     * @param table        主表
     * @param where        原始 where 条件
     * @param columnName   组织字段名
     * @param whereSegment 组织字段值
     * @return 拼接后的 where 表达式
     */
    private Expression andExpression(Table table, Expression where, final String columnName, final Long whereSegment) {
        //获得 where 条件表达式
        StringBuilder column = new StringBuilder();
        if (Objects.nonNull(table.getAlias())) {
            column.append(table.getAlias().getName()).append(".");
        }

        final Expression expression = new EqualsTo(new Column(column.append(columnName).toString()), new LongValue(whereSegment));
        if (Objects.isNull(where)) {
            return expression;
        }

        if (where instanceof OrExpression) {
            return new AndExpression(new ParenthesedExpressionList<>(where), expression);
        }

        return new AndExpression(where, expression);
    }
}
