package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;

public class LambdaUpdateChainWrapperX<D> extends LambdaUpdateChainWrapper<D>
        implements Update<LambdaUpdateChainWrapper<D>, SFunction<D, ?>> {

    public LambdaUpdateChainWrapperX(BaseMapper baseMapper, Class entityClass) {
        super(baseMapper);
        super.wrapperChildren = new LambdaUpdateWrapper(entityClass);
    }
}
