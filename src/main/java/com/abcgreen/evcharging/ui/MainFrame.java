package com.abcgreen.evcharging.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.abcgreen.evcharging.model.User;
import com.abcgreen.evcharging.service.NotificationService;
import com.abcgreen.evcharging.service.QueueProcessor;
import com.abcgreen.evcharging.service.SecurityContext;
import com.abcgreen.evcharging.ui.panels.BillingPanel;
import com.abcgreen.evcharging.ui.panels.ChargingPanel;
import com.abcgreen.evcharging.ui.panels.CustomersPanel;
import com.abcgreen.evcharging.ui.panels.DashboardPanel;
import com.abcgreen.evcharging.ui.panels.DemandPanel;
import com.abcgreen.evcharging.ui.panels.MaintenancePanel;
import com.abcgreen.evcharging.ui.panels.QueuePanel;
import com.abcgreen.evcharging.ui.panels.ReportsPanel;
import com.abcgreen.evcharging.ui.panels.StationsPanel;
import com.abcgreen.evcharging.ui.panels.SystemPanel;
import com.abcgreen.evcharging.ui.panels.UsersPanel;
import com.abcgreen.evcharging.ui.panels.VehiclesPanel;

/** Main application window: sidebar navigation + card layout of screens. */
public class MainFrame extends JFrame {

    private static final long serialVersionUID = 1L;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    /** Screen name -> factory (screens are created lazily the first time they are opened). */
    private final Map<String, Supplier<JComponent>> factories = new LinkedHashMap<>();
    private final Map<String, JComponent> screens = new LinkedHashMap<>();
    private final Map<String, JLabel> navItems = new LinkedHashMap<>();
    private final JButton notificationsButton = new JButton("Notifications");
    private final JLabel clock = new JLabel();
    private final NotificationService notificationService = new NotificationService();
    private String current;

    public MainFrame(User user) {
        super("Smart EV Charging Network Manager - " + user.getFullName() + " (" + user.getRole().getLabel() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1200, 720));
        setSize(1366, 820);
        setLocationRelativeTo(null);

        factories.put("Dashboard", DashboardPanel::new);
        factories.put("Customers", CustomersPanel::new);
        factories.put("Vehicles", VehiclesPanel::new);
        factories.put("Stations & Chargers", StationsPanel::new);
        factories.put("Charging & Bookings", ChargingPanel::new);
        factories.put("Charging Queue", QueuePanel::new);
        factories.put("Billing & Payments", BillingPanel::new);
        factories.put("Maintenance", MaintenancePanel::new);
        factories.put("Demand Analytics", DemandPanel::new);
        factories.put("Reports", ReportsPanel::new);
        if (user.getRole().isAdmin()) {
            factories.put("User Management", UsersPanel::new);
            factories.put("System & Backup", SystemPanel::new);
        }

        JPanel root = new JPanel(new BorderLayout());
        root.add(sidebar(user), BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout());
        right.add(topBar(user), BorderLayout.NORTH);
        content.setBackground(UiKit.BACKGROUND);
        right.add(content, BorderLayout.CENTER);
        root.add(right, BorderLayout.CENTER);
        setContentPane(root);

        show("Dashboard");
        startTimers();
        // refresh the visible screen when the queue thread allocates chargers
        QueueProcessor.addListener(n -> SwingUtilities.invokeLater(() -> {
            refreshNotifications();
            refreshCurrent();
        }));
    }

    private JComponent sidebar(User user) {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(UiKit.SIDEBAR);
        side.setPreferredSize(new Dimension(230, 0));
        side.setBorder(BorderFactory.createEmptyBorder(18, 0, 12, 0));

        JLabel logo = new JLabel("  EV Charge Manager");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 17));
        logo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel company = new JLabel("   ABC Green Energy");
        company.setForeground(new Color(110, 231, 183));
        company.setFont(UiKit.SMALL);
        company.setAlignmentX(LEFT_ALIGNMENT);
        side.add(logo);
        side.add(company);
        side.add(Box.createVerticalStrut(20));

        for (String name : factories.keySet()) {
            JLabel item = new JLabel("     " + name);
            item.setOpaque(true);
            item.setBackground(UiKit.SIDEBAR);
            item.setForeground(new Color(203, 213, 225));
            item.setFont(UiKit.BODY);
            item.setMaximumSize(new Dimension(230, 40));
            item.setPreferredSize(new Dimension(230, 40));
            item.setAlignmentX(LEFT_ALIGNMENT);
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    show(name);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!name.equals(current)) {
                        item.setBackground(UiKit.SIDEBAR_HOVER);
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!name.equals(current)) {
                        item.setBackground(UiKit.SIDEBAR);
                    }
                }
            });
            navItems.put(name, item);
            side.add(item);
        }
        side.add(Box.createVerticalGlue());

        JLabel who = new JLabel("<html>&nbsp;&nbsp;&nbsp;" + user.getFullName() + "<br>&nbsp;&nbsp;&nbsp;<font color='#94a3b8'>"
                + user.getRole().getLabel() + "</font></html>");
        who.setForeground(Color.WHITE);
        who.setAlignmentX(LEFT_ALIGNMENT);
        side.add(who);
        side.add(Box.createVerticalStrut(8));
        JButton logout = UiKit.button("Logout");
        logout.setAlignmentX(LEFT_ALIGNMENT);
        logout.addActionListener(e -> logout());
        JPanel lp = new JPanel(new BorderLayout());
        lp.setOpaque(false);
        lp.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        lp.setMaximumSize(new Dimension(230, 40));
        lp.setAlignmentX(LEFT_ALIGNMENT);
        lp.add(logout);
        side.add(lp);
        return side;
    }

    private JComponent topBar(User user) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiKit.BORDER),
                BorderFactory.createEmptyBorder(8, 24, 8, 16)));
        JLabel hello = new JLabel("Welcome, " + user.getFullName() + "  |  Operations Console");
        hello.setForeground(UiKit.MUTED);
        bar.add(hello, BorderLayout.WEST);
        JPanel right = UiKit.toolbar(clock, notificationsButton);
        notificationsButton.addActionListener(e -> show("Dashboard"));
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    public void show(String name) {
        if (!screens.containsKey(name)) {
            JComponent screen = factories.get(name).get();
            screens.put(name, screen);
            content.add(screen, name);
        }
        if (current != null) {
            navItems.get(current).setBackground(UiKit.SIDEBAR);
            navItems.get(current).setForeground(new Color(203, 213, 225));
        }
        current = name;
        navItems.get(name).setBackground(UiKit.PRIMARY);
        navItems.get(name).setForeground(Color.WHITE);
        cards.show(content, name);
        refreshCurrent();
    }

    private void refreshCurrent() {
        JComponent screen = screens.get(current);
        if (screen instanceof Refreshable) {
            ((Refreshable) screen).refresh();
        }
    }

    private void startTimers() {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE dd MMM yyyy  HH:mm:ss");
        Timer clockTimer = new Timer(1000, e -> clock.setText(LocalDateTime.now().format(f) + "    "));
        clockTimer.start();
        refreshNotifications();
        new Timer(15000, e -> refreshNotifications()).start();
    }

    public void refreshNotifications() {
        UiKit.async(this, notificationService::unreadCount, n -> {
            notificationsButton.setText("Notifications (" + n + ")");
            notificationsButton.setForeground(n > 0 ? UiKit.RED : UiKit.TEXT);
        });
    }

    private void logout() {
        if (UiKit.confirm(this, "Do you want to log out?")) {
            SecurityContext.setDesktopUser(null);
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
