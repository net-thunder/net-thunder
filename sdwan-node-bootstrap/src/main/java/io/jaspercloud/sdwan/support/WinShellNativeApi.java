package io.jaspercloud.sdwan.support;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

public interface WinShellNativeApi extends Library {

    WinShellNativeApi INSTANCE = Native.load("Shell32", WinShellNativeApi.class);

    Pointer ShellExecuteW(Pointer hwnd, WString lpOperation, WString lpFile, WString lpParameters, WString lpDirectory, int nShowCmd);
}
