package io.jaspercloud.sdwan.support;

import com.sun.jna.WString;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Win32Exception;

public final class WinShell {

    private WinShell() {

    }

    public static final int SW_HIDE = 0;
    public static final int SW_NORMAL = 1;
    public static final int SW_SHOWNORMAL = 1;
    public static final int SW_SHOWMINIMIZED = 2;
    public static final int SW_SHOWMAXIMIZED = 3;
    public static final int SW_MAXIMIZE = 3;
    public static final int SW_SHOWNOACTIVATE = 4;
    public static final int SW_SHOW = 5;
    public static final int SW_MINIMIZE = 6;
    public static final int SW_SHOWMINNOACTIVE = 7;
    public static final int SW_SHOWNA = 8;
    public static final int SW_RESTORE = 9;
    public static final int SW_SHOWDEFAULT = 10;
    public static final int SW_FORCEMINIMIZE = 11;

    public static void ShellExecuteW(String lpFile, String lpParameters, String lpDirectory, int nShowCmd) {
        WinShellNativeApi.INSTANCE.ShellExecuteW(
                null,
                new WString("runas"),
                new WString(lpFile),
                null != lpParameters ? new WString(lpParameters) : null,
                null != lpDirectory ? new WString(lpDirectory) : null,
                nShowCmd
        );
        int code = Kernel32.INSTANCE.GetLastError();
        if (0 != code) {
            throw new Win32Exception(code);
        }
    }
}
