package io.jaspercloud.sdwan.tun.windows;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinNT;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.*;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class WinTunDevice extends TunDevice {

    private WinNT.HANDLE adapter;
    private WinNT.HANDLE session;
    private boolean closing = false;

    public WinTunDevice(String name, String type, String guid) {
        super(name, type, guid);
    }

    @Override
    public void open() throws Exception {
        try {
            adapter = NativeWinTunApi.WintunOpenAdapter(new WString(getName()), new WString(getType()));
        } catch (LastErrorException e) {
            adapter = NativeWinTunApi.WintunCreateAdapter(new WString(getName()), new WString(getType()), getGuid());
        }
        session = NativeWinTunApi.WintunStartSession(adapter, NativeWinTunApi.WINTUN_MAX_RING_CAPACITY);
        setActive(true);
    }

    @Override
    public int getVersion() {
        return NativeWinTunApi.WintunGetRunningDriverVersion();
    }

    @Override
    public void setIP(String addr, int netmaskPrefix) throws Exception {
        String cmd = String.format("netsh interface ipv4 set address name=\"%s\" static %s/%s", getName(), addr, netmaskPrefix);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        String cmd = String.format("netsh interface ipv4 set subinterface \"%s\" mtu=%s store=active", getName(), mtu);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (isActive()) {
            if (closing) {
                throw new ProcessException("Device is closed.");
            }
            try {
                Pointer packetSizePointer = new Memory(Native.POINTER_SIZE);
                Pointer packetPointer = NativeWinTunApi.WintunReceivePacket(session, packetSizePointer);
                try {
                    int packetSize = packetSizePointer.getInt(0);
                    byte[] bytes = packetPointer.getByteArray(0, packetSize);
                    ByteBuf byteBuf = alloc.buffer(bytes.length);
                    byteBuf.writeBytes(bytes);
                    return byteBuf;
                } finally {
                    NativeWinTunApi.WintunReleaseReceivePacket(session, packetPointer);
                }
            } catch (LastErrorException e) {
                if (e.getErrorCode() == NativeWinTunApi.ERROR_NO_MORE_ITEMS) {
                    NativeKernel32Api.INSTANCE.WaitForSingleObject(NativeWinTunApi.WintunGetReadWaitEvent(session), NativeKernel32Api.INFINITE);
                } else {
                    throw e;
                }
            }
        }
        throw new ProcessException("Device is closed.");
    }

    @Override
    public void writePacket(ByteBufAllocator alloc, ByteBuf msg) {
        if (closing) {
            throw new ProcessException("Device is closed.");
        }
        //TunChannel已回收
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        Pointer packetPointer = NativeWinTunApi.WintunAllocateSendPacket(session, bytes.length);
        packetPointer.write(0, bytes, 0, bytes.length);
        NativeWinTunApi.WintunSendPacket(session, packetPointer);
    }

    @Override
    public void enableShareNetwork(String fromEth, TunAddress tunAddress) throws Exception {
        NetworkInterfaceInfo from = NetworkInterfaceUtil.findEth(fromEth);
        String fromIp = from.getInterfaceAddress().getAddress().getHostAddress();
        String toIp = tunAddress.getIp();
        int maskBits = tunAddress.getMaskBits();
        Ics.enable(fromIp, toIp, true);
        {
            String cmd = String.format("netsh interface ipv4 set address name=\"%s\" static %s/%s", getName(), toIp, maskBits);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
            TunChannel.waitAddress(toIp, 30 * 1000);
        }
        {
            String cmd = String.format("netsh interface ipv4 add address name=\"%s\" 192.168.137.1/24", getName());
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
        }
    }

    @Override
    public void disableShareNetwork(String fromEth, TunAddress tunAddress) throws Exception {
        NetworkInterfaceInfo from = NetworkInterfaceUtil.findEth(fromEth);
        String fromIp = from.getInterfaceAddress().getAddress().getHostAddress();
        String toIp = tunAddress.getIp();
        Ics.enable(fromIp, toIp, false);
    }

    @Override
    public void close() {
        if (closing) {
            return;
        }
        closing = true;
        setActive(false);
        NativeWinTunApi.WintunEndSession(session);
        NativeWinTunApi.WintunCloseAdapter(adapter);
    }

    @Override
    public boolean isClosed() {
        return !isActive();
    }

}
