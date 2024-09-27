package io.jaspercloud.sdwan.tun.osx;

import com.sun.jna.Native;
import com.sun.jna.ptr.IntByReference;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import static java.nio.charset.StandardCharsets.US_ASCII;

@Slf4j
public class OsxTunDevice extends TunDevice {

    private int fd;
    private int mtu = 65535;
    private boolean closing = false;
    private String deviceName;

    public OsxTunDevice(String tunName, String type, String guid) {
        super(tunName, type, guid);
    }

    @Override
    public void open() throws Exception {
        //fd
        fd = OsxNativeApi.socket(OsxNativeApi.AF_SYSTEM, OsxNativeApi.SOCK_DGRAM, OsxNativeApi.SYSPROTO_CONTROL);
        CtlInfo ctlInfo = new CtlInfo(OsxNativeApi.UTUN_CONTROL_NAME);
        OsxNativeApi.ioctl(fd, OsxNativeApi.CTLIOCGINFO, ctlInfo);
        SockaddrCtl address = new SockaddrCtl(OsxNativeApi.AF_SYSTEM, (short) OsxNativeApi.SYSPROTO_CONTROL, ctlInfo.ctl_id, 0);
        OsxNativeApi.connect(fd, address, address.sc_len);
        //deviceName
        SockName sockName = new SockName();
        IntByReference sockNameLen = new IntByReference(SockName.LENGTH);
        OsxNativeApi.getsockopt(fd, OsxNativeApi.SYSPROTO_CONTROL, OsxNativeApi.UTUN_OPT_IFNAME, sockName, sockNameLen);
        deviceName = Native.toString(sockName.name, US_ASCII);
        setActive(true);
    }

    @Override
    public int getVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIP(String addr, int netmaskPrefix) throws Exception {
        String deviceName = getEthName();
        Ifreq ifreq = new Ifreq(deviceName, mtu);
        OsxNativeApi.ioctl(fd, OsxNativeApi.SIOCSIFMTU, ifreq);
        Cidr cidr = Cidr.parseCidr(String.format("%s/%s", addr, netmaskPrefix));
        int addAddr = ProcessUtil.exec(String.format("ifconfig %s inet %s netmask %s broadcast %s",
                deviceName, addr, cidr.getMaskAddress(), cidr.getBroadcastAddress()));
        CheckInvoke.check(addAddr, 0);
        int up = ProcessUtil.exec(String.format("ifconfig %s up", deviceName));
        CheckInvoke.check(up, 0);
        //route: mac与其他系统的差异
        int route = ProcessUtil.exec(String.format("route -n add -net %s -interface %s", String.format("%s/%s", cidr.getNetworkIdentifier(), netmaskPrefix), deviceName));
        CheckInvoke.check(route, 0);
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        Ifreq ifreq = new Ifreq(deviceName, mtu);
        OsxNativeApi.ioctl(fd, OsxNativeApi.SIOCSIFMTU, ifreq);
    }

    public String getEthName() {
        return deviceName;
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (true) {
            if (closing) {
                throw new ProcessException("Device is closed.");
            }
            byte[] bytes = new byte[mtu];
            int read = OsxNativeApi.read(fd, bytes, bytes.length);
            if (read <= 0) {
                continue;
            }
            ByteBuf byteBuf = alloc.buffer(read);
            //process loopback
            byteBuf.writeBytes(bytes, 4, read);
            return byteBuf;
        }
    }

    @Override
    public void writePacket(ByteBufAllocator alloc, ByteBuf msg) {
        if (closing) {
            throw new ProcessException("Device is closed.");
        }
        //TunChannel已回收
        byte[] bytes = new byte[msg.readableBytes() + 4];
        bytes[3] = 0x2;
        //process loopback
        msg.readBytes(bytes, 4, msg.readableBytes());
        OsxNativeApi.write(fd, bytes, bytes.length);
    }

    @Override
    public void enableNetMesh(TunAddress tunAddress, String ethName) throws Exception {

    }

    @Override
    public void disableNetMesh(TunAddress tunAddress, String ethName) throws Exception {

    }

    @Override
    public void close() throws Exception {
        if (closing) {
            return;
        }
        closing = true;
        int close = OsxNativeApi.close(fd);
        CheckInvoke.check(close, 0);
    }

    @Override
    public boolean isClosed() {
        return !isActive();
    }

}
