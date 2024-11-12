package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditVNATRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.VNATResponse;
import io.jaspercloud.sdwan.server.entity.VNAT;
import io.jaspercloud.sdwan.server.repository.VNATRepository;
import io.jaspercloud.sdwan.server.repository.po.VNATPO;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
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
    private VNATRepository vnatRepository;

    @Resource
    private GroupConfigService groupConfigService;

    @Override
    public void add(EditVNATRequest request) {
        VNATPO vnat = BeanUtil.toBean(request, VNATPO.class);
        vnat.setId(null);
        vnat.insert();
    }

    @Override
    public void edit(EditVNATRequest request) {
        VNATPO vnat = BeanUtil.toBean(request, VNATPO.class);
        vnat.updateById();
    }

    @Override
    public void del(EditVNATRequest request) {
        if (groupConfigService.usedVNAT(request.getId())) {
            throw new ProcessException("vnat used");
        }
        vnatRepository.deleteById(request.getId());
    }

    @Override
    public PageResponse<VNATResponse> page() {
        Long total = vnatRepository.count();
        List<VNAT> list = vnatRepository.list();
        List<VNATResponse> collect = list.stream().map(e -> {
            VNATResponse vnatResponse = BeanUtil.toBean(e, VNATResponse.class);
            return vnatResponse;
        }).collect(Collectors.toList());
        PageResponse<VNATResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public VNAT queryId(Long id) {
        VNAT vnat = vnatRepository.selectById(id);
        return vnat;
    }

    @Override
    public List<VNAT> queryIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<VNAT> list = vnatRepository.list(vnatRepository.lambdaQuery()
                .in(VNATPO::getId, idList));
        return list;
    }
}
