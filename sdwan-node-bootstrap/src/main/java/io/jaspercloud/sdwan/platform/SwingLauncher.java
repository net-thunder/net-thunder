package io.jaspercloud.sdwan.platform;

import io.jaspercloud.sdwan.node.support.PathApi;
import io.jaspercloud.sdwan.util.GraalVM;
import io.jaspercloud.sdwan.util.NetworkInterfaceUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.stream.Collectors;

public class SwingLauncher {

    private static final Logger logger = LoggerFactory.getLogger(SwingLauncher.class);

    private static ClassLoader classLoader = SwingLauncher.class.getClassLoader();

    public static void startup(CommandLine cmd) throws Exception {
        if (GraalVM.isNative()) {
            String dir = PathApi.getExecutableParent();
            logger.info("dir: {}", dir);
            System.getProperties().put("java.home", dir);
        }
        MainJFrame frame = new MainJFrame();
        frame.setVisible(true);
        //tray
        SystemTray tray = SystemTray.getSystemTray();
        PopupMenu menu = new PopupMenu();
        String title = "net-thunder";
        BufferedImage icon = ImageIO.read(classLoader.getResourceAsStream("ui/favicon.ico"));
        TrayIcon trayIcon = new TrayIcon(icon, title, menu);
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    SwingUtilities.invokeLater(() -> {
                        frame.setVisible(true);
                    });
                }
            }
        });
        tray.add(trayIcon);
    }

    public static class MainJFrame extends JFrame implements ActionListener {

        private JComboBox netSelect;

        public MainJFrame() throws Exception {
            super();
            String title = "net-thunder";
            BufferedImage icon = ImageIO.read(classLoader.getResourceAsStream("ui/favicon.ico"));
            setTitle(title);
            setIconImage(icon);
            setSize(400, 350);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setResizable(false);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    int result = JOptionPane.showConfirmDialog(null,
                            "是否继续后台运行?",
                            "提示",
                            JOptionPane.YES_NO_OPTION);
                    switch (result) {
                        case JOptionPane.YES_OPTION: {
                            SwingUtilities.invokeLater(() -> {
                                setVisible(false);
                            });
                            break;
                        }
                        case JOptionPane.NO_OPTION: {
                            System.exit(0);
                            break;
                        }
                    }
                }
            });
            // 创建主面板
            add(new JPanel() {
                {
                    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
                    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
                    add(new JPanel() {
                        {
                            setLayout(new GridBagLayout());
                            GridBagConstraints grid = new GridBagConstraints();
                            grid.fill = GridBagConstraints.HORIZONTAL;
                            grid.insets = new Insets(5, 10, 5, 10);
                            //status
                            grid.gridx = 0;
                            grid.gridy = 0;
                            add(new JLabel("当前状态:"), grid);
                            grid.gridx = 1;
                            grid.gridy = 0;
                            JLabel statusLab = new JLabel("已断开");
                            add(statusLab, grid);
                            //vip
                            grid.gridx = 0;
                            grid.gridy = 1;
                            add(new JLabel("本地虚拟IP:"), grid);
                            grid.gridx = 1;
                            grid.gridy = 1;
                            JLabel vipLab = new JLabel("-");
                            add(vipLab, grid);
                            //netSelect
                            grid.gridx = 0;
                            grid.gridy = 2;
                            add(new JLabel("出口IP:"), grid);
                            grid.gridx = 1;
                            grid.gridy = 2;
                            netSelect = new JComboBox();
                            netSelect.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    System.out.println(netSelect.getSelectedItem());
                                }
                            });
                            refreshNetList();
                            add(netSelect, grid);
                        }
                    });
                    add(new JPanel() {
                        {
                            setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
                            JButton startBtn = new JButton("启动");
                            startBtn.setActionCommand("start");
                            JButton stopBtn = new JButton("停止");
                            stopBtn.setActionCommand("stop");
                            startBtn.addActionListener(MainJFrame.this);
                            stopBtn.addActionListener(MainJFrame.this);
                            add(startBtn);
                            add(stopBtn);
                        }
                    });
                }
            });
        }

        private void refreshNetList() {
            try {
                List<String> netList = NetworkInterfaceUtil.findIpv4NetworkInterfaceInfo(true)
                        .stream()
                        .filter(e -> StringUtils.isNotEmpty(e.getHardwareAddress()))
                        .map(e -> e.getIp())
                        .collect(Collectors.toList());
                netSelect.removeAllItems();
                netList.forEach(item -> {
                    netSelect.addItem(item);
                });
                if (!netList.isEmpty()) {
                    netSelect.setSelectedIndex(0);
                }
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if ("start".equals(e.getActionCommand())) {

            } else if ("stop".equals(e.getActionCommand())) {

            }
        }
    }
}
