package io.jaspercloud.sdwan.tranport;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;
import org.junit.jupiter.api.Test;

public class TapTest {

    @Test
    public void test() throws Exception {
        String tapDeviceName = "\\\\.\\Global\\" + "{D29D6905-FC8C-49D8-88E7-AAE08BD6054D}" + ".tap"; // Replace with your tap device name
        Kernel32 kernel32 = Kernel32.INSTANCE;
        WinNT.HANDLE handle = kernel32.CreateFile(tapDeviceName,
                WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE,
                null,
                WinNT.OPEN_EXISTING,
                0,
                null);
        boolean eq = handle != WinBase.INVALID_HANDLE_VALUE;
        {
            int TAP_WIN_IOCTL_GET_MAC = WinioctlUtil.CTL_CODE(
                    Winioctl.FILE_DEVICE_UNKNOWN,
                    1,
                    Winioctl.METHOD_BUFFERED,
                    Winioctl.FILE_ANY_ACCESS);
            Memory input = new Memory(6);
            Memory output = new Memory(6);
            IntByReference bytesReturned = new IntByReference();
            boolean success = Kernel32.INSTANCE.DeviceIoControl(handle, TAP_WIN_IOCTL_GET_MAC,
                    input, (int) input.size(), output, (int) output.size(), bytesReturned, null);
            System.out.println();
        }
        {
            int TAP_WIN_IOCTL_GET_VERSION = WinioctlUtil.CTL_CODE(
                    Winioctl.FILE_DEVICE_UNKNOWN,
                    2,
                    Winioctl.METHOD_BUFFERED,
                    Winioctl.FILE_ANY_ACCESS);
            Memory input = new Memory(128);
            Memory output = new Memory(128);
            IntByReference bytesReturned = new IntByReference();
            boolean success = Kernel32.INSTANCE.DeviceIoControl(handle, TAP_WIN_IOCTL_GET_VERSION,
                    input, (int) input.size(), output, (int) output.size(), bytesReturned, null);
            System.out.println(String.format("%s.%s.%s",
                    output.getInt(0 * 4), output.getInt(1 * 4), output.getInt(2 * 4)));
        }
        {
            int TAP_WIN_IOCTL_GET_MTU = WinioctlUtil.CTL_CODE(
                    Winioctl.FILE_DEVICE_UNKNOWN,
                    3,
                    Winioctl.METHOD_BUFFERED,
                    Winioctl.FILE_ANY_ACCESS);
            WinDef.ULONGByReference output = new WinDef.ULONGByReference();
            IntByReference bytesReturned = new IntByReference();
            boolean success = Kernel32.INSTANCE.DeviceIoControl(handle, TAP_WIN_IOCTL_GET_MTU,
                    null, 0, output.getPointer(), WinDef.ULONG.SIZE, bytesReturned, null);
            System.out.println(output.getValue().intValue());
        }
//        {
//            int TAP_WIN_IOCTL_GET_INFO = WinioctlUtil.CTL_CODE(
//                    Winioctl.FILE_DEVICE_UNKNOWN,
//                    4,
//                    Winioctl.METHOD_BUFFERED,
//                    Winioctl.FILE_ANY_ACCESS);
//            WinDef.DWORDByReference output = new WinDef.DWORDByReference();
//            IntByReference bytesReturned = new IntByReference();
//            boolean success = Kernel32.INSTANCE.DeviceIoControl(handle, TAP_WIN_IOCTL_GET_INFO,
//                    null, 0, output.getPointer(), WinDef.DWORD.SIZE, bytesReturned, null);
//            System.out.println();
//        }
//        {
//            int TAP_WIN_IOCTL_GET_LOG_LINE = WinioctlUtil.CTL_CODE(
//                    Winioctl.FILE_DEVICE_UNKNOWN,
//                    8,
//                    Winioctl.METHOD_BUFFERED,
//                    Winioctl.FILE_ANY_ACCESS);
//            Memory memory = new Memory(1024);
//            IntByReference bytesReturned = new IntByReference();
//            boolean success = Kernel32.INSTANCE.DeviceIoControl(handle, TAP_WIN_IOCTL_GET_LOG_LINE,
//                    null, 0, memory, (int) memory.size(), bytesReturned, null);
//            System.out.println();
//        }
        {
            int TAP_WIN_IOCTL_SET_MEDIA_STATUS = WinioctlUtil.CTL_CODE(
                    Winioctl.FILE_DEVICE_UNKNOWN,
                    6,
                    Winioctl.METHOD_BUFFERED,
                    Winioctl.FILE_ANY_ACCESS);
            WinDef.ULONGByReference status = new WinDef.ULONGByReference();
            status.setValue(new WinDef.ULONG(1));
            IntByReference bytesReturned = new IntByReference();
            boolean success = Kernel32.INSTANCE.DeviceIoControl(handle, TAP_WIN_IOCTL_SET_MEDIA_STATUS,
                    status.getPointer(), WinDef.ULONG.SIZE, status.getPointer(), WinDef.ULONG.SIZE, bytesReturned, null);
            System.out.println();
        }
    }

}
