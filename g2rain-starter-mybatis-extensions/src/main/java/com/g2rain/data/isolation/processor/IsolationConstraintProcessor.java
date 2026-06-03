package com.g2rain.data.isolation.processor;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataIsolationCache;
import com.g2rain.data.isolation.DataScopeExaminer;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import com.g2rain.data.isolation.support.CachedDataPermissionPolicyResolver;
import com.g2rain.data.isolation.util.IsolationOrganSupport;
import com.g2rain.mybatis.extension.InvocationContext;
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * 数据隔离约束处理器。
 * <p>
 * 处理 UPDATE / DELETE 场景，在 SQL 的 WHERE 条件中自动追加组织隔离约束，
 * 并在配置动态策略时追加数据权限过滤条件，防止越权修改或删除非授权数据。
 * </p>
 *
 * @author alpha
 * @since 2026/3/8
 */
@Deprecated
public class IsolationConstraintProcessor extends PrepareProcessor {

    /**
     * 数据范围校验器。
     */
    private final DataScopeExaminer dataScopeExaminer;

    /**
     * 数据权限策略解析器。
     */
    private final CachedDataPermissionPolicyResolver dataPermissionPolicyResolver;

    /**
     * 拦截器顺序
     */
    private final int order;

    /**
     * 构造函数
     *
     * @param order 拦截器执行顺序
     */
    public IsolationConstraintProcessor(DataScopeExaminer dataScopeExaminer, CachedDataPermissionPolicyResolver dataPermissionPolicyResolver, int order) {
        this.dataScopeExaminer = dataScopeExaminer;
        this.dataPermissionPolicyResolver = dataPermissionPolicyResolver;
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

        Object parameter = statementContext.boundSql().getParameterObject();
        Long targetOrganId = IsolationOrganSupport.resolveTargetOrganId(meta, statementContext.configuration(), parameter);
        if (!dataScopeExaminer.isOrganInScope(targetOrganId)) {
            throw new BusinessException(com.g2rain.data.isolation.enums.IsolationErrorCode.ISOLATION_TENANT_NON_SCOPE, targetOrganId);
        }

        Expression permissionExpression = null;
        if (meta.hasDynamicPolicy()) {
            DataPermissionPolicyResolveResult policy = dataPermissionPolicyResolver.resolve(
                targetOrganId, meta.getIsolationModule(), meta.getIsolationTable()
            );
            Table table = null;
            try {
                Statement statement = SqlParserDelegate.parse(statementContext.sqlContext().sql());
                if (statement instanceof Update update) {
                    table = update.getTable();
                } else if (statement instanceof Delete delete) {
                    table = delete.getTable();
                }
            } catch (JSQLParserException e) {
                throw new SQLException(e);
            }
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
     * 生成并拼接组织隔离条件，以及可选的数据权限策略条件。
     */
    private Expression andExpression(Table table, Expression where, final String columnName, final Long whereSegment) {
        Expression combined = buildOrganExpression(table, columnName, whereSegment);
        if (Objects.isNull(where)) {
            return combined;
        }

        if (where instanceof OrExpression) {
            return new AndExpression(new ParenthesedExpressionList<>(where), combined);
        }

        return new AndExpression(where, combined);
    }

    private Expression buildOrganExpression(Table table, String columnName, Long whereSegment) {
        StringBuilder column = new StringBuilder();
        if (Objects.nonNull(table.getAlias())) {
            column.append(table.getAlias().getName()).append(".");
        }

        return new EqualsTo(new Column(column.append(columnName).toString()), new LongValue(whereSegment));
    }
}
