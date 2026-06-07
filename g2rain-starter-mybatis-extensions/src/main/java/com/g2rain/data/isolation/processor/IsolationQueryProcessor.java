package com.g2rain.data.isolation.processor;

import com.g2rain.common.enums.OrganType;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.common.web.PrincipalContextHolder;
import com.g2rain.data.isolation.DataIsolationCache;
import com.g2rain.data.isolation.DataScopeExaminer;
import com.g2rain.data.isolation.enums.IsolationErrorCode;
import com.g2rain.data.isolation.model.DataIsolationMeta;
import com.g2rain.data.isolation.model.DataPermissionPolicyResolveResult;
import com.g2rain.data.isolation.sql.DataIsolationSelectVisitor;
import com.g2rain.data.isolation.sql.DataPermissionConditionBuilder;
import com.g2rain.data.isolation.support.CachedDataPermissionPolicyResolver;
import com.g2rain.data.isolation.util.IsolationOrganSupport;
import com.g2rain.mybatis.extension.InvocationContext;
import com.g2rain.mybatis.extension.QueryProcessor;
import com.g2rain.mybatis.extension.SqlHelper;
import com.g2rain.mybatis.extension.SqlParserDelegate;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
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
        if (PrincipalContextHolder.isAdminCompany()) {
            return false;
        }

        return OrganType.isTenant(PrincipalContextHolder.getOrganType());
    }

    @Override
    protected void onQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql) throws SQLException {
        // 拦截被数据隔离注解标识的接口, 并且没有设置忽略隔离的方法
        DataIsolationMeta meta = DataIsolationCache.getMeta(ms.getId());
        if (Objects.isNull(meta)) {
            return;
        }

        Long targetOrganId = IsolationOrganSupport.resolveTargetOrganId();
        // 这里暂时用不上, 不过保留, 后续对于公司、渠道商、服务商等类型的机构可能会继续使用
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
                permissionExpression = DataPermissionConditionBuilder.buildReadCondition(table, meta, policy);
            }

            select.accept(new DataIsolationSelectVisitor(
                meta.getOrganIdColumnName(),
                targetOrganId,
                permissionExpression
            ));
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
}
