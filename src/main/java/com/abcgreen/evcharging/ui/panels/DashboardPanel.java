package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.abcgreen.evcharging.model.DashboardStats;
import com.abcgreen.evcharging.model.Notification;
import com.abcgreen.evcharging.service.NotificationService;
import com.abcgreen.evcharging.service.ReportService;
import com.abcgreen.evcharging.ui.MainFrame;
import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.BarChart;
import com.abcgreen.evcharging.ui.components.DonutChart;
import com.abcgreen.evcharging.ui.components.LineChart;
import com.abcgreen.evcharging.ui.components.StatCard;

/** Graphical dashboard with live KPIs, charts and the operator notification feed. Auto-refreshes every 20 s. */
public class DashboardPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private final ReportService reportService = new ReportService();
    private final NotificationService notificationService = new NotificationService();

    private final StatCard stations = new StatCard("Stations", UiKit.PRIMARY);
    private final StatCard chargers = new StatCard("Available chargers", UiKit.PRIMARY);
    private final StatCard active = new StatCard("Charging now", UiKit.BLUE);
    private final StatCard queue = new StatCard("Waiting in queue", UiKit.AMBER);
    private final StatCard today = new StatCard("Sessions today", UiKit.BLUE);
    private final StatCard revenue = new StatCard("Revenue today", UiKit.PRIMARY_DARK);
    private final StatCard month = new StatCard("Revenue this month", UiKit.PURPLE);
    private final StatCard customers = new StatCard("Customers", UiKit.MUTED);

    private final BarChart hourly = new BarChart("Peak hour demand (sessions per hour, last 30 days)");
    private final LineChart daily = new LineChart("Energy delivered per day (kWh)", UiKit.BLUE);
    private final DonutChart status = new DonutChart("Charger status").colors(UiKit.PRIMARY, UiKit.BLUE, UiKit.RED, UiKit.AMBER);
    private final DefaultListModel<String> notifModel = new DefaultListModel<>();
    private final JLabel notifTitle = new JLabel("Notifications");

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(UiKit.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        add(UiKit.header("Dashboard", "Live overview of the charging network"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        JPanel kpis = new JPanel(new GridLayout(2, 4, 12, 12));
        kpis.setOpaque(false);
        for (StatCard c : new StatCard[] { stations, chargers, active, queue, today, revenue, month, customers }) {
            kpis.add(c);
        }
        body.add(kpis, BorderLayout.NORTH);

        JPanel charts = new JPanel(new GridLayout(2, 2, 12, 12));
        charts.setOpaque(false);
        charts.add(hourly);
        charts.add(daily);
        charts.add(status);
        charts.add(notificationsCard());
        body.add(charts, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        new Timer(20000, e -> {
            if (isShowing()) {
                refresh();
            }
        }).start();
    }

    private JPanel notificationsCard() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setBackground(UiKit.CARD);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiKit.BORDER), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        notifTitle.setFont(UiKit.HEADING);
        JButton markRead = UiKit.button("Mark all read");
        markRead.addActionListener(e -> UiKit.async(this, () -> {
            notificationService.markAllRead();
            return null;
        }, x -> {
            refresh();
            MainFrame f = (MainFrame) SwingUtilities.getWindowAncestor(this);
            f.refreshNotifications();
        }));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(notifTitle, BorderLayout.WEST);
        head.add(markRead, BorderLayout.EAST);
        p.add(head, BorderLayout.NORTH);
        JList<String> list = new JList<>(notifModel);
        list.setFont(UiKit.SMALL);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        UiKit.async(this, () -> new Object[] { reportService.dashboard(), notificationService.recent(40) }, result -> {
            DashboardStats d = (DashboardStats) result[0];
            @SuppressWarnings("unchecked")
            List<Notification> notes = (List<Notification>) result[1];
            show(d, notes);
        });
    }

    private void show(DashboardStats d, List<Notification> notes) {
        stations.set(String.valueOf(d.getTotalStations()), d.getTotalChargers() + " chargers installed");
        chargers.set(d.getAvailableChargers() + " / " + d.getTotalChargers(), d.getFaultyChargers() + " faulty, " + d.getMaintenanceChargers() + " in maintenance");
        active.set(String.valueOf(d.getActiveSessions()), d.getOccupiedChargers() + " chargers occupied");
        queue.set(String.valueOf(d.getQueueLength()), d.getQueueLength() > 0 ? "customers waiting for a charger" : "no one is waiting");
        today.set(String.valueOf(d.getTodaySessions()), String.format("%.1f kWh delivered", d.getTodayEnergyKwh()));
        revenue.set(UiKit.money(d.getTodayRevenue()), d.getPendingPayments() + " bills pending payment");
        month.set(UiKit.money(d.getMonthRevenue()), "paid invoices");
        customers.set(String.valueOf(d.getTotalCustomers()), d.getTotalVehicles() + " vehicles registered");

        String[] hours = new String[24];
        double[] hv = new double[24];
        for (int h = 0; h < 24; h++) {
            hours[h] = String.valueOf(h);
            hv[h] = d.getHourlyDemand()[h];
        }
        hourly.setData(hours, hv);
        daily.setData(d.getDailyEnergy().keySet().toArray(new String[0]), d.getDailyEnergy().values().stream().mapToDouble(Double::doubleValue).toArray());
        status.setData(new String[] { "Available", "Occupied", "Faulty", "Maintenance" },
                new double[] { d.getAvailableChargers(), d.getOccupiedChargers(), d.getFaultyChargers(), d.getMaintenanceChargers() });

        notifModel.clear();
        for (Notification n : notes) {
            notifModel.addElement((n.isRead() ? "   " : "* ") + UiKit.fmt(n.getCreatedAt()) + "  [" + n.getType() + "]  " + n.getMessage());
        }
        notifTitle.setText("Notifications (" + d.getUnreadNotifications() + " unread)");
    }
}
