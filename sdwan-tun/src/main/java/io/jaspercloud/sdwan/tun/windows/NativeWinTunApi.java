package io.jaspercloud.sdwan.tun.windows;

import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.WinNT;
import io.jaspercloud.sdwan.util.SystemFile;

public class NativeWinTunApi {

    static {
        try {
            String arch = System.getProperty("os.arch");
            String file = "wintun.dll";
            SystemFile.writeClassFile(String.format("META-INF/native/wintun/%s/%s", arch, file), file);
            Native.register(NativeWinTunApi.class, "wintun");
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final int ERROR_NO_MORE_ITEMS = 259;
    public static final int WINTUN_MIN_RING_CAPACITY = 0x20000;
    public static final int WINTUN_MAX_RING_CAPACITY = 0x4000000;

    public static native int WintunGetRunningDriverVersion() throws LastErrorException;

    public static native WinNT.HANDLE WintunOpenAdapter(WString Name, WString TunnelType) throws LastErrorException;

    public static native WinNT.HANDLE WintunCreateAdapter(WString Name, WString TunnelType, String RequestedGUID) throws LastErrorException;

    public static native void WintunCloseAdapter(WinNT.HANDLE Adapter) throws LastErrorException;

    public static native WinNT.HANDLE WintunStartSession(WinNT.HANDLE Adapter, int Capacity) throws LastErrorException;

    public static native void WintunEndSession(WinNT.HANDLE Session) throws LastErrorException;

    public static native Pointer WintunReceivePacket(WinNT.HANDLE Session, Pointer PacketSize) throws LastErrorException;

    public static native void WintunReleaseReceivePacket(WinNT.HANDLE Session, Pointer Packet) throws LastErrorException;

    public static native Pointer WintunAllocateSendPacket(WinNT.HANDLE Session, long PacketSize) throws LastErrorException;

    public static native void WintunSendPacket(WinNT.HANDLE Session, Pointer Packet) throws LastErrorException;

    public static native WinNT.HANDLE WintunGetReadWaitEvent(WinNT.HANDLE Session) throws LastErrorException;

    public static native void WintunGetAdapterLUID(WinNT.HANDLE Adapter, Pointer Luid) throws LastErrorException;
}
