package io.jaspercloud.sdwan.tun.osx;

import com.sun.jna.*;
import com.sun.jna.ptr.IntByReference;

public class OsxNativeApi {

    static {
        try {
            Native.register(OsxNativeApi.class, Platform.C_LIBRARY_NAME);
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static final String UTUN_CONTROL_NAME = "com.apple.net.utun_control";

    public static final int AF_SYSTEM = 32;
    public static final int SOCK_DGRAM = 2;
    public static final int SYSPROTO_CONTROL = 2;
    public static final int UTUN_OPT_IFNAME = 2;

    public static final int F_GETFL = 0x3;
    public static final int F_SETFL = 0x4;
    public static final int F_GETFD = 0x1;
    public static final int F_SETFD = 0x2;
    public static final int O_NONBLOCK = 0x4;
    public static final int FD_CLOEXEC = 0x1;

    public static final int PROC_PIDPATHINFO_MAXSIZE = 4096;

    public static final NativeLong CTLIOCGINFO = new NativeLong(0xc0644e03L);
    public static final NativeLong SIOCSIFMTU = new NativeLong(0x80206934L);

    public static native int geteuid() throws LastErrorException;

    public static native int getpid() throws LastErrorException;

    public static native int socket(int domain, int type, int protocol) throws LastErrorException;

    public static native int close(int fd) throws LastErrorException;

    public static native int ioctl(int fildes, NativeLong request, Structure argp) throws LastErrorException;

    public static native int fcntl(int socket, int cmd, int arg) throws LastErrorException;

    public static native int connect(int socket, Structure address, int address_len) throws LastErrorException;

    public static native int getsockopt(int socket, int level, int option_name, Structure option_value, IntByReference option_len) throws LastErrorException;

    public static native int read(int fd, byte[] buf, int nbytes) throws LastErrorException;

    public static native int write(int fd, byte[] buf, int nbytes) throws LastErrorException;

    public static native int proc_pidpath(int pid, byte[] buf, int size) throws LastErrorException;
}
