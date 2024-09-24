package io.jaspercloud.sdwan.tun;

import cn.hutool.core.lang.Assert;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.jaspercloud.sdwan.util.IPUtil;
import io.netty.buffer.ByteBuf;

public class UdpPacket {

    private int srcPort;
    private int dstPort;
    private int len;
    private int checksum;
    private ByteBuf payload;

    public int getSrcPort() {
        return srcPort;
    }

    public void setSrcPort(int srcPort) {
        this.srcPort = srcPort;
    }

    public int getDstPort() {
        return dstPort;
    }

    public void setDstPort(int dstPort) {
        this.dstPort = dstPort;
    }

    public int getLen() {
        return len;
    }

    public void setLen(int len) {
        this.len = len;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public ByteBuf getPayload() {
        payload.resetReaderIndex();
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        payload.markReaderIndex();
        this.payload = payload;
    }

    public static UdpPacket decodeMark(ByteBuf byteBuf) {
        byteBuf.markReaderIndex();
        UdpPacket packet = decode(byteBuf);
        byteBuf.resetReaderIndex();
        return packet;
    }

    public static UdpPacket decode(ByteBuf byteBuf) {
        int srcPort = byteBuf.readUnsignedShort();
        int dstPort = byteBuf.readUnsignedShort();
        int len = byteBuf.readUnsignedShort();
        int checksum = byteBuf.readUnsignedShort();
        ByteBuf payload = byteBuf.readSlice(len - 8);
        //set
        UdpPacket udpPacket = new UdpPacket();
        udpPacket.setSrcPort(srcPort);
        udpPacket.setDstPort(dstPort);
        udpPacket.setLen(len);
        udpPacket.setChecksum(checksum);
        udpPacket.setPayload(payload);
        return udpPacket;
    }

    public ByteBuf encode(Ipv4Packet ipv4Packet) {
        return encode(ipv4Packet, false);
    }

    public ByteBuf encode(Ipv4Packet ipv4Packet, boolean check) {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        byteBuf.writeShort(getSrcPort());
        byteBuf.writeShort(getDstPort());
        byteBuf.writeShort(getLen());
        int calcChecksum = calcChecksum(ipv4Packet);
        if (check) {
            Assert.isTrue(calcChecksum == getChecksum(), "checksum error");
        }
        checksum = calcChecksum;
        byteBuf.writeShort(calcChecksum);
        byteBuf.writeBytes(getPayload());
        return byteBuf;
    }

    private int calcChecksum(Ipv4Packet ipv4Packet) {
        ByteBuf ipHeaderByteBuf = ByteBufUtil.newPacketBuf();
        ByteBuf payloadByteBuf = ByteBufUtil.newPacketBuf();
        int sum = 0;
        try {
            //ipHeader
            ipHeaderByteBuf.writeBytes(IPUtil.ip2bytes(ipv4Packet.getSrcIP()));
            ipHeaderByteBuf.writeBytes(IPUtil.ip2bytes(ipv4Packet.getDstIP()));
            ipHeaderByteBuf.writeByte(0);
            ipHeaderByteBuf.writeByte(ipv4Packet.getProtocol());
            ipHeaderByteBuf.writeShort(ipv4Packet.getPayload().readableBytes());
            sum += CheckSum.calcTcpUdp(ipHeaderByteBuf);
            //udp
            payloadByteBuf.writeShort(getSrcPort());
            payloadByteBuf.writeShort(getDstPort());
            payloadByteBuf.writeShort(getLen());
            //checksum字段置为0
            payloadByteBuf.writeShort(0);
            payloadByteBuf.writeBytes(getPayload());
            sum += CheckSum.calcTcpUdp(payloadByteBuf);
            sum = CheckSum.calcHighLow(sum);
            return sum;
        } finally {
            ipHeaderByteBuf.release();
            payloadByteBuf.release();
        }
    }
}
