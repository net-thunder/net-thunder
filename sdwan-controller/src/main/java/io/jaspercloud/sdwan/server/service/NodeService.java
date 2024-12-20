package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.server.controller.request.EditNodeRequest;
import io.jaspercloud.sdwan.server.controller.request.NodeQueryRequest;
import io.jaspercloud.sdwan.server.controller.response.NodeDetailResponse;
import io.jaspercloud.sdwan.server.controller.response.NodeResponse;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.entity.Node;

import java.util.List;

public interface NodeService {

    void add(EditNodeRequest request);

    void edit(EditNodeRequest request);

    void del(EditNodeRequest request);

    List<NodeResponse> list(NodeQueryRequest request);

    PageResponse<NodeResponse> page();

    Node queryById(Long id);

    NodeDetailResponse queryDetail(Long id);

    List<Node> queryByIdList(List<Long> idList);

    List<Node> queryByTenantId(Long tenantId);

    NodeDetailResponse assignNodeInfo(Long tenantId, SDWanProtos.RegistReq registReq);

    boolean existsNode(Long nodeId);
}
