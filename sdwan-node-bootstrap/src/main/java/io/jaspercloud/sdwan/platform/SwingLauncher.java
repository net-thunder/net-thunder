package io.jaspercloud.sdwan.platform;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class SwingLauncher {

    private static final Logger logger = LoggerFactory.getLogger(SwingLauncher.class);

    public static void startup(CommandLine cmd) throws Exception {
        ClassLoader classLoader = SwingLauncher.class.getClassLoader();
        String title = "net-thunder";
        BufferedImage icon = ImageIO.read(classLoader.getResourceAsStream("ui/favicon.ico"));
        JFrame frame = new JFrame();
        frame.setTitle(title);
        frame.setIconImage(icon);
        frame.setSize(400, 350);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setResizable(false);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(null,
                        "是否继续后台运行?",
                        "提示",
                        JOptionPane.YES_NO_OPTION);
                switch (result) {
                    case JOptionPane.YES_OPTION: {
                        SwingUtilities.invokeLater(() -> {
                            frame.setVisible(false);
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
        frame.add(new JPanel() {
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
                    }
                });
                add(new JPanel() {
                    {
                        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
                        JButton startBtn = new JButton("启动");
                        JButton stopBtn = new JButton("停止");
                        add(startBtn);
                        add(stopBtn);
                    }
                });
            }
        });
        frame.setVisible(true);
        //tray
        SystemTray tray = SystemTray.getSystemTray();
        PopupMenu menu = new PopupMenu();
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
}
