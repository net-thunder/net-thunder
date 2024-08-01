package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.support.Referenced;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;

public class EthernetPacket implements Referenced {

    public static final int Ipv4 = 0x0800;

    private String srcAddr;
    private String dstAddr;
    private int protocol;
    private ByteBuf payload;

    public static EthernetPacket decode(ByteBuf byteBuf) {
        String dstAddr = parseAddr(byteBuf);
        String srcAddr = parseAddr(byteBuf);
        int protocol = byteBuf.readUnsignedShort();
        ByteBuf payload = byteBuf.readSlice(byteBuf.readableBytes());
        EthernetPacket packet = new EthernetPacket();
        packet.setSrcAddr(srcAddr);
        packet.setDstAddr(dstAddr);
        packet.setProtocol(protocol);
        packet.setPayload(payload);
        return packet;
    }

    public ByteBuf encode() {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        byteBuf.writeBytes(encodeAddr(dstAddr));
        byteBuf.writeBytes(encodeAddr(srcAddr));
        byteBuf.writeShort(protocol);
        byteBuf.writeBytes(payload);
        return byteBuf;
    }

    private byte[] encodeAddr(String addr) {
        String[] split = addr.split("\\:");
        byte[] bytes = new byte[split.length];
        for (int i = 0; i < split.length; i++) {
            String sp = split[i];
            bytes[i] = (byte) Integer.parseInt(sp, 16);
        }
        return bytes;
    }

    private static String parseAddr(ByteBuf byteBuf) {
        byte b1 = byteBuf.readByte();
        byte b2 = byteBuf.readByte();
        byte b3 = byteBuf.readByte();
        byte b4 = byteBuf.readByte();
        byte b5 = byteBuf.readByte();
        byte b6 = byteBuf.readByte();
        String addr = String.format("%02x:%02x:%02x:%02x:%02x:%02x", b1, b2, b3, b4, b5, b6);
        return addr;
    }

    public String getSrcAddr() {
        return srcAddr;
    }

    public void setSrcAddr(String srcAddr) {
        this.srcAddr = srcAddr;
    }

    public String getDstAddr() {
        return dstAddr;
    }

    public void setDstAddr(String dstAddr) {
        this.dstAddr = dstAddr;
    }

    public int getProtocol() {
        return protocol;
    }

    public void setProtocol(int protocol) {
        this.protocol = protocol;
    }

    public ByteBuf getPayload() {
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
    }

    @Override
    public int refCnt() {
        return payload.refCnt();
    }

    @Override
    public EthernetPacket retain(int increment) {
        payload.retain(increment);
        return this;
    }

    @Override
    public boolean release(int decrement) {
        return payload.release(decrement);
    }
}
