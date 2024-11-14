package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;

public class LambdaUpdateChainWrapperX<D, P> extends LambdaUpdateChainWrapper<D>
        implements Update<LambdaUpdateChainWrapper<D>, SFunction<D, ?>> {

    public LambdaUpdateChainWrapperX(BaseMapper<P> baseMapper, Class<P> entityClass) {
        super((BaseMapper<D>) baseMapper);
        super.wrapperChildren = new LambdaUpdateWrapper(entityClass);
    }

    @Override
    public boolean update() {
        return super.update();
    }
}
