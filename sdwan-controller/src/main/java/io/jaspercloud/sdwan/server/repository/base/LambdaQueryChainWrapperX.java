package io.jaspercloud.sdwan.server.repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.jaspercloud.sdwan.server.support.BeanTransformer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LambdaQueryChainWrapperX<D, P> extends BaseLambdaQueryChainWrapperX<D, LambdaQueryChainWrapperX<D, P>> {

    private final BeanTransformer<D, P> transformer;

    public LambdaQueryChainWrapperX(BaseMapper<P> baseMapper, Class<P> entityClass, BeanTransformer<D, P> transformer) {
        super(baseMapper, entityClass);
        this.transformer = transformer;
    }

    @Override
    public List<D> list() {
        List<D> collect = super.list().stream()
                .map(e -> transformer.output((P) e))
                .collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<D> list(IPage<D> page) {
        List<D> collect = super.list(page).stream()
                .map(e -> transformer.output((P) e))
                .collect(Collectors.toList());
        return collect;
    }

    @Override
    public D one() {
        P p = (P) super.one();
        if (null == p) {
            return null;
        }
        D d = transformer.output(p);
        return d;
    }

    @Override
    public Optional<D> oneOpt() {
        return super.oneOpt().map(p -> transformer.output((P) p));
    }

    @Override
    public <E extends IPage<D>> E page(E pageParam) {
        E page = super.page(pageParam);
        List<D> collect = page.getRecords().stream()
                .map(e -> transformer.output((P) e))
                .collect(Collectors.toList());
        IPage result = Page.of(page.getCurrent(), page.getSize(), page.getTotal(), page.searchCount());
        result.setRecords(collect);
        return (E) result;
    }
}
