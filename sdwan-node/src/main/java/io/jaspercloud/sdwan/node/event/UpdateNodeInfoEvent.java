package io.jaspercloud.sdwan.node.event;

import io.jaspercloud.sdwan.support.AddressUri;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateNodeInfoEvent {

    private List<AddressUri> p2pAddressList;
    private List<AddressUri> relayAddressList;
}
