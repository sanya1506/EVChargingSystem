package com.abcgreen.evcharging.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.util.AppLogger;
import com.formdev.flatlaf.FlatLightLaf;

/** Shared colours, fonts, factories and helpers for the Swing user interface. */
public final class UiKit {

    public static final Color PRIMARY = new Color(5, 150, 105);
    public static final Color PRIMARY_DARK = new Color(4, 120, 87);
    public static final Color SIDEBAR = new Color(15, 23, 42);
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);
    public static final Color BACKGROUND = new Color(241, 245, 249);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(15, 23, 42);
    public static final Color MUTED = new Color(100, 116, 139);
    public static final Color BLUE = new Color(37, 99, 235);
    public static final Color AMBER = new Color(217, 119, 6);
    public static final Color RED = new Color(220, 38, 38);
    public static final Color PURPLE = new Color(124, 58, 237);
    public static final Color BORDER = new Color(226, 232, 240);

    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private UiKit() {
    }

    public static void installLookAndFeel() {
        FlatLightLaf.setup();
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Table.rowHeight", 28);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("TableHeader.height", 32);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("defaultFont", BODY);
    }

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFont(BODY.deriveFont(Font.BOLD));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton button(String text) {
        JButton b = new JButton(text);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JButton dangerButton(String text) {
        JButton b = button(text);
        b.setForeground(RED);
        return b;
    }

    /** Page header with title and subtitle. */
    public static JComponent header(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(TITLE);
        t.setForeground(TEXT);
        JLabel s = new JLabel(subtitle);
        s.setFont(BODY);
        s.setForeground(MUTED);
        p.add(t, BorderLayout.NORTH);
        p.add(s, BorderLayout.SOUTH);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        return p;
    }

    /** White rounded card with padding. */
    public static JPanel card(JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        p.add(content);
        return p;
    }

    public static JPanel toolbar(Component... components) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setOpaque(false);
        for (Component c : components) {
            p.add(c);
        }
        return p;
    }

    /** Standard page: header on top, content in the centre, light background. */
    public static JPanel page(String title, String subtitle, JComponent content) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BACKGROUND);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        p.add(header(title, subtitle), BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public static void error(Component parent, Throwable e) {
        String msg = e.getMessage() == null ? e.toString() : e.getMessage();
        if (!(e instanceof EVChargingException)) {
            AppLogger.error("UI error", e);
        }
        JOptionPane.showMessageDialog(parent, wrap(msg), e instanceof EVChargingException ? "Please check" : "Error",
                e instanceof EVChargingException ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent, wrap(msg), "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String msg) {
        return JOptionPane.showConfirmDialog(parent, wrap(msg), "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private static String wrap(String msg) {
        return "<html><body style='width:340px'>" + msg.replace("<", "&lt;").replace("\n", "<br>") + "</body></html>";
    }

    /** A task that may throw (database access, business rules). */
    @FunctionalInterface
    public interface Task<T> {
        T run() throws Exception;
    }

    /**
     * Runs a task on a background thread (SwingWorker) so the UI never freezes,
     * then delivers the result on the Event Dispatch Thread.
     */
    public static <T> void async(Component parent, Task<T> task, Consumer<T> onSuccess) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.run();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (java.util.concurrent.ExecutionException e) {
                    error(parent, e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    /** Runs a task synchronously and shows any error; returns null on failure. */
    public static <T> T call(Component parent, Task<T> task) {
        try {
            return task.run();
        } catch (Exception e) {
            error(parent, e);
            return null;
        }
    }

    public static String fmt(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME);
    }

    public static String fmt(LocalDate d) {
        return d == null ? "" : d.format(DATE);
    }

    public static String money(double v) {
        return String.format("Rs %,.2f", v);
    }

    public static <T> Supplier<T> constant(T value) {
        return () -> value;
    }
}
