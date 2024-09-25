package io.jaspercloud.sdwan.support;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.W32APIOptions;

public interface Kernel32NativeApi extends Library {

    Kernel32NativeApi INSTANCE = Native.load("kernel32", Kernel32NativeApi.class, W32APIOptions.DEFAULT_OPTIONS);

    int GetModuleFileNameA(Pointer hModule, byte[] lpFilename, int nSize);
}
