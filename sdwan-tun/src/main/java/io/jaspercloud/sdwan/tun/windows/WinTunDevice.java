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

    public WinTunDevice(String name, String type, String guid) {
        super(name, type, guid);
    }

    @Override
    public void open() throws Exception {
        super.open();
        try {
            adapter = WinTunNativeApi.WintunOpenAdapter(new WString(getName()), new WString(getType()));
        } catch (LastErrorException e) {
            adapter = WinTunNativeApi.WintunCreateAdapter(new WString(getName()), new WString(getType()), getGuid());
        }
        session = WinTunNativeApi.WintunStartSession(adapter, WinTunNativeApi.WINTUN_MAX_RING_CAPACITY);
    }

    @Override
    public int getVersion() {
        return WinTunNativeApi.WintunGetRunningDriverVersion();
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
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (isActive()) {
            try {
                WinDef.UINTByReference reference = new WinDef.UINTByReference();
                Pointer packetPointer = WinTunNativeApi.WintunReceivePacket(session, reference);
                try {
                    int packetSize = reference.getValue().intValue();
                    byte[] bytes = packetPointer.getByteArray(0, packetSize);
                    ByteBuf byteBuf = alloc.buffer(bytes.length);
                    byteBuf.writeBytes(bytes, 0, bytes.length);
                    return byteBuf;
                } finally {
                    WinTunNativeApi.WintunReleaseReceivePacket(session, packetPointer);
                }
            } catch (LastErrorException e) {
                if (e.getErrorCode() == WinTunNativeApi.ERROR_NO_MORE_ITEMS) {
                    Kernel32.INSTANCE.WaitForSingleObject(WinTunNativeApi.WintunGetReadWaitEvent(session), Kernel32.INFINITE);
                } else {
                    throw e;
                }
            }
        }
        throw new ProcessException("Device is closed.");
    }

    @Override
    public void writePacket(ByteBufAllocator alloc, ByteBuf msg) {
        if (!isActive()) {
            throw new ProcessException("Device is closed.");
        }
        //TunChannel已回收
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        Pointer packetPointer = WinTunNativeApi.WintunAllocateSendPacket(session, bytes.length);
        packetPointer.write(0, bytes, 0, bytes.length);
        WinTunNativeApi.WintunSendPacket(session, packetPointer);
    }

    @Override
    public void enableNetMesh(TunAddress tunAddress, String ethName) throws Exception {
        NetworkInterfaceInfo eth = NetworkInterfaceUtil.findEth(ethName);
        String ethIp = eth.getInterfaceAddress().getAddress().getHostAddress();
        String tunIp = tunAddress.getIp();
        int maskBits = tunAddress.getMaskBits();
        Ics.operateICS(ethIp, tunIp, true);
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
    public void disableNetMesh(TunAddress tunAddress, String ethName) throws Exception {
        NetworkInterfaceInfo eth = NetworkInterfaceUtil.findEth(ethName);
        String ethIp = eth.getInterfaceAddress().getAddress().getHostAddress();
        String tunIp = tunAddress.getIp();
        Ics.operateICS(ethIp, tunIp, false);
    }

    @Override
    public void close() throws Exception {
        if (!isActive()) {
            return;
        }
        super.close();
        Kernel32.INSTANCE.SetEvent(WinTunNativeApi.WintunGetReadWaitEvent(session));
        WinTunNativeApi.WintunEndSession(session);
        WinTunNativeApi.WintunCloseAdapter(adapter);
    }
}
