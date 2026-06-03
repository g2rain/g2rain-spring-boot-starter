package com.g2rain.data.isolation.processor;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataIsolationCache;
import com.g2rain.data.isolation.DataScopeExaminer;
import com.g2rain.data.isolation.enums.IsolationErrorCode;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import com.g2rain.data.isolation.sql.DataIsolationSelectVisitor;
import com.g2rain.data.isolation.support.CachedDataPermissionPolicyResolver;
import com.g2rain.data.isolation.util.IsolationOrganSupport;
import com.g2rain.mybatis.extension.InvocationContext;
import com.g2rain.mybatis.extension.QueryProcessor;
import com.g2rain.mybatis.extension.SqlHelper;
import com.g2rain.mybatis.extension.SqlParserDelegate;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.util.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据隔离查询处理器。
 * <p>
 * 在查询 SQL 执行前完成两件事：
 * 1) 校验目标组织是否在当前登录组织可访问范围内；
 * 2) 为 SQL 自动追加组织隔离条件，并在配置动态策略时追加数据权限过滤条件。
 * </p>
 *
 * @author alpha
 * @since 2026/3/8
 */
public class IsolationQueryProcessor extends QueryProcessor {

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
    public IsolationQueryProcessor(DataScopeExaminer dataScopeExaminer, CachedDataPermissionPolicyResolver dataPermissionPolicyResolver, int order) {
        this.dataScopeExaminer = dataScopeExaminer;
        this.dataPermissionPolicyResolver = dataPermissionPolicyResolver;
        this.order = order;
    }

    /**
     * 判断当前请求是否需要执行数据隔离拦截。
     * <p>
     * 后台系统调用和运营公司账号不参与该拦截。
     * </p>
     *
     * @param context 调用上下文
     * @return 是否拦截
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
    protected void onQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql) throws SQLException {
        // 拦截被数据隔离注解标识的接口, 并且没有设置忽略隔离的方法
        DataIsolationMeta meta = DataIsolationCache.getMeta(ms.getId());
        if (Objects.isNull(meta)) {
            return;
        }

        Configuration configuration = ms.getConfiguration();
        Long targetOrganId = IsolationOrganSupport.resolveTargetOrganId(meta, configuration, parameter);
        if (!dataScopeExaminer.isOrganInScope(targetOrganId)) {
            throw new BusinessException(IsolationErrorCode.ISOLATION_TENANT_NON_SCOPE, targetOrganId);
        }

        try {
            Statement statement = SqlParserDelegate.parse(boundSql.getSql());
            Select select = (Select) statement;
            PlainSelect plainSelect = select.getPlainSelect();
            if (Objects.isNull(plainSelect) || !(plainSelect.getFromItem() instanceof Table table)) {
                throw new SQLException("unsupported select for data isolation");
            }

            Expression permissionExpression = null;
            if (meta.hasDynamicPolicy()) {
                DataPermissionPolicyResolveResult policy = dataPermissionPolicyResolver.resolve(
                    targetOrganId, meta.getIsolationModule(), meta.getIsolationTable()
                );
                permissionExpression = buildCondition(table, meta, policy);
            }

            select.accept(new DataIsolationSelectVisitor(meta.getOrganIdColumnName(), targetOrganId, permissionExpression));
            SqlHelper.sql(boundSql).sql(select.toString());
        } catch (JSQLParserException e) {
            throw new SQLException(e);
        }
    }

    /**
     * 返回处理器执行顺序。
     *
     * @return 顺序值（值越小越先执行）
     */
    @Override
    public int order() {
        return this.order;
    }

    private static Expression buildCondition(Table table, DataIsolationMeta meta, DataPermissionPolicyResolveResult policy) {
        if (!meta.hasDynamicPolicy()) {
            return null;
        }

        String tablePrefix = Objects.nonNull(table.getAlias()) ? table.getAlias().getName() + "." : "";
        List<Expression> parts = new ArrayList<>();

        if (meta.hasUserColumn()) {
            Long userId = PrincipalContextHolder.getUserId();
            if (Objects.nonNull(userId)) {
                parts.add(new EqualsTo(new Column(tablePrefix + meta.getUserIdColumnName()), new LongValue(userId)));
            }
        }

        if (Objects.nonNull(policy)) {
            if (policy.isGroupRead() && meta.hasDeptColumn()) {
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

            if (policy.isOtherRead() && StringUtils.hasText(policy.getOtherPermRule())) {
                try {
                    parts.add(CCJSqlParserUtil.parseCondExpression("(" + policy.getOtherPermRule() + ")"));
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

            var leftExpr = new ParenthesedExpressionList<>(combined);
            var rightExpr = new ParenthesedExpressionList<>(part);
            combined = new OrExpression(leftExpr, rightExpr);
        }

        return combined;
    }
}
