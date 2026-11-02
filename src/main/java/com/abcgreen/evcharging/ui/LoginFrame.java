package com.abcgreen.evcharging.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import com.abcgreen.evcharging.exception.AuthenticationException;
import com.abcgreen.evcharging.model.User;
import com.abcgreen.evcharging.service.AuthService;
import com.abcgreen.evcharging.service.QueueProcessor;
import com.abcgreen.evcharging.service.SecurityContext;
import com.abcgreen.evcharging.util.DBConnection;

/** Login screen for administrators and operators (FR-01). */
public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private final JTextField username = new JTextField();
    private final JPasswordField password = new JPasswordField();
    private final JLabel message = new JLabel(" ");
    private final JButton loginButton = UiKit.primaryButton("Sign in");
    private final AuthService authService = new AuthService();

    public LoginFrame() {
        super("EV Charging Network Manager - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 540);
        setLocationRelativeTo(null);
        setResizable(false);
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(new BrandPanel());
        root.add(formPanel());
        setContentPane(root);
        getRootPane().setDefaultButton(loginButton);
        checkDatabase();
    }

    private JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);

        JLabel title = new JLabel("Welcome back");
        title.setFont(UiKit.TITLE);
        JLabel sub = new JLabel("Sign in to the operations console");
        sub.setForeground(UiKit.MUTED);
        c.gridy = 0;
        p.add(title, c);
        c.gridy = 1;
        c.insets = new Insets(0, 0, 24, 0);
        p.add(sub, c);
        c.insets = new Insets(4, 0, 4, 0);
        c.gridy = 2;
        p.add(new JLabel("Username or email"), c);
        c.gridy = 3;
        username.setPreferredSize(new Dimension(260, 36));
        username.putClientProperty("JTextField.placeholderText", "admin");
        p.add(username, c);
        c.gridy = 4;
        p.add(new JLabel("Password"), c);
        c.gridy = 5;
        password.setPreferredSize(new Dimension(260, 36));
        password.putClientProperty("JTextField.showRevealButton", true);
        p.add(password, c);
        c.gridy = 6;
        message.setForeground(UiKit.RED);
        message.setFont(UiKit.SMALL);
        p.add(message, c);
        c.gridy = 7;
        loginButton.setPreferredSize(new Dimension(260, 40));
        p.add(loginButton, c);
        c.gridy = 8;
        c.insets = new Insets(18, 0, 0, 0);
        JLabel hint = new JLabel("<html>Demo: <b>admin</b> / Admin@123 &nbsp; or &nbsp; <b>operator</b> / Operator@123</html>");
        hint.setForeground(UiKit.MUTED);
        hint.setFont(UiKit.SMALL);
        p.add(hint, c);

        loginButton.addActionListener(e -> doLogin());
        return p;
    }

    private void checkDatabase() {
        UiKit.async(this, DBConnection::isAvailable, ok -> {
            if (!ok) {
                message.setText("<html>Cannot connect to MySQL. Check that MySQL is running and the password in db.properties.</html>");
            }
        });
    }

    private void doLogin() {
        String user = username.getText();
        String pass = new String(password.getPassword());
        loginButton.setEnabled(false);
        message.setText(" ");
        UiKit.async(this, () -> {
            try {
                User u = authService.login(user, pass);
                if (!u.getRole().isStaff()) {
                    throw new AuthenticationException("Customers please use the web portal (http://localhost:8080).");
                }
                return u;
            } finally {
                javax.swing.SwingUtilities.invokeLater(() -> loginButton.setEnabled(true));
            }
        }, u -> {
            SecurityContext.setDesktopUser(u);
            QueueProcessor.start();
            new MainFrame(u).setVisible(true);
            dispose();
        });
    }

    /** Left side: painted gradient with product name. */
    private static class BrandPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        BrandPanel() {
            setLayout(new BorderLayout());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(6, 95, 70), getWidth(), getHeight(), new Color(16, 185, 129)));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillOval(getWidth() - 220, -80, 320, 320);
            g2.fillOval(-120, getHeight() - 200, 300, 300);
            // lightning bolt logo
            int bx = 50;
            int by = 90;
            g2.setColor(new Color(253, 224, 71));
            g2.fillPolygon(new int[] { bx + 22, bx + 4, bx + 18, bx + 12, bx + 34, bx + 20 }, new int[] { by, by + 30, by + 30, by + 56, by + 22, by + 22 }, 6);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 30));
            g2.drawString("Smart EV Charging", 50, 200);
            g2.drawString("Network Manager", 50, 240);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            g2.setColor(new Color(209, 250, 229));
            g2.drawString("ABC Green Energy Solutions Pvt. Ltd.", 50, 280);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String[] lines = { "- Stations, chargers and live availability", "- Bookings, queues and charging sessions",
                "- Billing, demand analytics and reports" };
            for (int i = 0; i < lines.length; i++) {
                g2.drawString(lines[i], 50, 340 + i * 26);
            }
            g2.setColor(new Color(209, 250, 229, 180));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString("Inspired by the Shell.ai EV Charging Network Challenge", 50, getHeight() - 30);
        }
    }
}
