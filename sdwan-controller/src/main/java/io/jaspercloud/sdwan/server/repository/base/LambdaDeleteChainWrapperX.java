package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

public class LambdaDeleteChainWrapperX<D> extends BaseLambdaQueryChainWrapperX<D, LambdaDeleteChainWrapperX<D>> {

    public LambdaDeleteChainWrapperX(BaseMapper baseMapper, Class entityClass) {
        super(baseMapper, entityClass);
    }

    public boolean delete() {
        return execute(mapper -> mapper.delete(getWrapper())) > 0;
    }
}