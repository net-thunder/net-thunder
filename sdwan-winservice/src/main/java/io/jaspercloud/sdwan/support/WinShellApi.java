package io.jaspercloud.sdwan.support;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;

public interface WinShellApi extends Library {

    WinShellApi INSTANCE = Native.load("Shell32", WinShellApi.class);

    Pointer ShellExecuteW(Pointer hwnd, WString lpOperation, WString lpFile, WString lpParameters, WString lpDirectory, int nShowCmd);
}
