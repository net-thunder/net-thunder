package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.*;
import io.jaspercloud.sdwan.server.repsitory.NodeMapper;
import io.jaspercloud.sdwan.server.repsitory.NodeRouteMapper;
import io.jaspercloud.sdwan.server.repsitory.NodeRouteRuleMapper;
import io.jaspercloud.sdwan.server.repsitory.NodeVNATMapper;
import io.jaspercloud.sdwan.server.service.*;
import jakarta.annotation.Resource;
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
    private NodeMapper nodeMapper;

    @Resource
    private NodeRouteMapper nodeRouteMapper;

    @Resource
    private NodeRouteRuleMapper nodeRouteRuleMapper;

    @Resource
    private NodeVNATMapper nodeVNATMapper;

    @Override
    public void add(EditNodeRequest request) {
        Node node = BeanUtil.toBean(request, Node.class);
        node.setId(null);
        node.insert();
    }

    @Override
    public void edit(EditNodeRequest request) {
        Node node = BeanUtil.toBean(request, Node.class);
        node.updateById();
    }

    @Override
    public void del(EditNodeRequest request) {
        if (routeService.usedNode(request.getId())) {
            throw new ProcessException("route used");
        }
        nodeMapper.deleteById(request.getId());
    }

    @Override
    public PageResponse<NodeResponse> page() {
        Long total = new LambdaQueryChainWrapper<>(nodeMapper)
                .count();
        List<Node> list = new LambdaQueryChainWrapper<>(nodeMapper)
                .list();
        List<NodeResponse> collect = list.stream().map(e -> {
            NodeResponse nodeResponse = BeanUtil.toBean(e, NodeResponse.class);
            return nodeResponse;
        }).collect(Collectors.toList());
        PageResponse<NodeResponse> response = PageResponse.build(collect, total, 0L, 0L);
        return response;
    }

    @Override
    public NodeDetailResponse detail(Long id) {
        Node node = nodeMapper.selectById(id);
        if (null == node) {
            return null;
        }
        NodeDetailResponse nodeResponse = BeanUtil.toBean(node, NodeDetailResponse.class);
        List<Route> routeList = routeService.queryByIdList(new LambdaQueryChainWrapper<>(nodeRouteMapper)
                .eq(NodeRoute::getNodeId, node.getId())
                .list()
                .stream().map(e -> e.getRouteId()).collect(Collectors.toList()));
        nodeResponse.setRouteList(routeList);
        List<RouteRule> routeRuleList = routeRuleService.queryByIdList(new LambdaQueryChainWrapper<>(nodeRouteRuleMapper)
                .eq(NodeRouteRule::getNodeId, node.getId())
                .list()
                .stream().map(e -> e.getRuleId()).collect(Collectors.toList()));
        nodeResponse.setRouteRuleList(routeRuleList);
        List<VNAT> vnatList = vnatService.queryIdList(new LambdaQueryChainWrapper<>(nodeVNATMapper)
                .eq(NodeVNAT::getNodeId, node.getId())
                .list()
                .stream().map(e -> e.getVnatId()).collect(Collectors.toList()));
        nodeResponse.setVNATList(vnatList);
        List<Group> groupList = groupService.queryByMemberId(id);
        nodeResponse.setGroupList(groupList);
        return nodeResponse;
    }

    @Override
    public List<Node> queryByIdList(List<Long> idList) {
        if (CollectionUtil.isEmpty(idList)) {
            return Collections.emptyList();
        }
        List<Node> nodeList = nodeMapper.selectByIds(idList);
        return nodeList;
    }

    @Override
    public List<Node> queryByTenantId(Long tenantId) {
        List<Node> list = new LambdaQueryChainWrapper<>(nodeMapper)
                .eq(Node::getTenantId, tenantId)
                .list();
        return list;
    }

    @Override
    public boolean usedRoute(Long routeId) {
        Long count = new LambdaQueryChainWrapper<>(nodeRouteMapper)
                .eq(NodeRoute::getRouteId, routeId)
                .count();
        return count > 0;
    }

    @Override
    public boolean usedRouteRule(Long routeRuleId) {
        Long count = new LambdaQueryChainWrapper<>(nodeRouteRuleMapper)
                .eq(NodeRouteRule::getRuleId, routeRuleId)
                .count();
        return count > 0;
    }

    @Override
    public boolean usedVNAT(Long vnatId) {
        Long count = new LambdaQueryChainWrapper<>(nodeVNATMapper)
                .eq(NodeVNAT::getVnatId, vnatId)
                .count();
        return count > 0;
    }
}
