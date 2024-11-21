package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.support.Cidr;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CidrTest {

    @Test
    public void contains() {
        {
            Cidr cidr = Cidr.parseCidr("0.0.0.0/0");
            boolean contains = cidr.contains("192.222.0.1");
            String networkIdentifier = cidr.getNetworkIdentifier();
            String gatewayAddress = cidr.getGatewayAddress();
            String broadcastAddress = cidr.getBroadcastAddress();
            System.out.println();
        }
        {
            Cidr cidr = Cidr.parseCidr("15.5.0.2/32");
            boolean contains1 = cidr.contains("15.5.0.2");
            boolean contains2 = cidr.contains("15.5.0.3");
            System.out.println();
        }
    }

    @Test
    public void transform() {
        Cidr cidr = Cidr.parseCidr("10.5.0.0/16");
        cidr.allIpList();
        Cidr cidr1 = Cidr.parseCidr("172.168.0.0/24");
        Cidr cidr2 = Cidr.parseCidr("192.222.0.0/24");
        String ip = Cidr.transform("172.168.0.63", cidr1, cidr2);
        System.out.println(ip);
    }

    @Test
    public void genVip() {
        AtomicInteger gen = new AtomicInteger();
        Cidr cidr = Cidr.parseCidr("192.168.1.0/24");
        long total = cidr.getCount();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            int ct = 0;
            String vip;
            do {
                do {
                    if (ct >= total) {
                        System.out.println();
                    }
                    Integer idx = gen.getAndIncrement();
                    if (idx >= total) {
                        gen.set(0);
                        idx = gen.getAndIncrement();
                    }
                    vip = cidr.genIpByIdx(idx);
                    ct++;
                } while (!cidr.isAvailableIp(vip));
            } while (list.contains(vip));
            list.add(vip);
        }
        System.out.println();
    }
}
