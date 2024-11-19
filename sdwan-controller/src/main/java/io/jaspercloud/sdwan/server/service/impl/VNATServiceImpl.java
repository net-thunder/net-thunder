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
@Transactional(rollbackFor = Exception.class)
public class VNATServiceImpl implements VNATService {

    @Resource
    private VNATRepository vnatRepository;

    @Resource
    private GroupConfigService groupConfigService;

    @Override
    public void add(EditVNATRequest request) {
        checkUnique(request.getId(), request.getName());
        VNATPO vnat = BeanUtil.toBean(request, VNATPO.class);
        vnat.setId(null);
        vnat.insert();
        if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
            groupConfigService.updateGroupVNAT(vnat.getId(), request.getGroupIdList());
        }
    }

    @Override
    public void edit(EditVNATRequest request) {
        checkUnique(request.getId(), request.getName());
        VNATPO vnat = BeanUtil.toBean(request, VNATPO.class);
        vnat.updateById();
        if (null != request.getGroupIdList()) {
            groupConfigService.updateGroupVNAT(vnat.getId(), request.getGroupIdList());
        }
    }

    private void checkUnique(Long id, String name) {
        Long count = vnatRepository.query()
                .eq(VNAT::getName, name)
                .func(null != id, w -> {
                    w.ne(VNAT::getId, id);
                })
                .count();
        if (count > 0) {
            throw new ProcessException("名称已存在");
        }
    }

    @Override
    public void del(EditVNATRequest request) {
        groupConfigService.deleteGroupVNAT(request.getId());
        vnatRepository.deleteById(request.getId());
    }

    @Override
    public List<VNATResponse> list() {
        List<VNAT> list = vnatRepository.query().list();
        List<VNATResponse> collect = list.stream().map(e -> {
            VNATResponse vnatResponse = BeanUtil.toBean(e, VNATResponse.class);
            return vnatResponse;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public PageResponse<VNATResponse> page() {
        Long total = vnatRepository.query().count();
        List<VNAT> list = vnatRepository.query().list();
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
        List<VNAT> list = vnatRepository.query()
                .in(VNAT::getId, idList)
                .list();
        return list;
    }

    @Override
    public VNAT queryDetailById(Long id) {
        VNAT vnat = vnatRepository.selectById(id);
        if (null == vnat) {
            return null;
        }
        List<Long> list = groupConfigService.queryGroupVNATList(id);
        vnat.setGroupIdList(list);
        return vnat;
    }
}
