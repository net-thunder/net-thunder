package io.jaspercloud.sdwan.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Node;
import io.jaspercloud.sdwan.server.entity.NodeRoute;
import io.jaspercloud.sdwan.server.entity.NodeRouteRule;
import io.jaspercloud.sdwan.server.entity.NodeVNAT;
import io.jaspercloud.sdwan.server.repository.NodeRepository;
import io.jaspercloud.sdwan.server.repository.NodeRouteRepository;
import io.jaspercloud.sdwan.server.repository.NodeRouteRuleRepository;
import io.jaspercloud.sdwan.server.repository.NodeVNATRepository;
import io.jaspercloud.sdwan.server.repository.po.NodePO;
import io.jaspercloud.sdwan.server.repository.po.NodeRoutePO;
import io.jaspercloud.sdwan.server.repository.po.NodeRouteRulePO;
import io.jaspercloud.sdwan.server.repository.po.NodeVNATPO;
import io.jaspercloud.sdwan.server.service.NodeService;
import io.jaspercloud.sdwan.server.service.RouteService;
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
    private RouteService routeService;

    @Resource
    private NodeRepository nodeRepository;

    @Resource
    private NodeRouteRepository nodeRouteRepository;

    @Resource
    private NodeRouteRuleRepository nodeRouteRuleRepository;

    @Resource
    private NodeVNATRepository nodeVNATRepository;

    @Override
    public void add(EditNodeRequest request) {
        NodePO node = BeanUtil.toBean(request, NodePO.class);
        node.setId(null);
        node.insert();
        if (CollectionUtil.isNotEmpty(request.getRouteIdList())) {
            request.getRouteIdList().forEach(e -> {
                NodeRoutePO nodeRoutePO = new NodeRoutePO();
                nodeRoutePO.setNodeId(node.getId());
                nodeRoutePO.setRouteId(e);
                nodeRoutePO.insert();
            });
        }
        if (CollectionUtil.isNotEmpty(request.getRouteRuleIdList())) {
            request.getRouteRuleIdList().forEach(e -> {
                NodeRouteRulePO nodeRouteRulePO = new NodeRouteRulePO();
                nodeRouteRulePO.setNodeId(node.getId());
                nodeRouteRulePO.setRuleId(e);
                nodeRouteRulePO.insert();
            });
        }
        if (CollectionUtil.isNotEmpty(request.getVnatIdList())) {
            request.getVnatIdList().forEach(e -> {
                NodeVNATPO nodeVNATPO = new NodeVNATPO();
                nodeVNATPO.setNodeId(node.getId());
                nodeVNATPO.setVnatId(e);
                nodeVNATPO.insert();
            });
        }
    }

    @Override
    public void edit(EditNodeRequest request) {
        NodePO node = BeanUtil.toBean(request, NodePO.class);
        node.updateById();
        if (CollectionUtil.isNotEmpty(request.getRouteIdList())) {
            nodeRouteRepository.delete(nodeRouteRepository.lambdaQuery()
                    .eq(NodeRoutePO::getNodeId, node.getId()));
            request.getRouteIdList().forEach(e -> {
                NodeRoutePO nodeRoutePO = new NodeRoutePO();
                nodeRoutePO.setNodeId(node.getId());
                nodeRoutePO.setRouteId(e);
                nodeRoutePO.insert();
            });
        }
        if (CollectionUtil.isNotEmpty(request.getRouteRuleIdList())) {
            nodeRouteRuleRepository.delete(nodeRouteRuleRepository.lambdaQuery()
                    .eq(NodeRouteRulePO::getNodeId, node.getId()));
            request.getRouteRuleIdList().forEach(e -> {
                NodeRouteRulePO nodeRouteRulePO = new NodeRouteRulePO();
                nodeRouteRulePO.setNodeId(node.getId());
                nodeRouteRulePO.setRuleId(e);
                nodeRouteRulePO.insert();
            });
        }
        if (CollectionUtil.isNotEmpty(request.getVnatIdList())) {
            nodeVNATRepository.delete(nodeVNATRepository.lambdaQuery()
                    .eq(NodeVNATPO::getNodeId, node.getId()));
            request.getVnatIdList().forEach(e -> {
                NodeVNATPO nodeVNATPO = new NodeVNATPO();
                nodeVNATPO.setNodeId(node.getId());
                nodeVNATPO.setVnatId(e);
                nodeVNATPO.insert();
            });
        }
    }

    @Override
    public void del(EditNodeRequest request) {
        if (routeService.usedNode(request.getId())) {
            throw new ProcessException("route used");
        }
        nodeRouteRepository.delete(nodeRouteRepository.lambdaQuery()
                .eq(NodeRoutePO::getNodeId, request.getId()));
        nodeRouteRuleRepository.delete(nodeRouteRuleRepository.lambdaQuery()
                .eq(NodeRouteRulePO::getNodeId, request.getId()));
        nodeVNATRepository.delete(nodeVNATRepository.lambdaQuery()
                .eq(NodeVNATPO::getNodeId, request.getId()));
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
        List<NodeRoute> nodeRouteList = nodeRouteRepository.list(nodeRouteRepository.lambdaQuery()
                .eq(NodeRoutePO::getNodeId, node.getId()));
        List<NodeRouteRule> nodeRouteRuleList = nodeRouteRuleRepository.list(nodeRouteRuleRepository.lambdaQuery()
                .eq(NodeRouteRulePO::getNodeId, node.getId()));
        List<NodeVNAT> nodeVNATList = nodeVNATRepository.list(nodeVNATRepository.lambdaQuery()
                .eq(NodeVNATPO::getNodeId, node.getId()));
        node.setNodeRouteList(nodeRouteList);
        node.setNodeRouteRuleList(nodeRouteRuleList);
        node.setNodeVNATList(nodeVNATList);
        return node;
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
        //todo
        return null;
    }
}
