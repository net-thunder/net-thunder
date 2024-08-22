package io.jaspercloud.sdwan.tranport;

import lombok.*;

/**
 * @author jasper
 * @create 2024/7/12
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TunTransportConfig {

    private String tunName;
    private String ip;
    private int maskBits;
    private int mtu;
    private String localAddress;
    private Boolean icsEnable;
}
