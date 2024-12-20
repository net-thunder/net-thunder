package io.jaspercloud.sdwan.tun;

import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tun.linux.LinuxTunDevice;
import io.jaspercloud.sdwan.tun.osx.OsxTunDevice;
import io.jaspercloud.sdwan.tun.windows.WinTunDevice;
import io.jaspercloud.sdwan.util.NetworkInterfaceInfo;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.internal.PlatformDependent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
public class TunChannel extends AbstractChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(false);
    private TunChannelConfig channelConfig;
    private Runnable readTask = new Runnable() {
        @Override
        public void run() {
            try {
                doRead();
            } catch (ProcessException e) {
                log.error(e.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    };
    private boolean open;
    private boolean readPending;
    private EventLoop readLoop = new DefaultEventLoop();
    private List<Object> readBuf = new ArrayList<>();

    private TunAddress tunAddress;
    private TunDevice tunDevice;

    public TunAddress getTunAddress() {
        return tunAddress;
    }

    public TunDevice getTunDevice() {
        return tunDevice;
    }

    public TunChannel() {
        super(null);
        channelConfig = new TunChannelConfig(this);
        open = true;
    }

    @Override
    protected SocketAddress localAddress0() {
        return tunAddress;
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return tunAddress;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        tunAddress = (TunAddress) localAddress;
        String tunName = tunAddress.getTunName();
        String type = "jaspercloud";
        String guid = DigestUtils.md5Hex(tunName);
        if (PlatformDependent.isOsx()) {
            tunDevice = new OsxTunDevice(tunName, type, guid);
        } else if (PlatformDependent.isWindows()) {
            tunDevice = new WinTunDevice(tunName, type, guid);
        } else {
            tunDevice = new LinuxTunDevice(tunName, type, guid);
        }
        tunDevice.open();
        applyLocalAddress();
        int mtu = config().getOption(TunChannelConfig.MTU);
        tunDevice.setMTU(mtu);
    }

    public void enableNetMesh(String ethName) throws Exception {
        tunDevice.enableNetMesh(tunAddress, ethName);
    }

    public void disableNetMesh(String ethName) throws Exception {
        tunDevice.disableNetMesh(tunAddress, ethName);
    }

    public void applyLocalAddress() throws Exception {
        String ip = tunAddress.getIp();
        tunDevice.setIP(ip, tunAddress.getMaskBits());
    }

    public static void waitAddress(String vip, int timeout) throws Exception {
        long s = System.currentTimeMillis();
        while (true) {
            NetworkInterfaceInfo networkInterfaceInfo = NetworkInterfaceUtil.findIp(vip);
            if (null != networkInterfaceInfo) {
                return;
            }
            long e = System.currentTimeMillis();
            long diff = e - s;
            if (diff > timeout) {
                throw new TimeoutException();
            }
            Thread.sleep(10);
        }
    }

    @Override
    protected void doClose() throws Exception {
        open = false;
        tunDevice.close();
    }

    @Override
    protected void doBeginRead() throws Exception {
        if (readPending) {
            return;
        }
        if (!isActive()) {
            return;
        }
        readPending = true;
        readLoop.execute(readTask);
    }

    private void doRead() {
        if (!readPending) {
            // We have to check readPending here because the Runnable to read could have been scheduled and later
            // during the same read loop readPending was set to false.
            return;
        }
        // In OIO we should set readPending to false even if the read was not successful so we can schedule
        // another read on the event loop if no reads are done.
        readPending = false;

        final ChannelConfig config = config();
        final ChannelPipeline pipeline = pipeline();
        final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
        allocHandle.reset(config);

        boolean closed = false;
        Throwable exception = null;
        try {
            do {
                // Perform a read.
                int localRead = doReadMessages(readBuf);
                if (localRead == 0) {
                    break;
                }
                if (localRead < 0) {
                    closed = true;
                    break;
                }

                allocHandle.incMessagesRead(localRead);
            } while (allocHandle.continueReading());
        } catch (Throwable t) {
            exception = t;
        }

        boolean readData = false;
        int size = readBuf.size();
        if (size > 0) {
            readData = true;
            for (int i = 0; i < size; i++) {
                readPending = false;
                pipeline.fireChannelRead(readBuf.get(i));
            }
            readBuf.clear();
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();
        }

        if (exception != null) {
            if (exception instanceof IOException) {
                closed = true;
            }

            // pipeline.fireExceptionCaught(exception);
        }

        if (closed) {
            if (isOpen()) {
                unsafe().close(unsafe().voidPromise());
            }
        } else if (readPending || config.isAutoRead() || !readData && isActive()) {
            // Reading 0 bytes could mean there is a SocketTimeout and no data was actually read, so we
            // should execute read() again because no data may have been read.
            read();
        }
    }

    private int doReadMessages(List<Object> readBuf) {
        try {
            ByteBuf byteBuf = tunDevice.readPacket(config().getAllocator());
            byteBuf.markReaderIndex();
            byte version = (byte) (byteBuf.readUnsignedByte() >> 4);
            if (4 != version) {
                //read ipv4 only
                byteBuf.release();
                return 0;
            }
            byteBuf.resetReaderIndex();
            readBuf.add(byteBuf);
            return 1;
        } catch (ProcessException e) {
            throw e;
        } catch (Exception e) {
            throw new ProcessException("doReadMessages: " + e.getMessage(), e);
        }
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        while (isActive()) {
            final Object msg = in.current();
            if (msg == null) {
                break;
            }
            if (!(msg instanceof ByteBuf)) {
                break;
            }
            try {
                ByteBuf byteBuf = (ByteBuf) msg;
                tunDevice.writePacket(alloc(), byteBuf);
            } finally {
                in.remove();
            }
        }
    }

    @Override
    protected void doDisconnect() throws Exception {

    }

    @Override
    protected TunChannelUnsafe newUnsafe() {
        return new TunChannelUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof DefaultEventLoop;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean isActive() {
        return tunDevice != null && tunDevice.isActive();
    }

    @Override
    public ChannelConfig config() {
        return channelConfig;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    private class TunChannelUnsafe extends AbstractUnsafe {

        @Override
        public void connect(final SocketAddress remoteAddress,
                            final SocketAddress localAddress,
                            final ChannelPromise promise) {
        }
    }

}
