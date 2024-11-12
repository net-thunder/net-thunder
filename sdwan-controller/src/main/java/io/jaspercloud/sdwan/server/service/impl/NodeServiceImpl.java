package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.*;
import io.jaspercloud.sdwan.server.repository.NodeRepository;
import io.jaspercloud.sdwan.server.repository.po.NodePO;
import io.jaspercloud.sdwan.server.service.*;
import io.jaspercloud.sdwan.server.support.LockGroup;
import io.jaspercloud.sdwan.support.Cidr;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NodeServiceImpl implements NodeService {

    @Resource
    private GroupService groupService;

    @Resource
    private RouteService routeService;

    @Resource
    private RouteRuleService routeRuleService;

    @Resource
    private VNATService vnatService;

    @Resource
    private NodeRepository nodeRepository;

    @Resource
    private TenantService tenantService;

    @Resource
    private LockGroup lockGroup;

    @Override
    public void add(EditNodeRequest request) {
        NodePO node = BeanUtil.toBean(request, NodePO.class);
        node.setId(null);
        node.insert();
        if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
            request.getGroupIdList().forEach(id -> {
                Group group = groupService.queryById(id);
                if (null == group) {
                    throw new ProcessException("not found group");
                }
                groupService.addMember(group.getId(), node.getId());
            });
        } else {
            Group group = groupService.queryDefaultGroup();
            groupService.addMember(group.getId(), node.getId());
        }
    }

    @Override
    public void edit(EditNodeRequest request) {
        NodePO node = BeanUtil.toBean(request, NodePO.class);
        node.updateById();
        if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
            groupService.delAllGroupMember(node.getId());
            request.getGroupIdList().forEach(id -> {
                Group group = groupService.queryById(id);
                if (null == group) {
                    throw new ProcessException("not found group");
                }
                groupService.addMember(group.getId(), node.getId());
            });
        } else {
            Group group = groupService.queryDefaultGroup();
            groupService.addMember(group.getId(), node.getId());
        }
    }

    @Override
    public void del(EditNodeRequest request) {
        if (routeService.usedNode(request.getId())) {
            throw new ProcessException("route used");
        }
        groupService.delAllGroupMember(request.getId());
        nodeRepository.deleteById(request.getId());
    }

    @Override
    public PageResponse<NodeResponse> page() {
        Long total = nodeRepository.count();
        List<Node> list = nodeRepository.list();
        List<NodeResponse> collect = list.stream().map(e -> {
            NodeResponse nodeResponse = BeanUtil.toBean(e, NodeResponse.class);
            return nodeResponse;
        }).collect(Collectors.toList());
        PageResponse<NodeResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public Node queryById(Long id) {
        Node node = nodeRepository.selectById(id);
        if (null == node) {
            return null;
        }
        List<Long> collect = groupService.queryGroupIdListByMemberId(node.getId());
        node.setGroupIdList(collect);
        return node;
    }

    @Override
    public NodeDetailResponse queryDetail(Long id) {
        Node node = nodeRepository.selectById(id);
        if (null == node) {
            return null;
        }
        List<Long> collect = groupService.queryGroupIdListByMemberId(node.getId());
        node.setGroupIdList(collect);
        NodeDetailResponse nodeDetail = BeanUtil.toBean(node, NodeDetailResponse.class);
        List<Long> groupIdList = groupService.queryGroupIdListByMemberId(node.getId());
        List<Group> groupList = groupService.queryDetailList(groupIdList);
        List<Route> routeList = routeService.queryByIdList(groupList.stream()
                .flatMap(e -> e.getRouteIdList().stream()).distinct().collect(Collectors.toList()));
        List<RouteRule> routeRuleList = routeRuleService.queryByIdList(groupList.stream()
                .flatMap(e -> e.getRouteRuleIdList().stream()).distinct().collect(Collectors.toList()));
        List<VNAT> vnatList = vnatService.queryIdList(groupList.stream()
                .flatMap(e -> e.getVnatIdList().stream()).distinct().collect(Collectors.toList()));
        nodeDetail.setRouteList(routeList);
        nodeDetail.setRouteRuleList(routeRuleList);
        nodeDetail.setVnatList(vnatList);
        return nodeDetail;
    }

    @Override
    public List<Node> queryByIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<Node> nodeList = nodeRepository.selectBatchIds(idList);
        return nodeList;
    }

    @Override
    public List<Node> queryByTenantId(Long tenantId) {
        List<Node> list = nodeRepository.list(nodeRepository.lambdaQuery()
                .eq(NodePO::getTenantId, tenantId));
        return list;
    }

    @Override
    public NodeDetailResponse applyNodeInfo(Long tenantId, String macAddress) {
        try (LockGroup.Lock lock = lockGroup.getLock(tenantId)) {
            Node node = nodeRepository.one(nodeRepository.lambdaQuery()
                    .eq(NodePO::getMac, macAddress)
                    .eq(NodePO::getEnable, true));
            if (null == node) {
                Tenant tenant = tenantService.queryById(tenantId);
                Cidr cidr = Cidr.parseCidr(tenant.getCidr());
                String vip;
                do {
                    Integer idx = tenantService.incIpIndex(tenantId);
                    vip = cidr.genIpByIdx(idx);
                } while (!cidr.isAvailableIp(vip));
                NodePO nodePO = new NodePO();
                nodePO.setMac(macAddress);
                nodePO.setVip(vip);
                nodePO.setEnable(true);
                nodePO.insert();
                Group group = groupService.queryDefaultGroup();
                groupService.addMember(group.getId(), nodePO.getId());
                node = BeanUtil.toBean(nodePO, Node.class);
                List<Long> groupIdList = groupService.queryGroupIdListByMemberId(nodePO.getId());
                node.setGroupIdList(groupIdList);
            } else if (StringUtils.isNotEmpty(node.getVip())) {
                Tenant tenant = tenantService.queryById(tenantId);
                Cidr cidr = Cidr.parseCidr(tenant.getCidr());
                String vip;
                do {
                    Integer idx = tenantService.incIpIndex(tenantId);
                    vip = cidr.genIpByIdx(idx);
                } while (!cidr.isAvailableIp(vip));
                node.setVip(vip);
                nodeRepository.updateById(node);
            }
            NodeDetailResponse nodeDetail = BeanUtil.toBean(node, NodeDetailResponse.class);
            List<Long> groupIdList = groupService.queryGroupIdListByMemberId(node.getId());
            List<Group> groupList = groupService.queryDetailList(groupIdList);
            List<Route> routeList = routeService.queryByIdList(groupList.stream()
                    .flatMap(e -> e.getRouteIdList().stream()).distinct().collect(Collectors.toList()));
            List<RouteRule> routeRuleList = routeRuleService.queryByIdList(groupList.stream()
                    .flatMap(e -> e.getRouteRuleIdList().stream()).distinct().collect(Collectors.toList()));
            List<VNAT> vnatList = vnatService.queryIdList(groupList.stream()
                    .flatMap(e -> e.getVnatIdList().stream()).distinct().collect(Collectors.toList()));
            nodeDetail.setRouteList(routeList);
            nodeDetail.setRouteRuleList(routeRuleList);
            nodeDetail.setVnatList(vnatList);
            return nodeDetail;
        }
    }

}
