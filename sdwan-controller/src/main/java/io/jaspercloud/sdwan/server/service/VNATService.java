package io.jaspercloud.sdwan.server.service;

import io.jaspercloud.sdwan.server.controller.request.EditVNATRequest;
import io.jaspercloud.sdwan.server.controller.response.PageResponse;
import io.jaspercloud.sdwan.server.controller.response.VNATResponse;
import io.jaspercloud.sdwan.server.entity.VNAT;

import java.util.List;

public interface VNATService {

    void add(EditVNATRequest request);

    void edit(EditVNATRequest request);

    void del(EditVNATRequest request);

    PageResponse<VNATResponse> page();

    VNAT queryId(Long id);

    List<VNAT> queryIdList(List<Long> idList);
}