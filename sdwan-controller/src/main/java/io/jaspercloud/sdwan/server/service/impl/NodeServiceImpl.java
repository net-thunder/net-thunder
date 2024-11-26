package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.config.TenantContextHandler;
import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.*;
import io.jaspercloud.sdwan.server.repository.GroupMemberRepository;
import io.jaspercloud.sdwan.server.repository.NodeRepository;
import io.jaspercloud.sdwan.server.repository.mapper.VipPoolMapper;
import io.jaspercloud.sdwan.server.repository.po.NodePO;
import io.jaspercloud.sdwan.server.repository.po.VipPoolPO;
import io.jaspercloud.sdwan.server.service.*;
import io.jaspercloud.sdwan.server.support.LockGroup;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.util.ShortUUID;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class NodeServiceImpl implements NodeService, InitializingBean {

    @Resource
    private GroupService groupService;

    @Resource
    private GroupMemberRepository groupMemberRepository;

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
    private VipPoolMapper vipPoolMapper;

    @Resource
    private LockGroup lockGroup;

    @Resource
    private DataSourceTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @Override
    public void afterPropertiesSet() throws Exception {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void add(EditNodeRequest request) {
        checkUnique(request.getId(), request.getName(), request.getMac());
        Tenant tenant = tenantService.queryById(TenantContextHandler.getCurrentTenantId());
        NodePO node = BeanUtil.toBean(request, NodePO.class);
        node.setId(null);
        String vip = applyIp(tenant);
        node.setVip(vip);
        node.insert();
        if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
            request.getGroupIdList().forEach(id -> {
                Group group = groupService.queryById(id);
                if (null == group) {
                    throw new ProcessException("not found group");
                }
                groupService.addMember(group.getId(), node.getId());
            });
        }
    }

    @Override
    public void edit(EditNodeRequest request) {
        checkUnique(request.getId(), request.getName(), request.getMac());
        NodePO node = BeanUtil.toBean(request, NodePO.class);
        node.updateById();
        if (null != request.getGroupIdList()) {
            groupService.delAllGroupMember(node.getId());
            if (CollectionUtil.isNotEmpty(request.getGroupIdList())) {
                request.getGroupIdList().forEach(id -> {
                    Group group = groupService.queryById(id);
                    if (null == group) {
                        throw new ProcessException("not found group");
                    }
                    groupService.addMember(group.getId(), node.getId());
                });
            }
        }
    }

    private void checkUnique(Long id, String name, String mac) {
        Long nameCount = nodeRepository.query()
                .eq(Node::getName, name)
                .func(null != id, w -> {
                    w.ne(Node::getId, id);
                })
                .count();
        if (nameCount > 0) {
            throw new ProcessException("名称已存在");
        }
        Long macCount = nodeRepository.query()
                .eq(Node::getMac, mac)
                .func(null != id, w -> {
                    w.ne(Node::getId, id);
                })
                .count();
        if (macCount > 0) {
            throw new ProcessException("物理地址已存在");
        }
    }

    @Override
    public void del(EditNodeRequest request) {
        if (routeService.usedNode(request.getId())) {
            throw new ProcessException("被路由使用");
        }
        if (vnatService.usedNode(request.getId())) {
            throw new ProcessException("被地址转换使用");
        }
        Node node = nodeRepository.selectById(request.getId());
        if (null != node.getVip()) {
            new LambdaUpdateChainWrapper<>(vipPoolMapper)
                    .eq(VipPoolPO::getVip, node.getVip())
                    .set(VipPoolPO::getUsed, false)
                    .update();
        }
        groupService.delAllGroupMember(request.getId());
        nodeRepository.deleteById(request.getId());
    }

    @Override
    public List<NodeResponse> list() {
        List<Node> list = nodeRepository.query().list();
        List<NodeResponse> collect = list.stream().map(e -> {
            NodeResponse nodeResponse = BeanUtil.toBean(e, NodeResponse.class);
            return nodeResponse;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public PageResponse<NodeResponse> page() {
        Long total = nodeRepository.query().count();
        List<Node> list = nodeRepository.query().list();
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
        List<Long> groupIdList = groupService.queryGroupIdListByMemberId(node.getId());
        node.setGroupIdList(groupIdList);
        NodeDetailResponse nodeDetail = BeanUtil.toBean(node, NodeDetailResponse.class);
        List<Group> groupList = groupService.queryDetailList(groupIdList);
        List<Route> routeList = routeService.queryByIdList(groupList.stream()
                .flatMap(e -> e.getRouteIdList().stream()).distinct().collect(Collectors.toList()));
        List<RouteRule> routeRuleList = routeRuleService.queryByIdList(groupList.stream()
                .flatMap(e -> e.getRouteRuleIdList().stream()).distinct().collect(Collectors.toList()));
        List<VNAT> vnatList = vnatService.queryIdList(groupList.stream()
                .flatMap(e -> e.getVnatIdList().stream()).distinct().collect(Collectors.toList()));
        List<Node> nodeList;
        if (CollectionUtil.isNotEmpty(groupIdList)) {
            List<Long> nodeIdList = groupMemberRepository.query()
                    .in(GroupMember::getGroupId, groupIdList)
                    .list()
                    .stream()
                    .map(e -> e.getMemberId())
                    .distinct()
                    .collect(Collectors.toList());
            nodeList = nodeRepository.query()
                    .in(Node::getId, nodeIdList)
                    .list();
        } else {
            nodeList = Collections.emptyList();
        }
        nodeDetail.setGroupList(groupList);
        nodeDetail.setRouteList(routeList);
        nodeDetail.setRouteRuleList(routeRuleList);
        nodeDetail.setVnatList(vnatList);
        nodeDetail.setNodeList(nodeList);
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
        List<Node> list = nodeRepository.query()
                .list();
        return list;
    }

    @Override
    public NodeDetailResponse applyNodeInfo(Long tenantId, String macAddress) {
        try (LockGroup.Lock lock = lockGroup.getLock(tenantId)) {
            Node node = nodeRepository.query()
                    .eq(Node::getMac, macAddress)
                    .one();
            if (null == node) {
                Tenant tenant = tenantService.queryById(tenantId);
                NodePO nodePO = new NodePO();
                transactionTemplate.executeWithoutResult(s -> {
                    nodePO.setName(ShortUUID.gen());
                    nodePO.setMac(macAddress);
                    if (tenant.getNodeGrant()) {
                        nodePO.setEnable(false);
                    } else {
                        nodePO.setEnable(true);
                    }
                    nodePO.insert();
                    Group group = groupService.queryDefaultGroup();
                    groupService.addMember(group.getId(), nodePO.getId());
                });
                node = BeanUtil.toBean(nodePO, Node.class);
            }
            if (StringUtils.isEmpty(node.getVip())) {
                Tenant tenant = tenantService.queryById(tenantId);
                if (tenant.getNodeGrant() && false == node.getEnable()) {
                    throw new ProcessCodeException(SDWanProtos.MessageCode.NotGrant_VALUE);
                }
                String vip = applyIp(tenant);
                node.setVip(vip);
                nodeRepository.updateById(node);
            }
            if (!node.getEnable()) {
                throw new ProcessCodeException(SDWanProtos.MessageCode.Disabled_VALUE);
            }
            NodeDetailResponse nodeDetail = BeanUtil.toBean(node, NodeDetailResponse.class);
            List<Long> groupIdList = groupService.queryGroupIdListByMemberId(node.getId());
            List<Group> groupList = groupService.queryDetailList(groupIdList);
            List<Route> routeList = routeService.queryDetailByIdList(groupList.stream()
                            .flatMap(e -> e.getRouteIdList().stream()).distinct().collect(Collectors.toList()))
                    .stream().filter(e -> e.getEnable()).collect(Collectors.toList());
            List<RouteRule> routeRuleList = routeRuleService.queryByIdList(groupList.stream()
                            .flatMap(e -> e.getRouteRuleIdList().stream()).distinct().collect(Collectors.toList()))
                    .stream().filter(e -> e.getEnable()).collect(Collectors.toList());
            List<VNAT> vnatList = vnatService.queryIdList(groupList.stream()
                            .flatMap(e -> e.getVnatIdList().stream()).distinct().collect(Collectors.toList()))
                    .stream().filter(e -> e.getEnable()).collect(Collectors.toList());
            nodeDetail.setRouteList(routeList);
            nodeDetail.setRouteRuleList(routeRuleList);
            nodeDetail.setVnatList(vnatList);
            return nodeDetail;
        }
    }

    private String applyIp(Tenant tenant) {
        String vip = reuseIp();
        if (null != vip) {
            return vip;
        }
        Cidr cidr = Cidr.parseCidr(tenant.getCidr());
        Integer idx = tenant.getIpIndex();
        do {
            if (idx > cidr.getCount()) {
                throw new ProcessCodeException(SDWanProtos.MessageCode.NotEnough_VALUE);
            }
            idx = selectIdx(tenant.getId(), idx);
            vip = cidr.genIpByIdx(idx);
        } while (!cidr.isAvailableIp(vip));
        VipPoolPO vipPoolPO = new VipPoolPO();
        vipPoolPO.setVip(vip);
        vipPoolPO.setUsed(true);
        vipPoolPO.setTenantId(tenant.getId());
        vipPoolPO.insert();
        return vip;
    }

    private String reuseIp() {
        while (true) {
            VipPoolPO poolPO = new LambdaQueryChainWrapper<>(vipPoolMapper)
                    .eq(VipPoolPO::getUsed, false)
                    .last("limit 1")
                    .one();
            if (null == poolPO) {
                return null;
            }
            boolean update = new LambdaUpdateChainWrapper<>(vipPoolMapper)
                    .eq(VipPoolPO::getId, poolPO.getId())
                    .eq(VipPoolPO::getUsed, false)
                    .set(VipPoolPO::getUsed, true)
                    .update();
            if (update) {
                return poolPO.getVip();
            }
        }
    }

    private int selectIdx(Long tenantId, int idx) {
        boolean updated;
        int next;
        do {
            next = idx + 1;
            updated = tenantService.updateIpIndex(tenantId, idx, next);
            idx = next;
        } while (!updated);
        return next;
    }

    @Override
    public boolean existsNode(Long nodeId) {
        Long count = nodeRepository.query()
                .eq(Node::getId, nodeId)
                .count();
        return count > 0;
    }
}
