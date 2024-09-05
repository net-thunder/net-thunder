package io.jaspercloud.sdwan.util;

import org.apache.commons.lang3.StringUtils;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class NetworkInterfaceUtil {

    public static String getHardwareAddress(String ip) throws SocketException {
        Map<String, NetworkInterfaceInfo> map = findUpIpv4NetworkInterfaceInfo()
                .stream().collect(Collectors.toMap(e -> e.getIp(), e -> e));
        NetworkInterfaceInfo interfaceInfo = map.get(ip);
        if (null == interfaceInfo) {
            return null;
        }
        String hardwareAddress = interfaceInfo.getHardwareAddress();
        return hardwareAddress;
    }

    public static List<NetworkInterfaceInfo> parseInetAddress(InetAddress[] inetAddresses) throws SocketException {
        Map<String, NetworkInterfaceInfo> map = findUpIpv4NetworkInterfaceInfo()
                .stream().collect(Collectors.toMap(e -> e.getIp(), e -> e));
        List<NetworkInterfaceInfo> list = new ArrayList<>();
        for (InetAddress address : inetAddresses) {
            NetworkInterfaceInfo interfaceInfo = map.get(address.getHostAddress());
            if (null == interfaceInfo) {
                continue;
            }
            list.add(interfaceInfo);
        }
        return list;
    }

    public static List<NetworkInterfaceInfo> findUpIpv4NetworkInterfaceInfo() throws SocketException {
        return findIpv4NetworkInterfaceInfo(true);
    }

    public static List<NetworkInterfaceInfo> findIpv4NetworkInterfaceInfo(boolean findActive) throws SocketException {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        List<NetworkInterfaceInfo> list = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            if (findActive && !networkInterface.isUp()) {
                continue;
            }
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (!IPUtil.isIPv4(interfaceAddress.getAddress().getHostAddress())) {
                    continue;
                }
                NetworkInterfaceInfo networkInterfaceInfo = new NetworkInterfaceInfo();
                networkInterfaceInfo.setEthName(networkInterface.getName());
                networkInterfaceInfo.setIndex(networkInterface.getIndex());
                networkInterfaceInfo.setInterfaceAddress(interfaceAddress);
                networkInterfaceInfo.setIp(interfaceAddress.getAddress().getHostAddress());
                if (null != networkInterface.getHardwareAddress()) {
                    String hardwareAddress = parseHardwareAddress(networkInterface.getHardwareAddress());
                    networkInterfaceInfo.setHardwareAddress(hardwareAddress);
                }
                list.add(networkInterfaceInfo);
            }
        }
        return list;
    }

    public static NetworkInterfaceInfo findIp(String ip) throws SocketException {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            if (!networkInterface.isUp()) {
                continue;
            }
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (!IPUtil.isIPv4(interfaceAddress.getAddress().getHostAddress())) {
                    continue;
                }
                if (StringUtils.equals(interfaceAddress.getAddress().getHostAddress(), ip)) {
                    NetworkInterfaceInfo networkInterfaceInfo = new NetworkInterfaceInfo();
                    networkInterfaceInfo.setEthName(networkInterface.getName());
                    networkInterfaceInfo.setIndex(networkInterface.getIndex());
                    networkInterfaceInfo.setInterfaceAddress(interfaceAddress);
                    networkInterfaceInfo.setIp(interfaceAddress.getAddress().getHostAddress());
                    if (null != networkInterface.getHardwareAddress()) {
                        String hardwareAddress = parseHardwareAddress(networkInterface.getHardwareAddress());
                        networkInterfaceInfo.setHardwareAddress(hardwareAddress);
                    }
                    return networkInterfaceInfo;
                }
            }
        }
        return null;
    }

    public static NetworkInterfaceInfo findEth(String eth) throws SocketException {
        Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces();
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            if (!networkInterface.isUp()) {
                continue;
            }
            for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                if (!IPUtil.isIPv4(interfaceAddress.getAddress().getHostAddress())) {
                    continue;
                }
                if (StringUtils.equals(networkInterface.getName(), eth)) {
                    NetworkInterfaceInfo networkInterfaceInfo = new NetworkInterfaceInfo();
                    networkInterfaceInfo.setEthName(networkInterface.getName());
                    networkInterfaceInfo.setIndex(networkInterface.getIndex());
                    networkInterfaceInfo.setInterfaceAddress(interfaceAddress);
                    if (null != networkInterface.getHardwareAddress()) {
                        String hardwareAddress = parseHardwareAddress(networkInterface.getHardwareAddress());
                        networkInterfaceInfo.setHardwareAddress(hardwareAddress);
                    }
                    return networkInterfaceInfo;
                }
            }
        }
        return null;
    }

    private static String parseHardwareAddress(byte[] hardwareAddress) {
        StringBuilder builder = new StringBuilder();
        for (byte b : hardwareAddress) {
            builder.append(String.format("%02x", b)).append(":");
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        String address = builder.toString();
        return address;
    }
}
