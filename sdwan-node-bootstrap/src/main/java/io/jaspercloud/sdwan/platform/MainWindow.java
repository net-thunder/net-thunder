package io.jaspercloud.sdwan.platform;

import com.sun.jna.platform.win32.Winsvc;
import io.jaspercloud.sdwan.node.ConfigSystem;
import io.jaspercloud.sdwan.node.SdWanNodeConfig;
import io.jaspercloud.sdwan.support.WinShell;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainWindow extends JFrame {

    @Override
    protected void frameInit() {
        super.frameInit();
        setTitle("sdwan-node");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        JPanel panel = new JPanel();
        // 创建要居中的 JPanel
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        // 添加垂直空白区域以推中心面板到中间
        panel.add(Box.createVerticalGlue());
        panel.add(centerPanel);
        panel.add(Box.createVerticalGlue());
        initPanel(centerPanel);
        getContentPane().add(panel);
        centerWindow(this);
    }

    private void initPanel(JPanel panel) {
        JLabel statusLabel = new JLabel("status: not install");
        panel.add(statusLabel);
        JButton startBtn = new JButton("start");
        panel.add(startBtn);
        JButton stopBtn = new JButton("stop");
        panel.add(stopBtn);
        JButton uninstallBtn = new JButton("uninstall");
        panel.add(uninstallBtn);
        startBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    shellExecuteService("start");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        stopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    shellExecuteService("stop");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        uninstallBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    shellExecuteService("uninstall");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    try {
                        String executeJarPath = WinSvcUtil.getExecuteJarPath();
                        String parentPath = new File(executeJarPath).getParent();
                        String configPath = new File(parentPath, "application.yaml").getAbsolutePath();
                        SdWanNodeConfig config = new ConfigSystem().init(configPath);
                        String serviceName = config.getTunName();
                        int s = WinSvcUtil.queryServiceStatus(serviceName);
                        switch (s) {
                            case -1: {
                                statusLabel.setText("status: not install");
                                break;
                            }
                            case Winsvc.SERVICE_RUNNING: {
                                statusLabel.setText("status: running");
                                break;
                            }
                            case Winsvc.SERVICE_START:
                            case Winsvc.SERVICE_START_PENDING: {
                                statusLabel.setText("status: starting");
                                break;
                            }
                            case Winsvc.SERVICE_STOP:
                            case Winsvc.SERVICE_STOP_PENDING: {
                                statusLabel.setText("status: stopping");
                                break;
                            }
                            case Winsvc.SERVICE_STOPPED: {
                                statusLabel.setText("status: stopped");
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void shellExecuteService(String action) throws Exception {
        String javaPath = WinSvcUtil.getJavaPath();
        List<String> argList = new ArrayList<>();
        argList.add("-jar");
        String executeJarPath = WinSvcUtil.getExecuteJarPath();
        String parentPath = new File(executeJarPath).getParent();
        String configPath = new File(parentPath, "application.yaml").getAbsolutePath();
        String logPath = new File(parentPath, "app.log").getAbsolutePath();
        argList.add(executeJarPath);
        argList.add("-t");
        argList.add(action);
        argList.add("-n");
        SdWanNodeConfig config = new ConfigSystem().init(configPath);
        String name = config.getTunName();
        argList.add(name);
        argList.add("-c");
        argList.add(new File(configPath).getAbsolutePath());
        argList.add("-log");
        argList.add(new File(logPath).getAbsolutePath());
        String args = StringUtils.join(argList, " ");
        System.out.println(String.format("ShellExecuteW %s %s", javaPath, args));
        WinShell.ShellExecuteW(javaPath, args, null, WinShell.SW_HIDE);
    }

    private void centerWindow(JFrame frame) {
        // 获取屏幕尺寸
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension screenSize = toolkit.getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        // 获取窗口尺寸
        Dimension windowSize = frame.getSize();
        int windowWidth = windowSize.width;
        int windowHeight = windowSize.height;
        // 计算窗口居中位置
        int x = (screenWidth - windowWidth) / 2;
        int y = (screenHeight - windowHeight) / 2;
        // 设置窗口位置
        frame.setLocation(x, y);
    }
}
