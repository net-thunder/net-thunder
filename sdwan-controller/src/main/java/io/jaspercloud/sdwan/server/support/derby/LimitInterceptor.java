package io.jaspercloud.sdwan.server.support.derby;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.BaseMultiTableInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;

public class LimitInterceptor extends BaseMultiTableInnerInterceptor implements InnerInterceptor {

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
        mpBs.sql(parserSingle(mpBs.sql(), null));
    }

    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        final String whereSegment = (String) obj;
        processSelectBody(select.getSelectBody(), whereSegment);
        List<WithItem> withItemsList = select.getWithItemsList();
        if (!CollectionUtils.isEmpty(withItemsList)) {
            withItemsList.forEach(withItem -> processSelectBody(withItem, whereSegment));
        }
    }

    @Override
    protected void processPlainSelect(PlainSelect plainSelect, String whereSegment) {
        super.processPlainSelect(plainSelect, whereSegment);
        Limit limit = plainSelect.getLimit();
        if (null != limit) {
            LongValue longValue = (LongValue) limit.getRowCount();
            Fetch fetch = new Fetch();
            fetch.setRowCount(longValue.getValue());
            fetch.setFetchParamFirst(true);
            fetch.setFetchParam("ROW");
            plainSelect.setFetch(fetch);
            plainSelect.setLimit(null);
        }
    }

    @Override
    public Expression buildTableExpression(Table table, Expression where, String whereSegment) {
        return null;
    }
}