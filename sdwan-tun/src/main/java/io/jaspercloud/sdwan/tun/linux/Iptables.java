package io.jaspercloud.sdwan.tun.linux;

import cn.hutool.core.io.FileUtil;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

@Slf4j
public final class Iptables {

    private Iptables() {

    }

    public static void enableIpForward(String ethName, String tunName) throws ProcessException {
        log.info("enableIpForward");
        try {
            enableIpForward();
            //filter
            if (Iptables.queryFilterRule(tunName, ethName)) {
                Iptables.deleteFilterRule(tunName, ethName);
            }
            Iptables.addFilterRule(tunName, ethName);
            //nat
            if (Iptables.queryNatRule(ethName)) {
                Iptables.deleteNatRule(ethName);
            }
            Iptables.addNatRule(ethName);
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }

    public static void disableIpForward(String ethName, String tunName) throws ProcessException {
        log.info("disableIpForward");
        try {
            //filter
            Iptables.deleteFilterRule(tunName, ethName);
            //nat
            Iptables.deleteNatRule(ethName);
        } catch (Exception e) {
            throw new ProcessException(e.getMessage(), e);
        }
    }

    private static void enableIpForward() {
        File file = new File("/proc/sys/net/ipv4/ip_forward");
        String value = FileUtil.readString(file, Charset.forName("utf-8"));
        if (!StringUtils.equals(value, "1") && file.canWrite()) {
            FileUtil.writeBytes("1".getBytes(), file);
        }
    }

    private static void addFilterRule(String tunName, String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t filter -A FORWARD -i %s -o %s -j ACCEPT", tunName, ethName);
        log.info("addFilterRule: {}", cmd);
        int code = ProcessUtil.exec(cmd);
    }

    private static void addNatRule(String ethName) throws IOException, InterruptedException {
        String cmd = String.format("iptables -t nat -A POSTROUTING -o %s -j MASQUERADE", ethName);
        log.info("addNatRule: {}", cmd);
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
