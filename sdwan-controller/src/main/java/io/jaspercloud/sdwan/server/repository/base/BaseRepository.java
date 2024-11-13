package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import io.jaspercloud.sdwan.server.entity.BaseEntity;
import io.jaspercloud.sdwan.server.support.BeanTransformer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BaseRepository<D extends BaseEntity, P extends BasePO, M extends BaseMapper<P>> implements InitializingBean {

    private Class<?>[] typeArguments = GenericTypeUtils.resolveTypeArguments(getClass(), BaseRepository.class);

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

    @Override
    public void afterPropertiesSet() throws Exception {
        baseMapper = (M) beanFactory.getBean(currentMapperClass());
        transformer = transformerBuilder().build();
    }

    protected BeanTransformer.Builder<D, P> transformerBuilder() {
        return BeanTransformer.builder(currentDOClass(), currentPOClass());
    }

    public int insert(D entity) {
        P p = transformer.build(entity);
        return getBaseMapper().insert(p);
    }

    public int deleteById(Serializable id) {
        return getBaseMapper().deleteById(id);
    }

    public int deleteById(D entity) {
        P p = transformer.build(entity);
        return deleteById(p.getId());
    }

    public int updateById(D entity) {
        P p = transformer.build(entity);
        return getBaseMapper().updateById(p);
    }

    public int update(UpdateWrapper updateWrapper) {
        return getBaseMapper().update(updateWrapper);
    }

    public D selectById(Serializable id) {
        P p = getBaseMapper().selectById(id);
        D d = transformer.output(p);
        return d;
    }

    public List<D> selectBatchIds(Collection<? extends Serializable> idList) {
        List<P> list = getBaseMapper().selectBatchIds(idList);
        List<D> collect = list.stream().map(e -> transformer.output(e)).collect(Collectors.toList());
        return collect;
    }

    public Long count(Wrapper queryWrapper) {
        return baseMapper.selectCount(queryWrapper);
    }

    public Long count() {
        return count(Wrappers.emptyWrapper());
    }

    public List<D> list(Wrapper queryWrapper) {
        List<P> list = baseMapper.selectList(queryWrapper);
        List<D> collect = list.stream().map(e -> transformer.output(e)).collect(Collectors.toList());
        return collect;
    }

    public List<D> list() {
        return list(Wrappers.emptyWrapper());
    }

    public D one(Wrapper queryWrapper) {
        P selectOne = (P) baseMapper.selectOne(queryWrapper);
        if (null == selectOne) {
            return null;
        }
        D output = transformer.output(selectOne);
        return output;
    }

    public int delete(Wrapper queryWrapper) {
        return baseMapper.delete(queryWrapper);
    }

    public LambdaQueryWrapper<D> lambdaQuery() {
        return new LambdaQueryWrapper(currentPOClass());
    }

    public LambdaUpdateWrapper<D> lambdaUpdate() {
        return new LambdaUpdateWrapper(currentPOClass());
    }
}
