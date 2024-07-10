package io.jaspercloud.sdwan.tun.linux;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.util.List;

public final class Iptables {

    private Iptables() {

    }

    public static void ipForward(String fromEth, String toEth) throws ProcessException {
        try {
            enableIpForward();
            //filter
            if (Iptables.queryFilterRule(fromEth, toEth)) {
                Iptables.deleteFilterRule(fromEth, toEth);
            }
            Iptables.addFilterRule(fromEth, toEth);
            //nat
            if (Iptables.queryNatRule(toEth)) {
                Iptables.deleteNatRule(toEth);
            }
            Iptables.addNatRule(toEth);
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }

    private static void enableIpForward() {
        File file = new File("/proc/sys/net/ipv4/ip_forward");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"))) {
            FileCopyUtils.copy("1", writer);
        } catch (Throwable e) {
            throw new ProcessException("enableIpForward failed", e);
        }
    }

    private static void addFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t filter -A FORWARD -i %s -o %s -j ACCEPT", tunName, ethName);
        int code = ProcessUtil.exec(cmd);
    }

    private static void addNatRule(String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t nat -A POSTROUTING -o %s -j MASQUERADE", ethName);
        int code = ProcessUtil.exec(cmd);
    }

    private static boolean queryFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = "iptables -t filter -L FORWARD -n -v";
        List<String> list = ProcessUtil.query(cmd);
        for (String line : list) {
            if (containsKeyword(line, tunName, ethName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean queryNatRule(String ethName) throws IOException, InterruptedException {
        String cmd = "iptables -t nat -L POSTROUTING -n -v";
        List<String> list = ProcessUtil.query(cmd);
        for (String line : list) {
            if (containsKeyword(line, ethName)) {
                return true;
            }
        }
        return false;
    }

    private static void deleteFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t filter -D FORWARD -i %s -o %s -j ACCEPT", tunName, ethName);
        int code = ProcessUtil.exec(cmd);
    }

    private static void deleteNatRule(String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t nat -D POSTROUTING -o %s -j MASQUERADE", ethName);
        int code = ProcessUtil.exec(cmd);
    }

    private static boolean containsKeyword(String line, String... keywords) {
        for (String keyword : keywords) {
            if (!StringUtils.contains(line, keyword)) {
                return false;
            }
        }
        return true;
    }
}
