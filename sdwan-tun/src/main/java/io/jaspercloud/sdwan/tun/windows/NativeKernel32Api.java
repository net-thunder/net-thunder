package io.jaspercloud.sdwan.tun.windows;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.win32.StdCallLibrary;

public interface NativeKernel32Api extends StdCallLibrary {

    NativeKernel32Api INSTANCE = Native.load("kernel32", NativeKernel32Api.class);

    int INFINITE = 0xFFFFFFFF;

    int WaitForSingleObject(WinNT.HANDLE hHandle, int dwMilliseconds);
}