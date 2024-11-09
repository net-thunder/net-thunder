package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditVNATRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.VNATResponse;
import io.jaspercloud.sdwan.server.entity.VNAT;
import io.jaspercloud.sdwan.server.repsitory.VNATMapper;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.VNATService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class VNATServiceImpl implements VNATService {

    @Resource
    private VNATMapper vnatMapper;

    @Resource
    private NodeService nodeService;

    @Override
    public void add(EditVNATRequest request) {
        VNAT vnat = BeanUtil.toBean(request, VNAT.class);
        vnat.setId(null);
        vnat.insert();
    }

    @Override
    public void edit(EditVNATRequest request) {
        VNAT vnat = BeanUtil.toBean(request, VNAT.class);
        vnat.updateById();
    }

    @Override
    public void del(EditVNATRequest request) {
        if (nodeService.usedVNAT(request.getId())) {
            throw new ProcessException("vnat used");
        }
        vnatMapper.deleteById(request.getId());
    }

    @Override
    public PageResponse<VNATResponse> page() {
        Long total = new LambdaQueryChainWrapper<>(vnatMapper).count();
        List<VNAT> list = new LambdaQueryChainWrapper<>(vnatMapper)
                .list();
        List<VNATResponse> collect = list.stream().map(e -> {
            VNATResponse vnatResponse = BeanUtil.toBean(e, VNATResponse.class);
            return vnatResponse;
        }).collect(Collectors.toList());
        PageResponse<VNATResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public List<VNAT> queryIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<VNAT> list = new LambdaQueryChainWrapper<>(vnatMapper)
                .in(VNAT::getId, idList)
                .list();
        return list;
    }
}
