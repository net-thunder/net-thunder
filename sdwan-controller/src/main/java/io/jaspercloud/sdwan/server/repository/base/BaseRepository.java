package io.jaspercloud.sdwan.server.repository.base;

import io.jaspercloud.sdwan.server.entity.BaseEntity;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface BaseRepository<D extends BaseEntity> {

    int insert(D entity);

    int updateById(D entity);

    int deleteById(Serializable id);

    D selectById(Serializable id);

    List<D> selectBatchIds(Collection<? extends Serializable> idList);

    LambdaUpdateChainWrapperX<D> update();

    LambdaDeleteChainWrapperX<D> delete();

    LambdaQueryChainWrapperX<D, ?> query();
}
