package io.jaspercloud.sdwan.node;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import io.jaspercloud.sdwan.exception.ProcessCodeException;
import io.jaspercloud.sdwan.exception.ProcessException;
import io.jaspercloud.sdwan.tranport.Lifecycle;
import io.jaspercloud.sdwan.tun.IpLayerPacket;
import io.jaspercloud.sdwan.tun.Ipv4Packet;
import io.jaspercloud.sdwan.util.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author jasper
 * @create 2024/7/2
 */
@Slf4j
public class BaseSdWanNode implements Lifecycle, Runnable {

    private SdWanNodeConfig config;

    private VirtualRouter virtualRouter;
    private AtomicBoolean status = new AtomicBoolean(false);
    private AtomicBoolean loopStatus = new AtomicBoolean(false);
    private Thread loopThread;
    private ReentrantLock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private List<EventListener> eventListenerList = new ArrayList<>();

    public boolean getStatus() {
        return status.get();
    }

    public String getLocalVip() {
        return virtualRouter.getLocalVip();
    }

    public int getMaskBits() {
        return virtualRouter.getMaskBits();
    }

    public String getVipCidr() {
        return virtualRouter.getVipCidr();
    }

    public VirtualRouter getVirtualRouter() {
        return virtualRouter;
    }

    public void addEventListener(EventListener listener) {
        eventListenerList.add(listener);
    }

    public BaseSdWanNode(SdWanNodeConfig config) {
        this.config = config;
    }

    static {
        System.setProperty("io.netty.leakDetection.level", "PARANOID");
    }

    @Override
    public synchronized void start() throws Exception {
        install();
        log.info("SdWanNode started: vip={}", getLocalVip());
        if (config.getAutoReconnect()) {
            loopStatus.set(true);
        }
        loopThread = new Thread(this, "loop");
        loopThread.start();
        status.set(true);
    }

    protected void install() throws Exception {
        virtualRouter = new VirtualRouter(config) {

            @Override
            protected String processMacAddress(String hardwareAddress) {
                return BaseSdWanNode.this.processMacAddress(hardwareAddress);
            }

            @Override
            protected void onData(VirtualRouter router, IpLayerPacket packet) {
                BaseSdWanNode.this.onData(packet);
            }

            @Override
            protected void onSdWanClientInactive() {
                log.info("SdWanClientInactive");
                if (getStatus() && !config.getAutoReconnect()) {
                    fireEvent(EventListener::onErrorDisconnected);
                }
                if (!loopStatus.get()) {
                    return;
                }
                fireEvent(EventListener::onReConnecting);
                signalAll();
            }
        };
        virtualRouter.start();
        log.info("BaseSdWanNode installed");
    }

    protected void uninstall() throws Exception {
        log.info("BaseSdWanNode uninstalling");
        if (null != virtualRouter) {
            virtualRouter.stop();
            virtualRouter = null;
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        if (!status.get()) {
            return;
        }
        log.info("SdWanNode stopping");
        loopStatus.set(false);
        loopThread.interrupt();
        status.set(false);
        uninstall();
        log.info("SdWanNode stopped");
    }

    public void sendIpPacket(SDWanProtos.IpPacket ipPacket) {
        Ipv4Packet.Packet packet = Ipv4Packet.builder()
                .version((short) 4)
                .protocol(Ipv4Packet.Icmp)
                .srcIP(ipPacket.getSrcIP())
                .dstIP(ipPacket.getDstIP())
                .payload(ByteBufUtil.toByteBuf(ipPacket.getPayload().toByteArray()))
                .build();
        Ipv4Packet ipv4Packet = new Ipv4Packet(packet);
        IpLayerPacket ipLayerPacket = new IpLayerPacket(ipv4Packet.encode());
        sendIpPacket(ipLayerPacket);
    }

    public void sendIpPacket(IpLayerPacket ipLayerPacket) {
        virtualRouter.send(ipLayerPacket);
    }

    protected String processMacAddress(String hardwareAddress) {
        return hardwareAddress;
    }

    protected void onData(IpLayerPacket packet) {

    }

    public void signalAll() {
        lock.lock();
        try {
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void await(long time) {
        lock.lock();
        try {
            condition.await(time, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        } finally {
            lock.unlock();
        }
    }

    protected void fireEvent(Consumer<EventListener> consumer) {
        for (EventListener listener : eventListenerList) {
            consumer.accept(listener);
        }
    }

    @Override
    public void run() {
        boolean up = true;
        while (loopStatus.get()) {
            try {
                if (up) {
                    await(3000);
                    up = virtualRouter.isRunning();
                    if (up) {
                        continue;
                    }
                }
                if (!loopStatus.get()) {
                    return;
                }
                uninstall();
                install();
                up = virtualRouter.isRunning();
            } catch (ProcessCodeException e) {
                fireEvent(fire -> fire.onError(e.getCode()));
            } catch (InterruptedException e) {
            } catch (ProcessException e) {
                log.error(e.getMessage());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
