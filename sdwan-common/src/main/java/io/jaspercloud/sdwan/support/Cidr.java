package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.exception.CidrParseException;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.util.IPUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class Cidr {

    private String networkIdentifier;
    private String maskAddress;
    private String broadcastAddress;
    private String gatewayAddress;
    private int maskBits;

    private Cidr() {
    }

    public static void check(String text) {
        String[] split = text.split("/");
        String address = split[0];
        if (!IPUtil.isIPv4(address)) {
            throw new CidrParseException("address error: " + address);
        }
        int maskBits = Integer.parseInt(split[1]);
        if (maskBits < 1 || maskBits > 32) {
            throw new CidrParseException("mask error: " + maskBits);
        }
    }

    public static Cidr parseCidr(String text) {
        String[] split = text.split("/");
        int address = IPUtil.ip2int(split[0]);
        int maskBits = Integer.parseInt(split[1]);
        address = parseIdentifierAddress(address, maskBits);
        String maskAddress = parseMaskAddress(maskBits);
        String networkIdentifier = parseNetworkIdentifier(address, maskBits);
        String broadcastAddress = parseBroadcastAddress(address, maskBits);
        String gatewayAddress = parseIp(address, maskBits, +1);
        Cidr cidr = new Cidr();
        cidr.setNetworkIdentifier(networkIdentifier);
        cidr.setMaskBits(maskBits);
        cidr.setMaskAddress(maskAddress);
        cidr.setBroadcastAddress(broadcastAddress);
        cidr.setGatewayAddress(gatewayAddress);
        return cidr;
    }

    public List<String> allIpList() {
        int address = parseIdentifierAddress(IPUtil.ip2int(networkIdentifier), maskBits);
        List<String> ipList = parseIpList(address, maskBits);
        return ipList;
    }

    public List<String> availableIpList() {
        List<String> availableIpList = new ArrayList<>(allIpList());
        availableIpList.remove(networkIdentifier);
        availableIpList.remove(maskAddress);
        availableIpList.remove(broadcastAddress);
        availableIpList.remove(gatewayAddress);
        return availableIpList;
    }

    public static String parseCidr(String vip, int maskBits) {
        int address = IPUtil.ip2int(vip);
        address = parseIdentifierAddress(address, maskBits);
        String identifier = IPUtil.int2ip(address);
        String result = String.format("%s/%d", identifier, maskBits);
        return result;
    }

    private static List<String> parseIpList(int address, int maskBits) {
        address = parseIdentifierAddress(address, maskBits);
        int count = (int) Math.pow(2, 32 - maskBits) - 1;
        List<String> list = new ArrayList<>();
        int s = address;
        for (int n = 0; n <= count; n++) {
            String ip = IPUtil.int2ip(s);
            list.add(ip);
            s += 1;
        }
        return list;
    }

    private static int parseIdentifierAddress(int address, int maskBits) {
        address = (address >> (32 - maskBits)) << (32 - maskBits);
        return address;
    }

    private static String parseIp(int address, int maskBits, int index) {
        address = parseIdentifierAddress(address, maskBits);
        String ip = IPUtil.int2ip(address + index);
        return ip;
    }

    private static String parseNetworkIdentifier(int address, int maskBits) {
        address = parseIdentifierAddress(address, maskBits);
        String ip = IPUtil.int2ip(address);
        return ip;
    }

    private static String parseBroadcastAddress(int address, int maskBits) {
        address = parseIdentifierAddress(address, maskBits);
        int count = (int) Math.pow(2, 32 - maskBits) - 1;
        int s = address + count;
        String ip = IPUtil.int2ip(s);
        return ip;
    }

    private static String parseMaskAddress(int maskBits) {
        int mask = Integer.MAX_VALUE << (32 - maskBits);
        String maskAddr = IPUtil.int2ip(mask);
        return maskAddr;
    }

    public static boolean isBroadcastAddress(String cidr, String ip) {
        check(cidr);
        String[] split = cidr.split("/");
        int address = IPUtil.ip2int(split[0]);
        int maskBits = Integer.parseInt(split[1]);
        String broadcastAddress = parseBroadcastAddress(address, maskBits);
        boolean equals = StringUtils.equals(ip, broadcastAddress);
        return equals;
    }

    public static boolean contains(String cidr, String ip) {
        check(cidr);
        String[] split = cidr.split("/");
        int address = IPUtil.ip2int(split[0]);
        int maskBits = Integer.parseInt(split[1]);
        address = parseIdentifierAddress(address, maskBits);
        int count = (int) Math.pow(2, 32 - maskBits) - 1;
        int maxAddr = address + count;
        int checkAddr = IPUtil.ip2int(ip);
        boolean contains = checkAddr >= address && checkAddr <= maxAddr;
        return contains;
    }

    private boolean contains(String ip) {
        int address = parseIdentifierAddress(IPUtil.ip2int(networkIdentifier), maskBits);
        int count = (int) Math.pow(2, 32 - maskBits) - 1;
        int maxAddr = address + count;
        int checkAddr = IPUtil.ip2int(ip);
        boolean contains = checkAddr >= address && checkAddr <= maxAddr;
        return contains;
    }

    private int getIdx(String ip) {
        int address = parseIdentifierAddress(IPUtil.ip2int(networkIdentifier), maskBits);
        int checkAddr = IPUtil.ip2int(ip);
        return checkAddr - address;
    }

    private String genIpByIdx(int idx) {
        int address = parseIdentifierAddress(IPUtil.ip2int(networkIdentifier), maskBits);
        String ip = IPUtil.int2ip(address + idx);
        return ip;
    }

    public static String transform(String ip, Cidr src, Cidr target) {
        if (!src.contains(ip)) {
            throw new ProcessException("ip contains failed");
        }
        if (!Objects.equals(src.getMaskBits(), target.getMaskBits())) {
            throw new ProcessException("maskBits equals failed");
        }
        int idx = src.getIdx(ip);
        String result = target.genIpByIdx(idx);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s/%d", networkIdentifier, maskBits);
    }
}
