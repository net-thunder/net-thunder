package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.jaspercloud.sdwan.server.entity.BaseEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface BaseRepository<D extends BaseEntity> {

    int insert(D entity);

    int updateById(D entity);

    int deleteById(Serializable id);

    int deleteById(D entity);

    D selectById(Serializable id);

    List<D> selectBatchIds(Collection<? extends Serializable> idList);

    D one(Wrapper queryWrapper);

    List<D> list(Wrapper queryWrapper);

    IPage<D> page(Wrapper queryWrapper, IPage pageParam);

    int delete(LambdaQueryWrapper queryWrapper);

    LambdaQueryWrapper<D> lambdaQuery();

    LambdaUpdateChainWrapperX<D, ?> lambdaUpdateChain();

    LambdaQueryChainWrapperX<D, ?> lambdaQueryChain();
}
