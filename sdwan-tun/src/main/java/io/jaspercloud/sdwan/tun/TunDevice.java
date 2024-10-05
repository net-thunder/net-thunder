package io.jaspercloud.sdwan.tun;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public abstract class TunDevice {

    private String name;
    private String type;
    private String guid;
    private boolean active;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getGuid() {
        return guid;
    }

    public boolean isActive() {
        return active;
    }

    public TunDevice(String name, String type, String guid) {
        this.name = name;
        this.type = type;
        this.guid = guid;
    }

    public void open() throws Exception {
        active = true;
    }

    public void close() throws Exception {
        active = false;
    }

    public abstract int getVersion();

    public abstract void setIP(String addr, int netmaskPrefix) throws Exception;

    public abstract void setMTU(int mtu) throws Exception;

    public abstract ByteBuf readPacket(ByteBufAllocator alloc);

    public abstract void writePacket(ByteBufAllocator alloc, ByteBuf msg);

    public abstract void enableNetMesh(TunAddress tunAddress, String ethName) throws Exception;

    public abstract void disableNetMesh(TunAddress tunAddress, String ethName) throws Exception;
}
