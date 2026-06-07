package com.g2rain.data.isolation.processor;

import com.g2rain.common.enums.OrganType;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataIsolationCache;
import com.g2rain.data.isolation.DataScopeExaminer;
import com.g2rain.data.isolation.enums.IsolationErrorCode;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import com.g2rain.data.isolation.sql.DataPermissionConditionBuilder;
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
        if (PrincipalContextHolder.isAdminCompany()) {
            return false;
        }

        return OrganType.isTenant(PrincipalContextHolder.getOrganType());
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

        Long targetOrganId = IsolationOrganSupport.resolveTargetOrganId();
        // 这里暂时用不上, 不过保留, 后续对于公司、渠道商、服务商等类型的机构可能会继续使用
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
                    targetOrganId,
                    buildWriteCondition(targetOrganId, meta, update.getTable())
                ));
            } else if (statement instanceof Delete delete) {
                delete.setWhere(this.andExpression(
                    delete.getTable(),
                    delete.getWhere(),
                    meta.getOrganIdColumnName(),
                    targetOrganId,
                    buildWriteCondition(targetOrganId, meta, delete.getTable())
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

    private Expression buildWriteCondition(Long targetOrganId, DataIsolationMeta meta, Table table) {
        if (!meta.hasDynamicPolicy()) {
            return null;
        }

        DataPermissionPolicyResolveResult policy = dataPermissionPolicyResolver.resolve(
            targetOrganId, meta.getIsolationModule(), meta.getIsolationTable()
        );

        return DataPermissionConditionBuilder.buildWriteCondition(table, meta, policy);
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
    private Expression andExpression(Table table, Expression where, final String columnName, final Long whereSegment, Expression permissionExpr) {
        //获得 where 条件表达式
        StringBuilder column = new StringBuilder();
        if (Objects.nonNull(table.getAlias())) {
            column.append(table.getAlias().getName()).append(".");
        }

        Expression combined = new EqualsTo(new Column(column.append(columnName).toString()), new LongValue(whereSegment));
        if (Objects.nonNull(permissionExpr)) {
            combined = new AndExpression(new ParenthesedExpressionList<>(combined), new ParenthesedExpressionList<>(permissionExpr));
        }

        if (Objects.isNull(where)) {
            return combined;
        }

        if (where instanceof OrExpression) {
            return new AndExpression(new ParenthesedExpressionList<>(where), combined);
        }

        return new AndExpression(where, combined);
    }
}
