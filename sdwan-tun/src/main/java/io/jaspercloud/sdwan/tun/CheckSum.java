package io.jaspercloud.sdwan.tun;

import io.netty.buffer.ByteBuf;

public final class CheckSum {

    private CheckSum() {

    }

    public static int calcTcpUdp(ByteBuf byteBuf) {
        byteBuf.markReaderIndex();
        try {
            int sum = 0;
            while (byteBuf.readableBytes() >= 2) {
                sum += byteBuf.readUnsignedShort();
            }
            while (byteBuf.readableBytes() > 0) {
                sum += 0;
            }
            return sum;
        } finally {
            byteBuf.resetReaderIndex();
        }
    }

    public static int calcHighLow(int sum) {
        //如果有进位，则加到低16位
        while ((sum >> 16) > 0) {
            sum = (sum & 0xffff) + (sum >> 16);
        }
        //按位取反并返回
        sum = ~sum;
        return sum;
    }

    public static int calcIp(ByteBuf byteBuf) {
        byteBuf.markReaderIndex();
        try {
            int sum = 0;
            while (byteBuf.readableBytes() >= 2) {
                sum += byteBuf.readUnsignedShort();
            }
            while (byteBuf.readableBytes() > 0) {
                sum += 0;
            }
            sum = calcHighLow(sum);
            return sum;
        } finally {
            byteBuf.resetReaderIndex();
        }
    }
}
