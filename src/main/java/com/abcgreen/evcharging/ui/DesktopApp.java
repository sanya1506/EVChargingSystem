package com.abcgreen.evcharging.ui;

import javax.swing.SwingUtilities;

import com.abcgreen.evcharging.util.AppLogger;

/**
 * DESKTOP APPLICATION ENTRY POINT (Swing).
 * Right-click > Run As > Java Application.
 */
public class DesktopApp {

    public static void main(String[] args) {
        AppLogger.info("Desktop application starting");
        SwingUtilities.invokeLater(() -> {
            UiKit.installLookAndFeel();
            new LoginFrame().setVisible(true);
        });
    }
}
