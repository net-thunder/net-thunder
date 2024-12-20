package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.jaspercloud.sdwan.util.IPUtil;
import io.jaspercloud.sdwan.util.ShortUUID;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

import java.util.Arrays;

public class IpLayerPacket implements IpPacket {

    private ByteBuf byteBuf;
    private boolean update = false;

    public IpLayerPacket(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public void setSrcIP(String ip) {
        writeIp(12, ip);
        update = true;
    }

    public void setDstIP(String ip) {
        writeIp(16, ip);
        update = true;
    }

    @Override
    public short getVersion() {
        try {
            byteBuf.markReaderIndex();
            byteBuf.readerIndex(9);
            short protocol = byteBuf.readUnsignedByte();
            return protocol;
        } finally {
            byteBuf.resetReaderIndex();
        }
    }

    @Override
    public int getProtocol() {
        try {
            byteBuf.markReaderIndex();
            byteBuf.readerIndex(9);
            int protocol = byteBuf.readUnsignedByte();
            return protocol;
        } finally {
            byteBuf.resetReaderIndex();
        }
    }

    @Override
    public String getSrcIP() {
        return readIp(12);
    }

    @Override
    public String getDstIP() {
        return readIp(16);
    }

    public int getSrcPort() {
        return readPort(20);
    }

    public int getDstPort() {
        return readPort(22);
    }

    public String getIdentifier() {
        return getIdentifier(false);
    }

    public String getIdentifier(boolean exchangeIp) {
        int protocol = getProtocol();
        if (IpPacket.Icmp == protocol) {
            String srcIP = getSrcIP();
            String dstIP = getDstIP();
            int identifier;
            byteBuf.markReaderIndex();
            try {
                byteBuf.readerIndex(24);
                identifier = byteBuf.readUnsignedShort();
            } finally {
                byteBuf.resetReaderIndex();
            }
            String key;
            if (exchangeIp) {
                key = String.format("%s:%s %s->%s", protocol, identifier, dstIP, srcIP);
            } else {
                key = String.format("%s:%s %s->%s", protocol, identifier, srcIP, dstIP);
            }
            return key;
        } else if (Arrays.asList(IpPacket.Tcp, IpPacket.Udp).contains(protocol)) {
            String srcIP = getSrcIP();
            String dstIP = getDstIP();
            int srcPort = getSrcPort();
            int dstPort = getDstPort();
            String key;
            if (exchangeIp) {
                key = String.format("%s: %s:%d->%s:%d", protocol, dstIP, dstPort, srcIP, srcPort);
            } else {
                key = String.format("%s: %s:%d->%s:%d", protocol, srcIP, srcPort, dstIP, dstPort);
            }
            return key;
        } else {
            return ShortUUID.gen();
        }
    }

    public ByteBuf getPayload() {
        byteBuf.markReaderIndex();
        try {
            byteBuf.readerIndex(0);
            short head = byteBuf.readUnsignedByte();
            byte headLen = (byte) ((head & 0b00001111) * 4);
            byteBuf.readerIndex(headLen);
            ByteBuf payload = byteBuf.readSlice(byteBuf.readableBytes());
            return payload;
        } finally {
            byteBuf.resetReaderIndex();
        }
    }

    public void setPayload(ByteBuf payload) {
        byteBuf.markReaderIndex();
        try {
            byteBuf.readerIndex(0);
            short head = byteBuf.readUnsignedByte();
            byte headLen = (byte) ((head & 0b00001111) * 4);
            byteBuf.writerIndex(2);
            byteBuf.writeShort(headLen + payload.readableBytes());
            byteBuf.writerIndex(headLen);
            byteBuf.writeBytes(payload);
        } finally {
            byteBuf.resetReaderIndex();
        }
        update = true;
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

    private int readPort(int index) {
        byteBuf.markReaderIndex();
        try {
            byteBuf.readerIndex(index);
            int port = byteBuf.readUnsignedShort();
            return port;
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

    public ByteBuf rebuild() {
        if (!update) {
            return byteBuf;
        }
        byteBuf.markReaderIndex();
        byteBuf.readerIndex(0);
        short head = byteBuf.readUnsignedByte();
        byte headLen = (byte) ((head & 0b00001111) * 4);
        byteBuf.readerIndex(9);
        int protocol = byteBuf.readUnsignedByte();
        byteBuf.readerIndex(headLen);
        ByteBuf payload = byteBuf.readSlice(byteBuf.readableBytes());
        byteBuf.resetReaderIndex();
        switch (protocol) {
            case Ipv4Packet.Tcp: {
                String srcIp = getSrcIP();
                String dstIp = getDstIP();
                reCalcTcpCheckSum(payload, protocol, srcIp, dstIp);
                break;
            }
            case Ipv4Packet.Udp: {
                String srcIp = getSrcIP();
                String dstIp = getDstIP();
                reCalcUdpCheckSum(payload, protocol, srcIp, dstIp);
                break;
            }
            case Ipv4Packet.Icmp: {
                reCalcIcmpCheckSum(payload);
                break;
            }
            default: {
                break;
            }
        }
        reCalcIpCheckSum();
        return byteBuf;
    }

    private void reCalcIcmpCheckSum(ByteBuf payload) {
        //set 0
        payload.markWriterIndex();
        payload.writerIndex(2);
        payload.writeShort(0);
        payload.resetWriterIndex();
        //calc
        int sum = CheckSum.calcIp(payload);
        //set checksum
        payload.markWriterIndex();
        payload.writerIndex(2);
        payload.writeShort(sum);
        payload.resetWriterIndex();
    }

    private void reCalcTcpCheckSum(ByteBuf payload, int protocol, String srcIp, String dstIp) {
        ByteBuf ipHeaderByteBuf = ByteBufUtil.newPacketBuf();
        try {
            int sum = 0;
            //ipHeader
            ipHeaderByteBuf.writeBytes(IPUtil.ip2bytes(srcIp));
            ipHeaderByteBuf.writeBytes(IPUtil.ip2bytes(dstIp));
            ipHeaderByteBuf.writeByte(0);
            ipHeaderByteBuf.writeByte(protocol);
            ipHeaderByteBuf.writeShort(payload.readableBytes());
            sum += CheckSum.calcTcpUdp(ipHeaderByteBuf);
            //set 0
            payload.markWriterIndex();
            payload.writerIndex(16);
            payload.writeShort(0);
            payload.resetWriterIndex();
            sum += CheckSum.calcTcpUdp(payload);
            sum = CheckSum.calcHighLow(sum);
            //set checksum
            payload.markWriterIndex();
            payload.writerIndex(16);
            payload.writeShort(sum);
            payload.resetWriterIndex();
        } finally {
            ipHeaderByteBuf.release();
        }
    }

    private void reCalcUdpCheckSum(ByteBuf payload, int protocol, String srcIp, String dstIp) {
        ByteBuf ipHeaderByteBuf = ByteBufUtil.newPacketBuf();
        int sum = 0;
        try {
            //ipHeader
            ipHeaderByteBuf.writeBytes(IPUtil.ip2bytes(srcIp));
            ipHeaderByteBuf.writeBytes(IPUtil.ip2bytes(dstIp));
            ipHeaderByteBuf.writeByte(0);
            ipHeaderByteBuf.writeByte(protocol);
            ipHeaderByteBuf.writeShort(payload.readableBytes());
            sum += CheckSum.calcTcpUdp(ipHeaderByteBuf);
            //set 0
            payload.markWriterIndex();
            payload.writerIndex(6);
            payload.writeShort(0);
            payload.resetWriterIndex();
            sum += CheckSum.calcTcpUdp(payload);
            sum = CheckSum.calcHighLow(sum);
            //set checksum
            payload.markWriterIndex();
            payload.writerIndex(6);
            payload.writeShort(sum);
            payload.resetWriterIndex();
        } finally {
            ipHeaderByteBuf.release();
        }
    }

    private void reCalcIpCheckSum() {
        byteBuf.markReaderIndex();
        ByteBuf head = byteBuf.readSlice(20);
        byteBuf.resetReaderIndex();
        //set 0
        head.markWriterIndex();
        head.writerIndex(10);
        head.writeShort(0);
        head.resetWriterIndex();
        //calc
        int sum = CheckSum.calcIp(head);
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
    public IpLayerPacket retain() {
        retain(1);
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return byteBuf.release(decrement);
    }

    @Override
    public String toString() {
        return String.format("%s -> %s", getSrcIP(), getDstIP());
    }
}
