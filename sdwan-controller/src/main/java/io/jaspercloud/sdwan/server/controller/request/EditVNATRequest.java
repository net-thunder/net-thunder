package io.jaspercloud.sdwan.server.controller.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditVNATRequest {

    private Long id;
    private String name;
    private String description;
    private String srcCidr;
    private String dstCidr;
    private List<Long> groupIdList;
    private Boolean enable;
}
