package io.jaspercloud.sdwan.tun.linux;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.support.Cidr;
import io.jaspercloud.sdwan.tun.CheckInvoke;
import io.jaspercloud.sdwan.tun.ProcessUtil;
import io.jaspercloud.sdwan.tun.TunAddress;
import io.jaspercloud.sdwan.tun.TunDevice;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LinuxTunDevice extends TunDevice {

    private int fd;
    private int mtu = 65535;
    private Timeval timeval;
    private boolean closing = false;

    public LinuxTunDevice(String tunName, String type, String guid) {
        super(tunName, type, guid);
    }

    @Override
    public void open() throws Exception {
        fd = LinuxNativeApi.open("/dev/net/tun", LinuxNativeApi.O_RDWR);
        int flags = LinuxNativeApi.fcntl(fd, LinuxNativeApi.F_GETFL, 0);
        int noblock = LinuxNativeApi.fcntl(fd, LinuxNativeApi.F_SETFL, flags | LinuxNativeApi.O_NONBLOCK);
        CheckInvoke.check(noblock, 0);
        timeval = new Timeval();
        timeval.tv_sec = 5;
        Ifreq ifreq = new Ifreq(getName(), (short) (LinuxNativeApi.IFF_TUN | LinuxNativeApi.IFF_NO_PI));
        LinuxNativeApi.ioctl(fd, LinuxNativeApi.TUNSETIFF, ifreq);
        setActive(true);
    }

    @Override
    public int getVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setIP(String addr, int netmaskPrefix) throws Exception {
        Cidr cidr = Cidr.parseCidr(String.format("%s/%s", addr, netmaskPrefix));
        int addAddr = ProcessUtil.exec(String.format("ifconfig %s inet %s netmask %s",
                getName(), addr, cidr.getMaskAddress()));
        CheckInvoke.check(addAddr, 0);
        String cmd = String.format("/sbin/ip link set dev %s up", getName());
        log.info("setIP: {}", cmd);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    public void setMTU(int mtu) throws Exception {
        String cmd = String.format("/sbin/ip link set %s mtu %s", getName(), mtu);
        log.info("setMTU: {}", cmd);
        int code = ProcessUtil.exec(cmd);
        CheckInvoke.check(code, 0);
    }

    @Override
    public ByteBuf readPacket(ByteBufAllocator alloc) {
        while (true) {
            if (closing) {
                throw new ProcessException("Device is closed.");
            }
            FdSet fdSet = new FdSet();
            fdSet.FD_SET(fd);
            int select = LinuxNativeApi.select(fd + 1, fdSet, null, null, timeval);
            if (-1 == select) {
                throw new ProcessException("select -1");
            }
            if (fdSet.FD_ISSET(fd)) {
                byte[] bytes = new byte[mtu];
                int read = LinuxNativeApi.read(fd, bytes, bytes.length);
                if (read <= 0) {
                    continue;
                }
                ByteBuf byteBuf = alloc.buffer(read);
                byteBuf.writeBytes(bytes, 0, read);
                return byteBuf;
            }
        }
    }

    @Override
    public void writePacket(ByteBufAllocator alloc, ByteBuf msg) {
        if (closing) {
            throw new ProcessException("Device is closed.");
        }
        //TunChannel已回收
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        LinuxNativeApi.write(fd, bytes, bytes.length);
    }

    @Override
    public void enableNetMesh(TunAddress tunAddress, String ethName) throws Exception {
        Iptables.enableIpForward(ethName, tunAddress.getTunName());
    }

    @Override
    public void disableNetMesh(TunAddress tunAddress, String ethName) throws Exception {
        Iptables.disableIpForward(ethName, tunAddress.getTunName());
    }

    @Override
    public void close() throws Exception {
        if (closing) {
            return;
        }
        closing = true;
        int close = LinuxNativeApi.close(fd);
        CheckInvoke.check(close, 0);
    }

    @Override
    public boolean isClosed() {
        return !isActive();
    }

}
