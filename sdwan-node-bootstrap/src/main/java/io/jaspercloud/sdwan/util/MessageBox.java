package io.jaspercloud.sdwan.util;

import javax.swing.*;

public class MessageBox {

    public static void showError(String msg) {
        JOptionPane.showMessageDialog(null, msg, "提示", JOptionPane.ERROR_MESSAGE);
    }
}
