package io.jaspercloud.sdwan.tranport.config;

import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VNATConfig {

    private String name;
    private String srcCidr;
    private String dstCidr;
    private List<String> vipList;
}