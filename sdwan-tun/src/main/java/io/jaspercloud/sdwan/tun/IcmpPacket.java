package io.jaspercloud.sdwan.tun;

import cn.hutool.core.lang.Assert;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import io.netty.buffer.ByteBuf;

public class IcmpPacket {

    public static final byte Echo = 8;
    public static final byte Reply = 0;

    private byte type;
    private byte code;
    private int checksum;
    private int identifier;
    private int sequence;
    private long timestamp;
    private ByteBuf payload;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public int getChecksum() {
        return checksum;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public int getSequence() {
        return sequence;
    }

    public void setSequence(int sequence) {
        this.sequence = sequence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ByteBuf getPayload() {
        return payload;
    }

    public void setPayload(ByteBuf payload) {
        this.payload = payload;
    }

    public static IcmpPacket decodeMark(ByteBuf byteBuf) {
        byteBuf.markReaderIndex();
        IcmpPacket packet = decode(byteBuf);
        byteBuf.resetReaderIndex();
        return packet;
    }

    public static IcmpPacket decode(ByteBuf byteBuf) {
        IcmpPacket icmpPacket = new IcmpPacket();
        byte type = byteBuf.readByte();
        icmpPacket.setType(type);
        byte code = byteBuf.readByte();
        icmpPacket.setCode(code);
        int checksum = byteBuf.readUnsignedShort();
        icmpPacket.setChecksum(checksum);
        int identifier = byteBuf.readUnsignedShort();
        icmpPacket.setIdentifier(identifier);
        int sequence = byteBuf.readUnsignedShort();
        icmpPacket.setSequence(sequence);
        long timestamp = byteBuf.readLong();
        icmpPacket.setTimestamp(timestamp);
        ByteBuf payload = byteBuf.readSlice(byteBuf.readableBytes());
        icmpPacket.setPayload(payload);
        return icmpPacket;
    }

    public ByteBuf encode() {
        return encode(true, false);
    }

    public ByteBuf encode(boolean calc, boolean check) {
        ByteBuf byteBuf = ByteBufUtil.newPacketBuf();
        byteBuf.writeByte(type);
        byteBuf.writeByte(code);
        int calcChecksum;
        if (calc) {
            calcChecksum = calcChecksum();
            if (check) {
                Assert.isTrue(calcChecksum == getChecksum(), "checksum error");
            }
        } else {
            calcChecksum = 0;
        }
        checksum = calcChecksum;
        byteBuf.writeShort(calcChecksum);
        byteBuf.writeShort(identifier);
        byteBuf.writeShort(sequence);
        byteBuf.writeLong(timestamp);
        byteBuf.writeBytes(payload);
        return byteBuf;
    }

    private int calcChecksum() {
        ByteBuf payloadByteBuf = ByteBufUtil.newPacketBuf();
        int sum = 0;
        try {
            payloadByteBuf.writeByte(type);
            payloadByteBuf.writeByte(code);
            payloadByteBuf.writeShort(0);
            payloadByteBuf.writeShort(identifier);
            payloadByteBuf.writeShort(sequence);
            payloadByteBuf.writeLong(timestamp);
            payload.markReaderIndex();
            payloadByteBuf.writeBytes(payload);
            payload.resetReaderIndex();
            sum += CheckSum.calcTcpUdp(payloadByteBuf);
            sum = CheckSum.calcHighLow(sum);
            return sum;
        } finally {
            payloadByteBuf.release();
        }
    }
}
