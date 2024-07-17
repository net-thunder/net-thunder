package io.jaspercloud.sdwan.util;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author jasper
 * @create 2024/7/2
 */
public final class SocketAddressUtil {

    private SocketAddressUtil() {
    }

    public static InetSocketAddress parse(String address) {
        String[] split = address.split("\\:");
        String addr = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(addr, port);
    }

    public static String toAddress(SocketAddress address) {
        String text = address.toString().replaceAll("^/", "");
        return text;
    }

    public static String toAddress(String host, int port) {
        String text = String.format("%s:%d", host, port);
        return text;
    }
}
