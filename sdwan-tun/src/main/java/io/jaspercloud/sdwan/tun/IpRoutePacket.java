package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.support.Referenced;
import io.jaspercloud.sdwan.util.IPUtil;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

public class IpRoutePacket implements Referenced {

    private ByteBuf byteBuf;

    public IpRoutePacket(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public void setSrcIp(String ip) {
        writeIp(12, ip);
    }

    public void setDstIp(String ip) {
        writeIp(16, ip);
    }

    public String getSrcIp() {
        return readIp(12);
    }

    public String getDstIp() {
        return readIp(16);
    }

    private String readIp(int index) {
        byteBuf.markReaderIndex();
        try {
            byteBuf.readerIndex(index);
            byte[] bytes = new byte[4];
            byteBuf.readBytes(bytes);
            return IPUtil.bytes2ip(bytes);
        } finally {
            byteBuf.resetReaderIndex();
        }
    }

    private void writeIp(int index, String ip) {
        byteBuf.markWriterIndex();
        try {
            byteBuf.writerIndex(index);
            byteBuf.writeBytes(IPUtil.ip2bytes(ip));
        } finally {
            byteBuf.resetWriterIndex();
        }
    }

    public void rebuild() {
        byteBuf.markReaderIndex();
        byteBuf.readerIndex(0);
        short head = byteBuf.readUnsignedByte();
        byte headLen = (byte) ((head & 0b00001111) * 4);
        byteBuf.readerIndex(2);
        int totalLen = byteBuf.readUnsignedShort();
        byteBuf.readerIndex(9);
        int protocol = byteBuf.readUnsignedByte();
        byteBuf.readerIndex(headLen);
        ByteBuf payload = byteBuf.readSlice(totalLen - headLen);
        byteBuf.resetReaderIndex();
        switch (protocol) {
            case Ipv4Packet.Tcp: {
                String srcIp = getSrcIp();
                String dstIp = getDstIp();
                reCalcTcpCheckSum(payload, protocol, srcIp, dstIp);
                break;
            }
            case Ipv4Packet.Udp: {
                String srcIp = getSrcIp();
                String dstIp = getDstIp();
                reCalcUdpCheckSum(payload, protocol, srcIp, dstIp);
                break;
            }
        }
        reCalcIpCheckSum();
    }

    private void reCalcTcpCheckSum(ByteBuf payload, int protocol, String srcIp, String dstIp) {
        ByteBuf buffer = payload.alloc().buffer(12 + payload.readableBytes() + 1);
        try {
            //ipHeader
            buffer.writeBytes(IPUtil.ip2bytes(srcIp));
            buffer.writeBytes(IPUtil.ip2bytes(dstIp));
            buffer.writeByte(0);
            buffer.writeByte(protocol);
            buffer.writeShort(payload.readableBytes());
            //reset checksum
            payload.markWriterIndex();
            payload.writerIndex(16);
            payload.writeShort(0);
            payload.resetWriterIndex();
            //tcp
            payload.markReaderIndex();
            buffer.writeBytes(payload);
            payload.resetReaderIndex();
            //calc
            if (0 != buffer.readableBytes() % 2) {
                //数据长度为奇数，在该字节之后补一个字节
                buffer.writeByte(0);
            }
            int sum = 0;
            while (buffer.readableBytes() > 0) {
                sum += buffer.readUnsignedShort();
            }
            int h = sum >> 16;
            int l = sum & 0b11111111_11111111;
            sum = (h + l);
            sum = 0b11111111_11111111 & ~sum;
            //set checksum
            payload.markWriterIndex();
            payload.writerIndex(16);
            payload.writeShort(sum);
        } finally {
            buffer.release();
        }
    }

    private void reCalcUdpCheckSum(ByteBuf payload, int protocol, String srcIp, String dstIp) {
        ByteBuf buffer = payload.alloc().buffer(12 + payload.readableBytes() + 1);
        try {
            //ipHeader
            buffer.writeBytes(IPUtil.ip2bytes(srcIp));
            buffer.writeBytes(IPUtil.ip2bytes(dstIp));
            buffer.writeByte(0);
            buffer.writeByte(protocol);
            buffer.writeShort(payload.readableBytes());
            //reset checksum
            payload.markWriterIndex();
            payload.writerIndex(6);
            payload.writeShort(0);
            payload.resetWriterIndex();
            //udp
            payload.markReaderIndex();
            buffer.writeBytes(payload);
            payload.resetReaderIndex();
            //calc
            if (0 != buffer.readableBytes() % 2) {
                //数据长度为奇数，在该字节之后补一个字节
                buffer.writeByte(0);
            }
            int sum = 0;
            while (buffer.readableBytes() > 0) {
                sum += buffer.readUnsignedShort();
            }
            int h = sum >> 16;
            int l = sum & 0b11111111_11111111;
            sum = (h + l);
            sum = 0b11111111_11111111 & ~sum;
            //set checksum
            payload.markWriterIndex();
            payload.writerIndex(6);
            payload.writeShort(sum);
        } finally {
            buffer.release();
        }
    }

    private void reCalcIpCheckSum() {
        byteBuf.markReaderIndex();
        ByteBuf head = byteBuf.readSlice(20);
        byteBuf.resetReaderIndex();
        //reset checksum
        head.markWriterIndex();
        head.writerIndex(10);
        head.writeShort(0);
        head.resetWriterIndex();
        //calc
        head.markReaderIndex();
        int sum = 0;
        while (head.readableBytes() > 0) {
            sum += head.readUnsignedShort();
        }
        int h = sum >> 16;
        int l = sum & 0b11111111_11111111;
        sum = (h + l);
        sum = 0b11111111_11111111 & ~sum;
        head.resetReaderIndex();
        //set checksum
        head.markWriterIndex();
        head.writerIndex(10);
        head.writeShort(sum);
        head.resetWriterIndex();
    }

    @Override
    public int refCnt() {
        return byteBuf.refCnt();
    }

    @Override
    public ReferenceCounted retain(int increment) {
        return byteBuf.retain(increment);
    }

    @Override
    public boolean release(int decrement) {
        return byteBuf.release(decrement);
    }
}
