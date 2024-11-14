package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.AbstractChainWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;

import java.util.List;
import java.util.function.Predicate;

public class LambdaDeleteChainWrapperX<T> extends AbstractChainWrapper<T, SFunction<T, ?>, LambdaDeleteChainWrapperX<T>, LambdaQueryWrapper<T>>
        implements Query<LambdaDeleteChainWrapperX<T>, T, SFunction<T, ?>> {

    private final BaseMapper<T> baseMapper;
    private final Class<T> entityClass;

    public LambdaDeleteChainWrapperX(BaseMapper<T> baseMapper, Class<T> entityClass) {
        super();
        this.baseMapper = baseMapper;
        this.entityClass = entityClass;
        super.wrapperChildren = new LambdaQueryWrapper<>(entityClass);
    }

    private <R> R execute(SFunction<BaseMapper<T>, R> function) {
        if (baseMapper != null) {
            return function.apply(baseMapper);
        }
        return SqlHelper.execute(entityClass, function);
    }

    public boolean delete() {
        return execute(mapper -> mapper.delete(getWrapper())) > 0;
    }

    @Override
    public LambdaDeleteChainWrapperX<T> select(boolean condition, List<SFunction<T, ?>> columns) {
        return doSelect(condition, columns);
    }

    @Override
    @SafeVarargs
    public final LambdaDeleteChainWrapperX<T> select(SFunction<T, ?>... columns) {
        return doSelect(true, CollectionUtils.toList(columns));
    }

    @Override
    @SafeVarargs
    public final LambdaDeleteChainWrapperX<T> select(boolean condition, SFunction<T, ?>... columns) {
        return doSelect(condition, CollectionUtils.toList(columns));
    }

    /**
     * @since 3.5.4
     */
    protected LambdaDeleteChainWrapperX<T> doSelect(boolean condition, List<SFunction<T, ?>> columns) {
        wrapperChildren.select(condition, columns);
        return typedThis;
    }

    @Override
    public LambdaDeleteChainWrapperX<T> select(Class<T> entityClass, Predicate<TableFieldInfo> predicate) {
        wrapperChildren.select(entityClass, predicate);
        return typedThis;
    }

    @Override
    public String getSqlSelect() {
        throw ExceptionUtils.mpe("can not use this method for \"%s\"", "getSqlSelect");
    }

}