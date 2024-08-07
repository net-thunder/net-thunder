package io.jaspercloud.sdwan.support;

import io.jaspercloud.sdwan.exception.CidrParseException;
import io.jaspercloud.sdwan.util.IPUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import sun.net.util.IPAddressUtil;

import java.util.ArrayList;
import java.util.List;

@Data
public class Cidr {

    private List<String> ipList;
    private List<String> availableIpList;
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
        if (!IPAddressUtil.isIPv4LiteralAddress(address)) {
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
        List<String> ipList = parseIpList(address, maskBits);
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
        cidr.setIpList(ipList);
        List<String> availableIpList = new ArrayList<>(ipList);
        availableIpList.remove(networkIdentifier);
        availableIpList.remove(maskAddress);
        availableIpList.remove(broadcastAddress);
        availableIpList.remove(gatewayAddress);
        cidr.setAvailableIpList(availableIpList);
        return cidr;
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

    @Override
    public String toString() {
        return String.format("%s/%d", networkIdentifier, maskBits);
    }
}
