package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditVNATRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.VNAT;
import io.jaspercloud.sdwan.server.entity.VNATNodeItem;
import io.jaspercloud.sdwan.server.repository.VNATNodeItemRepository;
import io.jaspercloud.sdwan.server.repository.VNATRepository;
import io.jaspercloud.sdwan.server.repository.po.VNATNodeItemPO;
import io.jaspercloud.sdwan.server.repository.po.VNATPO;
import io.jaspercloud.sdwan.server.service.GroupConfigService;
import io.jaspercloud.sdwan.server.service.VNATService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class VNATServiceImpl implements VNATService {

    @Resource
    private VNATRepository vnatRepository;

    @Resource
    private VNATNodeItemRepository vnatNodeItemRepository;

    @Resource
    private GroupConfigService groupConfigService;

    @Override
    public void add(EditVNATRequest request) {
        checkUnique(request.getId(), request.getName());
        VNATPO vnat = BeanUtil.toBean(request, VNATPO.class);
        vnat.setId(null);
        vnat.insert();
        updateVNATItemList(vnat.getId(), request.getNodeIdList());
        groupConfigService.updateGroupVNAT(vnat.getId(), request.getGroupIdList());
    }

    @Override
    public void edit(EditVNATRequest request) {
        checkUnique(request.getId(), request.getName());
        VNATPO vnat = BeanUtil.toBean(request, VNATPO.class);
        vnat.updateById();
        updateVNATItemList(vnat.getId(), request.getNodeIdList());
        groupConfigService.updateGroupVNAT(vnat.getId(), request.getGroupIdList());
    }

    private void updateVNATItemList(Long id, List<Long> nodeIdList) {
        vnatNodeItemRepository.delete()
                .eq(VNATNodeItem::getVnatId, id)
                .delete();
        for (Long nodeId : nodeIdList) {
            VNATNodeItemPO itemPO = new VNATNodeItemPO();
            itemPO.setVnatId(id);
            itemPO.setNodeId(nodeId);
            itemPO.insert();
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
        vnatNodeItemRepository.delete()
                .eq(VNATNodeItem::getVnatId, request.getId())
                .delete();
        vnatRepository.deleteById(request.getId());
    }

    @Override
    public List<VNAT> list() {
        List<VNAT> list = vnatRepository.query().list();
        List<VNAT> collect = list.stream().map(item -> {
            List<Long> nodeIdList = vnatNodeItemRepository.query()
                    .eq(VNATNodeItem::getVnatId, item.getId())
                    .list()
                    .stream().map(e -> e.getNodeId()).collect(Collectors.toList());
            item.setNodeIdList(nodeIdList);
            return item;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public PageResponse<VNAT> page() {
        Long total = vnatRepository.query().count();
        List<VNAT> list = vnatRepository.query().list();
        List<VNAT> collect = list.stream().map(item -> {
            List<Long> nodeIdList = vnatNodeItemRepository.query()
                    .eq(VNATNodeItem::getVnatId, item.getId())
                    .list()
                    .stream().map(e -> e.getNodeId()).collect(Collectors.toList());
            item.setNodeIdList(nodeIdList);
            return item;
        }).collect(Collectors.toList());
        PageResponse<VNAT> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public VNAT queryById(Long id) {
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
        Map<Long, List<Long>> map = vnatNodeItemRepository.query()
                .in(VNATNodeItem::getVnatId, list.stream().map(e -> e.getId()).collect(Collectors.toList()))
                .list().stream().collect(Collectors.groupingBy(e -> e.getVnatId(), Collectors.mapping(e -> e.getNodeId(), Collectors.toList())));
        list.forEach(e -> {
            List<Long> itemList = map.getOrDefault(e.getId(), Collections.emptyList());
            e.setNodeIdList(itemList);
        });
        return list;
    }

    @Override
    public VNAT queryDetailById(Long id) {
        VNAT vnat = vnatRepository.selectById(id);
        if (null == vnat) {
            return null;
        }
        List<Long> nodeIdList = vnatNodeItemRepository.query()
                .eq(VNATNodeItem::getVnatId, id)
                .list()
                .stream().map(e -> e.getNodeId()).collect(Collectors.toList());
        vnat.setNodeIdList(nodeIdList);
        List<Long> groupIdList = groupConfigService.queryGroupVNATList(id);
        vnat.setGroupIdList(groupIdList);
        return vnat;
    }

    @Override
    public boolean usedNode(Long nodeId) {
        Long count = vnatNodeItemRepository.query()
                .eq(VNATNodeItem::getNodeId, nodeId)
                .count();
        return count > 0;
    }
}
