package io.jaspercloud.sdwan.tun.windows;

import com.sun.jna.LastErrorException;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.*;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WinTunDevice extends TunDevice {

    private WinNT.HANDLE adapter;
    private WinNT.HANDLE session;
    private int mtu = 65535;
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
        log.info("setIP: {}", cmd);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        String cmd = String.format("netsh interface ipv4 set subinterface \"%s\" mtu=%s store=active", getName(), mtu);
        log.info("setMTU: {}", cmd);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
        this.mtu = mtu;
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (isActive()) {
            if (closing) {
                throw new ProcessException("Device is closed.");
            }
            try {
                WinDef.UINTByReference reference = new WinDef.UINTByReference();
                Pointer packetPointer = NativeWinTunApi.WintunReceivePacket(session, reference);
                try {
                    int packetSize = reference.getValue().intValue();
                    byte[] bytes = packetPointer.getByteArray(0, packetSize);
                    ByteBuf byteBuf = alloc.buffer(bytes.length);
                    byteBuf.writeBytes(bytes, 0, bytes.length);
                    return byteBuf;
                } finally {
                    NativeWinTunApi.WintunReleaseReceivePacket(session, packetPointer);
                }
            } catch (LastErrorException e) {
                if (e.getErrorCode() == NativeWinTunApi.ERROR_NO_MORE_ITEMS) {
                    Kernel32.INSTANCE.WaitForSingleObject(NativeWinTunApi.WintunGetReadWaitEvent(session), Kernel32.INFINITE);
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
    public void enableShareNetwork(TunAddress tunAddress, String ethName) throws Exception {
        NetworkInterfaceInfo eth = NetworkInterfaceUtil.findEth(ethName);
        String ethIp = eth.getInterfaceAddress().getAddress().getHostAddress();
        String tunIp = tunAddress.getIp();
        int maskBits = tunAddress.getMaskBits();
        Ics.enable(ethIp, tunIp, true);
        {
            String cmd = String.format("netsh interface ipv4 set address name=\"%s\" static %s/%s", getName(), tunIp, maskBits);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
            TunChannel.waitAddress(tunIp, 30 * 1000);
        }
        {
            String cmd = String.format("netsh interface ipv4 add address name=\"%s\" %s/24", getName(), Ics.IcsIp);
            int code = ProcessUtil.exec(cmd);
            CheckInvoke.check(code, 0);
        }
    }

    @Override
    public void disableShareNetwork(TunAddress tunAddress, String ethName) throws Exception {
        NetworkInterfaceInfo eth = NetworkInterfaceUtil.findEth(ethName);
        String ethIp = eth.getInterfaceAddress().getAddress().getHostAddress();
        String tunIp = tunAddress.getIp();
        Ics.enable(ethIp, tunIp, false);
    }

    @Override
    public void close() {
        if (closing) {
            return;
        }
        Kernel32.INSTANCE.SetEvent(NativeWinTunApi.WintunGetReadWaitEvent(session));
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
