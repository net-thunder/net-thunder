package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.support.Cidr;
import org.junit.jupiter.api.Test;

public class CidrTest {

    @Test
    public void transform() {
        Cidr cidr1 = Cidr.parseCidr("172.168.0.0/24");
        Cidr cidr2 = Cidr.parseCidr("192.222.0.0/24");
        String ip = Cidr.transform("172.168.0.63", cidr1, cidr2);
        System.out.println(ip);
    }
}
