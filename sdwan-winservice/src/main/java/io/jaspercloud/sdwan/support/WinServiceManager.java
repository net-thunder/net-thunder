package io.jaspercloud.sdwan.support;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;

import java.io.Closeable;

public class WinServiceManager implements Closeable {

    private Winsvc.SC_HANDLE handle;
    private int access;

    private WinServiceManager(Winsvc.SC_HANDLE handle, int access) {
        this.handle = handle;
        this.access = access;
    }

    public static WinServiceManager openExecute() {
        return open(WinNT.GENERIC_EXECUTE);
    }

    public static WinServiceManager openManager() {
        return open(Winsvc.SC_MANAGER_ALL_ACCESS);
    }

    public static WinServiceManager open(int access) {
        Winsvc.SC_HANDLE handle = Advapi32.INSTANCE.OpenSCManager(null, null, access);
        if (handle == null) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
        return new WinServiceManager(handle, access);
    }

    @Override
    public void close() {
        boolean ret = Advapi32.INSTANCE.CloseServiceHandle(handle);
        if (!ret) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
    }

    public static void startServiceCtrlDispatcher(CommandLine cmd, ServiceProcHandler procHandler) {
        Winsvc.SERVICE_TABLE_ENTRY entry = new Winsvc.SERVICE_TABLE_ENTRY();
        entry.lpServiceName = cmd.getOptionValue("n");
        entry.lpServiceProc = new WinServiceProc(cmd, procHandler);
        boolean ret = Advapi32.INSTANCE.StartServiceCtrlDispatcher((Winsvc.SERVICE_TABLE_ENTRY[]) entry.toArray(2));
        if (!ret) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
    }

    public WinService openService(String serviceName) {
        Winsvc.SC_HANDLE serviceHandler = Advapi32.INSTANCE.OpenService(handle, serviceName, access);
        if (serviceHandler == null) {
            return null;
        }
        return new WinService(serviceHandler);
    }

    public void createService(String serviceName, String path) {
        Winsvc.SC_HANDLE serviceHandler = Advapi32.INSTANCE.CreateService(
                handle,
                serviceName,
                serviceName,
                Winsvc.SERVICE_ALL_ACCESS,
                WinNT.SERVICE_WIN32_OWN_PROCESS,
                WinNT.SERVICE_DEMAND_START,
                WinNT.SERVICE_ERROR_NORMAL,
                path,
                null,
                null,
                "\0",
                null,
                null);
        if (serviceHandler == null) {
            throw new IllegalStateException("Failed to install service ");
        }
        boolean ret = Advapi32.INSTANCE.CloseServiceHandle(serviceHandler);
        if (!ret) {
            throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
        }
    }

    @Slf4j
    public static class WinServiceProc implements Winsvc.SERVICE_MAIN_FUNCTION {

        private CommandLine cmd;
        private ServiceProcHandler serviceProcHandler;
        private Winsvc.SERVICE_STATUS_HANDLE handle;

        public WinServiceProc(CommandLine cmd, ServiceProcHandler serviceProcHandler) {
            this.cmd = cmd;
            this.serviceProcHandler = serviceProcHandler;
        }

        @Override
        public void callback(int dwArgc, Pointer lpszArgv) {
            log.info("WinServiceProc: {}", dwArgc);
            String serviceName = cmd.getOptionValue("n");
            log.info("WinServiceProc: RegisterServiceCtrlHandlerEx={}", serviceName);
            handle = Advapi32.INSTANCE.RegisterServiceCtrlHandlerEx(serviceName, serviceProcHandler, null);
            if (null == handle) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
            serviceProcHandler.setHandle(handle);
            serviceProcHandler.reportStatus(Winsvc.SERVICE_START_PENDING, WinError.NO_ERROR, 0);
            try {
                serviceProcHandler.start();
                serviceProcHandler.reportStatus(Winsvc.SERVICE_RUNNING, WinError.NO_ERROR, 0);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                serviceProcHandler.reportStatus(Winsvc.SERVICE_STOPPED, WinError.ERROR_TIMEOUT, 0);
            }
        }
    }

    @Slf4j
    public static abstract class ServiceProcHandler implements Winsvc.HandlerEx {

        private Winsvc.SERVICE_STATUS_HANDLE handle;

        public void setHandle(Winsvc.SERVICE_STATUS_HANDLE handle) {
            this.handle = handle;
        }

        @Override
        public int callback(int dwControl, int dwEventType, Pointer lpEventData, Pointer lpContext) {
            log.info("dwControl={}, dwEventType={}", dwControl, dwEventType);
            switch (dwControl) {
                case Winsvc.SERVICE_CONTROL_STOP:
                case Winsvc.SERVICE_CONTROL_SHUTDOWN: {
                    reportStatus(Winsvc.SERVICE_STOP_PENDING, WinError.NO_ERROR, 0);
                    try {
                        stop();
                        reportStatus(Winsvc.SERVICE_STOPPED, WinError.NO_ERROR, 0);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        reportStatus(Winsvc.SERVICE_STOPPED, WinError.ERROR_TIMEOUT, 0);
                    }
                    break;
                }
            }
            return WinError.NO_ERROR;
        }

        public abstract void start() throws Exception;

        public abstract void stop() throws Exception;

        public void reportStatus(int status, int win32ExitCode, int waitHint) {
            Winsvc.SERVICE_STATUS serviceStatus = new Winsvc.SERVICE_STATUS();
            serviceStatus.dwServiceType = WinNT.SERVICE_WIN32_OWN_PROCESS;
            serviceStatus.dwControlsAccepted = status == Winsvc.SERVICE_START_PENDING ? 0
                    : (Winsvc.SERVICE_ACCEPT_STOP
                    | Winsvc.SERVICE_ACCEPT_SHUTDOWN
                    | Winsvc.SERVICE_CONTROL_PAUSE
                    | Winsvc.SERVICE_CONTROL_CONTINUE);
            serviceStatus.dwWin32ExitCode = win32ExitCode;
            serviceStatus.dwWaitHint = waitHint;
            serviceStatus.dwCurrentState = status;
            boolean ret = Advapi32.INSTANCE.SetServiceStatus(handle, serviceStatus);
            if (!ret) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
        }
    }

    public static class WinService implements Closeable {

        private Winsvc.SC_HANDLE handle;

        public WinService(Winsvc.SC_HANDLE handle) {
            this.handle = handle;
        }

        public int status() {
            Winsvc.SERVICE_STATUS status = new Winsvc.SERVICE_STATUS();
            boolean ret = Advapi32.INSTANCE.QueryServiceStatus(handle, status);
            if (!ret) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
            return status.dwCurrentState;
        }

        public void start() {
            boolean ret = Advapi32.INSTANCE.StartService(handle, 0, null);
            if (!ret) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
        }

        public void stop() {
            Winsvc.SERVICE_STATUS serviceStatus = new Winsvc.SERVICE_STATUS();
            boolean ret = Advapi32.INSTANCE.ControlService(handle, Winsvc.SERVICE_CONTROL_STOP, serviceStatus);
            if (!ret) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
        }

        public void deleteService() {
            boolean ret = Advapi32.INSTANCE.DeleteService(handle);
            if (!ret) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
        }

        @Override
        public void close() {
            boolean ret = Advapi32.INSTANCE.CloseServiceHandle(handle);
            if (!ret) {
                throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
            }
        }
    }
}
