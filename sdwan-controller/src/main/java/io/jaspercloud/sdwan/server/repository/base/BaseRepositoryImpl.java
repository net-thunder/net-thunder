package io.jaspercloud.sdwan.server.repository.base;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import io.jaspercloud.sdwan.server.entity.BaseEntity;
import io.jaspercloud.sdwan.server.support.BeanTransformer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BaseRepositoryImpl<D extends BaseEntity, P extends BasePO, M extends BaseMapper<P>>
        implements InitializingBean, BaseRepository<D> {

    private Class<?>[] typeArguments = GenericTypeUtils.resolveTypeArguments(getClass(), BaseRepositoryImpl.class);

    @Resource
    private BeanFactory beanFactory;

    private M baseMapper;

    public M getBaseMapper() {
        return baseMapper;
    }

    protected Class<D> currentDOClass() {
        return (Class<D>) typeArguments[0];
    }

    protected Class<P> currentPOClass() {
        return (Class<P>) typeArguments[1];
    }

    protected Class<BaseMapper<P>> currentMapperClass() {
        return (Class<BaseMapper<P>>) typeArguments[2];
    }

    private BeanTransformer<D, P> transformer;

    public BeanTransformer<D, P> getTransformer() {
        return transformer;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        baseMapper = (M) beanFactory.getBean(currentMapperClass());
        transformer = transformerBuilder().build();
    }

    protected BeanTransformer.Builder<D, P> transformerBuilder() {
        return BeanTransformer.builder(currentDOClass(), currentPOClass());
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public int insert(D entity) {
        P p = transformer.build(entity);
        int insert = getBaseMapper().insert(p);
        entity.setId(p.getId());
        return insert;
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public int updateById(D entity) {
        P p = transformer.build(entity);
        return getBaseMapper().updateById(p);
    }

    @Transactional(rollbackFor = Throwable.class)
    @Override
    public int deleteById(Serializable id) {
        return getBaseMapper().deleteById(id);
    }

    @Override
    public D selectById(Serializable id) {
        P p = getBaseMapper().selectById(id);
        if (null == p) {
            return null;
        }
        D d = transformer.output(p);
        return d;
    }

    @Override
    public List<D> selectBatchIds(Collection<? extends Serializable> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<P> list = getBaseMapper().selectBatchIds(idList);
        List<D> collect = list.stream().map(e -> transformer.output(e)).collect(Collectors.toList());
        return collect;
    }

    @Override
    public LambdaUpdateChainWrapperX<D> update() {
        return new LambdaUpdateChainWrapperX<>(baseMapper, currentPOClass());
    }

    @Override
    public LambdaDeleteChainWrapperX<D> delete() {
        return new LambdaDeleteChainWrapperX<>(baseMapper, currentPOClass());
    }

    @Override
    public LambdaQueryChainWrapperX<D, ?> query() {
        return new LambdaQueryChainWrapperX<>(baseMapper, currentPOClass(), transformer);
    }
}
