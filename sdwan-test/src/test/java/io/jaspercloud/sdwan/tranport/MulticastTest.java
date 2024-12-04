package io.jaspercloud.sdwan.tranport;

import cn.hutool.core.lang.Assert;
import io.jaspercloud.sdwan.support.Multicast;
import org.junit.jupiter.api.Test;

public class MulticastTest {

    @Test
    public void test() {
        Assert.isTrue(Multicast.isMulticastIp("255.255.255.255"));
        Assert.isTrue(Multicast.isMulticastIp("224.0.0.0"));
        Assert.isTrue(Multicast.isMulticastIp("239.255.255.255"));
    }
}
