package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.AbstractChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.ChainQuery;

import java.util.List;
import java.util.function.Predicate;

public class BaseLambdaQueryChainWrapperX<D, Children extends BaseLambdaQueryChainWrapperX<D, Children>> extends AbstractChainWrapper<D, SFunction<D, ?>, Children, LambdaQueryWrapper<D>>
        implements ChainQuery<D>, Query<Children, D, SFunction<D, ?>> {

    private final BaseMapper baseMapper;

    public BaseLambdaQueryChainWrapperX(BaseMapper baseMapper, Class entityClass) {
        super();
        this.baseMapper = baseMapper;
        super.wrapperChildren = new LambdaQueryWrapper(entityClass);

    }

    @Override
    public Children select(boolean condition, List<SFunction<D, ?>> columns) {
        return doSelect(condition, columns);
    }

    @Override
    @SafeVarargs
    public final Children select(SFunction<D, ?>... columns) {
        return doSelect(true, CollectionUtils.toList(columns));
    }

    @Override
    @SafeVarargs
    public final Children select(boolean condition, SFunction<D, ?>... columns) {
        return doSelect(condition, CollectionUtils.toList(columns));
    }

    /**
     * @since 3.5.4
     */
    protected Children doSelect(boolean condition, List<SFunction<D, ?>> columns) {
        wrapperChildren.select(condition, columns);
        return typedThis;
    }

    @Override
    public Children select(Class<D> entityClass, Predicate<TableFieldInfo> predicate) {
        wrapperChildren.select(entityClass, predicate);
        return typedThis;
    }

    @Override
    public String getSqlSelect() {
        throw ExceptionUtils.mpe("can not use this method for \"%s\"", "getSqlSelect");
    }

    @Override
    public BaseMapper getBaseMapper() {
        return baseMapper;
    }

    @Override
    public Class getEntityClass() {
        return super.wrapperChildren.getEntityClass();
    }
}