package io.jaspercloud.sdwan.tranport.config;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class VNATConfig {

    private String srcCidr;
    private String dstCidr;
}