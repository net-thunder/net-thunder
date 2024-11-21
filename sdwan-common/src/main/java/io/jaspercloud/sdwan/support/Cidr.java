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
    private long count;

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
        long address = IPUtil.ip2long(split[0]);
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
        long count = calcCount(maskBits);
        cidr.setCount(count);
        return cidr;
    }

    public List<String> allIpList() {
        long address = parseIdentifierAddress(IPUtil.ip2long(networkIdentifier), maskBits);
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
        long address = IPUtil.ip2long(vip);
        address = parseIdentifierAddress(address, maskBits);
        String identifier = IPUtil.int2ip(address);
        String result = String.format("%s/%d", identifier, maskBits);
        return result;
    }

    private static List<String> parseIpList(long address, int maskBits) {
        address = parseIdentifierAddress(address, maskBits);
        long count = calcCount(maskBits);
        List<String> list = new ArrayList<>();
        long s = address;
        for (int n = 0; n < count; n++) {
            String ip = IPUtil.int2ip(s);
            list.add(ip);
            s += 1;
        }
        return list;
    }

    private static long parseIdentifierAddress(long address, int maskBits) {
        address = (address >> (32 - maskBits)) << (32 - maskBits);
        return address;
    }

    private static String parseIp(long address, int maskBits, int index) {
        address = parseIdentifierAddress(address, maskBits);
        String ip = IPUtil.int2ip(address + index);
        return ip;
    }

    private static String parseNetworkIdentifier(long address, int maskBits) {
        address = parseIdentifierAddress(address, maskBits);
        String ip = IPUtil.int2ip(address);
        return ip;
    }

    private static String parseBroadcastAddress(long address, int maskBits) {
        address = parseIdentifierAddress(address, maskBits);
        long count = calcCount(maskBits);
        long s = calcMaxAddr(address, count);
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
        long address = IPUtil.ip2long(split[0]);
        int maskBits = Integer.parseInt(split[1]);
        String broadcastAddress = parseBroadcastAddress(address, maskBits);
        boolean equals = StringUtils.equals(ip, broadcastAddress);
        return equals;
    }

    public static boolean contains(String cidr, String ip) {
        check(cidr);
        String[] split = cidr.split("/");
        long address = IPUtil.ip2long(split[0]);
        int maskBits = Integer.parseInt(split[1]);
        address = parseIdentifierAddress(address, maskBits);
        long count = calcCount(maskBits);
        long maxAddr = calcMaxAddr(address, count);
        long checkAddr = IPUtil.ip2long(ip);
        boolean contains = checkAddr >= address && checkAddr <= maxAddr;
        return contains;
    }

    public boolean contains(String ip) {
        long address = parseIdentifierAddress(IPUtil.ip2long(networkIdentifier), maskBits);
        long count = calcCount(maskBits);
        long maxAddr = calcMaxAddr(address, count);
        long checkAddr = IPUtil.ip2long(ip);
        boolean contains = checkAddr >= address && checkAddr <= maxAddr;
        return contains;
    }

    private long getIdx(String ip) {
        long address = parseIdentifierAddress(IPUtil.ip2long(networkIdentifier), maskBits);
        long checkAddr = IPUtil.ip2long(ip);
        return checkAddr - address;
    }

    public String genIpByIdx(long idx) {
        long address = parseIdentifierAddress(IPUtil.ip2long(networkIdentifier), maskBits);
        String ip = IPUtil.int2ip(address + idx);
        return ip;
    }

    public boolean isAvailableIp(String ip) {
        if (StringUtils.equals(networkIdentifier, ip)) {
            return false;
        }
        if (StringUtils.equals(maskAddress, ip)) {
            return false;
        }
        if (StringUtils.equals(broadcastAddress, ip)) {
            return false;
        }
        if (StringUtils.equals(gatewayAddress, ip)) {
            return false;
        }
        return true;
    }

    public static String transform(String ip, Cidr src, Cidr target) {
        if (!src.contains(ip)) {
            throw new ProcessException("ip contains failed");
        }
        if (!Objects.equals(src.getMaskBits(), target.getMaskBits())) {
            throw new ProcessException("maskBits equals failed");
        }
        long idx = src.getIdx(ip);
        String result = target.genIpByIdx(idx);
        return result;
    }

    private static long calcCount(int maskBits) {
        long count = (long) Math.pow(2, 32 - maskBits);
        return count;
    }

    private static long calcMaxAddr(long address, long count) {
        //address占一个
        return address + count - 1;
    }

    @Override
    public String toString() {
        return String.format("%s/%d", networkIdentifier, maskBits);
    }
}
