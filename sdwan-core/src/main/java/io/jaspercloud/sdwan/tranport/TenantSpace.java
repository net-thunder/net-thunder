package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.support.Cidr;
import io.netty.channel.Channel;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TenantSpace {

    private Cidr ipPool;
    private Map<String, AtomicReference<Channel>> bindIPMap = new ConcurrentHashMap<>();
    private Map<String, String> fixedVipMap = new ConcurrentHashMap<>();
    private List<SdWanServerConfig.Route> routeList = new ArrayList<>();
}
