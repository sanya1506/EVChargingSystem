package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Booking;
import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.ChargingSession;
import com.abcgreen.evcharging.model.ChargingStation;
import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.model.Vehicle;
import com.abcgreen.evcharging.service.BookingService;
import com.abcgreen.evcharging.service.ChargeRequestResult;
import com.abcgreen.evcharging.service.ChargingService;
import com.abcgreen.evcharging.service.CustomerService;
import com.abcgreen.evcharging.service.StationService;
import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.BeanTable;
import com.abcgreen.evcharging.ui.components.FormDialog;

/** FR-05 slot booking + FR-07 charging session management (start / stop / history). */
public class ChargingPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private final transient ChargingService charging = new ChargingService();
    private final transient BookingService bookings = new BookingService();
    private final transient CustomerService customers = new CustomerService();
    private final transient StationService stations = new StationService();
    private final JTabbedPane tabs = new JTabbedPane();

    private final BeanTable<ChargingSession> active = new BeanTable<>(
            new String[] { "Session", "Customer", "Vehicle", "Station", "Charger", "Type", "Started", "Start %", "Charged (sim. min)" },
            s -> new Object[] { s.getSessionId(), s.getCustomerName(), s.getVehicleRegNo(), s.getStationName(), s.getChargerCode(),
                s.getChargerType(), UiKit.fmt(s.getStartTime()), s.getStartSoc(), charging.simulatedMinutes(s.getStartTime(), LocalDateTime.now()) });

    private final BeanTable<Booking> bookingTable = new BeanTable<>(
            new String[] { "Booking", "Customer", "Vehicle", "Station", "Charger", "Slot start", "Slot end", "Status" },
            b -> new Object[] { b.getBookingId(), b.getCustomerName(), b.getVehicleRegNo(), b.getStationName(), b.getChargerCode(),
                UiKit.fmt(b.getSlotStart()), UiKit.fmt(b.getSlotEnd()), b.getStatus() });
    private final JComboBox<String> bookingStatus = new JComboBox<>(new String[] { "All statuses", "BOOKED", "IN_PROGRESS", "COMPLETED", "CANCELLED", "NO_SHOW" });

    private final BeanTable<ChargingSession> history = new BeanTable<>(
            new String[] { "Session", "Customer", "Vehicle", "Station", "Charger", "Start", "End", "SOC", "kWh", "Minutes", "Cost (Rs)", "Status", "Payment" },
            s -> new Object[] { s.getSessionId(), s.getCustomerName(), s.getVehicleRegNo(), s.getStationName(), s.getChargerCode(),
                UiKit.fmt(s.getStartTime()), UiKit.fmt(s.getEndTime()), s.getStartSoc() + "% > " + (s.getEndSoc() == null ? "-" : s.getEndSoc() + "%"),
                s.getEnergyKwh(), s.getDurationMinutes(), s.getCost(), s.getStatus(), s.getPaymentStatus() });
    private final JTextField historySearch = new JTextField(20);
    private final JComboBox<String> historyStatus = new JComboBox<>(new String[] { "All statuses", "ACTIVE", "COMPLETED", "ABORTED" });

    public ChargingPanel() {
        setLayout(new BorderLayout());
        tabs.addTab("Active Sessions", activeTab());
        tabs.addTab("Start Charging (walk-in)", startTab());
        tabs.addTab("Bookings", bookingsTab());
        tabs.addTab("Session History", historyTab());
        tabs.addChangeListener(e -> refresh());
        add(UiKit.page("Charging & Bookings", "Start and stop charging sessions, manage slot bookings and view charging history", tabs));
        // live update of the elapsed time column
        new Timer(5000, e -> {
            if (isShowing() && tabs.getSelectedIndex() == 0) {
                active.repaint();
            }
        }).start();
    }

    private JComponent activeTab() {
        JButton stop = UiKit.primaryButton("Stop Charging & Generate Bill");
        stop.addActionListener(e -> stopSelected());
        JButton reload = UiKit.button("Refresh");
        reload.addActionListener(e -> refresh());
        JLabel hint = new JLabel("Simulation: 1 real second = 1 minute of charging (configurable in System settings).");
        hint.setForeground(UiKit.MUTED);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(UiKit.toolbar(stop, reload, hint), BorderLayout.NORTH);
        p.add(UiKit.card(active.scroll()), BorderLayout.CENTER);
        return p;
    }

    private void stopSelected() {
        ChargingSession s = active.getSelectedItem();
        if (s == null) {
            UiKit.info(this, "Select an active session first.");
            return;
        }
        if (!UiKit.confirm(this, "Stop charging " + s.getVehicleRegNo() + " on " + s.getChargerCode() + "?")) {
            return;
        }
        UiKit.async(this, () -> charging.stopSession(s.getSessionId()), (Payment bill) -> {
            refresh();
            ReceiptDialog.show(this, bill);
        });
    }

    private JComponent startTab() {
        JComboBox<Customer> customer = new JComboBox<>();
        JComboBox<Vehicle> vehicle = new JComboBox<>();
        JComboBox<ChargingStation> station = new JComboBox<>();
        JComboBox<ChargerType> type = new JComboBox<>(ChargerType.values());
        JSpinner soc = new JSpinner(new SpinnerNumberModel(25, 0, 99, 1));
        JLabel result = new JLabel(" ");
        result.setFont(UiKit.HEADING);
        JButton go = UiKit.primaryButton("Charge Now");
        JButton loadLists = UiKit.button("Reload lists");

        Runnable load = () -> UiKit.async(this, () -> new Object[] { customers.search(""), stations.findAll() }, r -> {
            customer.removeAllItems();
            @SuppressWarnings("unchecked")
            List<Customer> cs = (List<Customer>) r[0];
            cs.forEach(customer::addItem);
            station.removeAllItems();
            @SuppressWarnings("unchecked")
            List<ChargingStation> ss = (List<ChargingStation>) r[1];
            ss.stream().filter(s -> "ACTIVE".equals(s.getStatus())).forEach(station::addItem);
        });
        loadLists.addActionListener(e -> load.run());
        customer.addActionListener(e -> {
            Customer c = (Customer) customer.getSelectedItem();
            vehicle.removeAllItems();
            if (c != null) {
                UiKit.async(this, () -> customers.vehiclesOf(c.getCustomerId()), vs -> vs.forEach(vehicle::addItem));
            }
        });
        go.addActionListener(e -> {
            Customer c = (Customer) customer.getSelectedItem();
            Vehicle v = (Vehicle) vehicle.getSelectedItem();
            ChargingStation s = (ChargingStation) station.getSelectedItem();
            if (c == null || v == null || s == null) {
                UiKit.info(this, "Please choose customer, vehicle and station.");
                return;
            }
            UiKit.async(this, () -> charging.chargeNow(c.getCustomerId(), v.getVehicleId(), s.getStationId(), (ChargerType) type.getSelectedItem(),
                    (Integer) soc.getValue()), (ChargeRequestResult r) -> {
                        result.setForeground(r.isQueued() ? UiKit.AMBER : UiKit.PRIMARY_DARK);
                        result.setText("<html>" + r.getMessage() + "</html>");
                    });
        });
        load.run();

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(UiKit.CARD);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        Object[][] rows = { { "Customer", customer }, { "Vehicle", vehicle }, { "Station", station }, { "Charger type", type }, { "Battery level now (%)", soc } };
        for (int i = 0; i < rows.length; i++) {
            g.gridy = i;
            g.gridx = 0;
            g.weightx = 0;
            form.add(new JLabel((String) rows[i][0]), g);
            g.gridx = 1;
            g.weightx = 1;
            ((JComponent) rows[i][1]).setPreferredSize(new java.awt.Dimension(380, 32));
            form.add((JComponent) rows[i][1], g);
        }
        g.gridy = rows.length;
        g.gridx = 1;
        form.add(UiKit.toolbar(go, loadLists), g);
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 2;
        form.add(result, g);
        g.gridy++;
        JLabel info = new JLabel("<html><body style='width:520px'>If a charger of the selected type is free, charging starts immediately on the most powerful "
                + "free charger. If all are occupied, the customer is added to the waiting queue and the operator is notified. The queue "
                + "processor thread allocates the next free charger automatically.</body></html>");
        info.setForeground(UiKit.MUTED);
        form.add(info, g);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        wrap.add(UiKit.card(form), BorderLayout.NORTH);
        return wrap;
    }

    private JComponent bookingsTab() {
        JButton newBooking = UiKit.primaryButton("+ New Booking");
        newBooking.addActionListener(e -> newBooking());
        JButton start = UiKit.button("Start Session");
        start.addActionListener(e -> startBooking());
        JButton cancel = UiKit.dangerButton("Cancel Booking");
        cancel.addActionListener(e -> bookingAction(true));
        JButton noShow = UiKit.button("Mark No-show");
        noShow.addActionListener(e -> bookingAction(false));
        bookingStatus.setSelectedItem("BOOKED");
        bookingStatus.addActionListener(e -> refresh());
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(UiKit.toolbar(new JLabel("Show:"), bookingStatus, newBooking, start, cancel, noShow), BorderLayout.NORTH);
        p.add(UiKit.card(bookingTable.scroll()), BorderLayout.CENTER);
        return p;
    }

    private void newBooking() {
        JComboBox<Customer> customer = VehiclesPanel.customerPicker(customers, 0);
        JComboBox<Vehicle> vehicle = new JComboBox<>();
        customer.addActionListener(e -> {
            vehicle.removeAllItems();
            Customer c = (Customer) customer.getSelectedItem();
            if (c != null) {
                customers.vehiclesOf(c.getCustomerId()).forEach(vehicle::addItem);
            }
        });
        JComboBox<ChargingStation> station = new JComboBox<>(stations.findAll().toArray(new ChargingStation[0]));
        JComboBox<ChargerType> type = new JComboBox<>(ChargerType.values());
        JTextField date = new JTextField(LocalDate.now().plusDays(1).toString());
        JTextField time = new JTextField("10:00");
        JSpinner duration = new JSpinner(new SpinnerNumberModel(60, 15, 480, 15));
        boolean saved = new FormDialog(this, "New charging slot booking")
                .add("Customer", customer).add("Vehicle", vehicle).add("Station", station).add("Charger type", type)
                .add("Date (yyyy-mm-dd)", date).add("Start time (HH:mm)", time).add("Duration (minutes)", duration)
                .note("The system picks a free charger automatically and prevents double booking of the same charger.")
                .onSave(() -> {
                    Customer c = (Customer) customer.getSelectedItem();
                    Vehicle v = (Vehicle) vehicle.getSelectedItem();
                    ChargingStation s = (ChargingStation) station.getSelectedItem();
                    if (c == null || v == null || s == null) {
                        throw new ValidationException("Customer, vehicle and station are required.");
                    }
                    LocalDateTime start;
                    try {
                        start = LocalDate.parse(date.getText().trim()).atTime(LocalTime.parse(time.getText().trim()));
                    } catch (DateTimeParseException ex) {
                        throw new ValidationException("Enter the date as yyyy-mm-dd and time as HH:mm.");
                    }
                    Booking b = bookings.book(c.getCustomerId(), v.getVehicleId(), s.getStationId(), (ChargerType) type.getSelectedItem(), start,
                            (Integer) duration.getValue());
                    UiKit.info(this, "Booking #" + b.getBookingId() + " confirmed on charger " + b.getChargerCode() + ".");
                    return null;
                }).showDialog();
        if (saved) {
            refresh();
        }
    }

    private void startBooking() {
        Booking b = bookingTable.getSelectedItem();
        if (b == null) {
            UiKit.info(this, "Select a booking first.");
            return;
        }
        String soc = JOptionPane.showInputDialog(this, "Battery level of " + b.getVehicleRegNo() + " now (%):", "30");
        if (soc == null) {
            return;
        }
        UiKit.async(this, () -> charging.startFromBooking(b.getBookingId(), Integer.parseInt(soc.trim())), s -> {
            UiKit.info(this, "Charging session #" + s.getSessionId() + " started.");
            refresh();
        });
    }

    private void bookingAction(boolean cancel) {
        Booking b = bookingTable.getSelectedItem();
        if (b == null) {
            UiKit.info(this, "Select a booking first.");
            return;
        }
        UiKit.async(this, () -> {
            if (cancel) {
                bookings.cancel(b.getBookingId(), null);
            } else {
                bookings.markNoShow(b.getBookingId());
            }
            return null;
        }, x -> refresh());
    }

    private JComponent historyTab() {
        historySearch.putClientProperty("JTextField.placeholderText", "Customer, vehicle, station...");
        historySearch.addActionListener(e -> refresh());
        historyStatus.addActionListener(e -> refresh());
        JButton find = UiKit.button("Search");
        find.addActionListener(e -> refresh());
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(UiKit.toolbar(historySearch, historyStatus, find, new JLabel("  (latest 1000 sessions)")), BorderLayout.NORTH);
        p.add(UiKit.card(history.scroll()), BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        switch (tabs.getSelectedIndex()) {
            case 0:
                UiKit.async(this, charging::activeSessions, active::setItems);
                break;
            case 2:
                String st = (String) bookingStatus.getSelectedItem();
                UiKit.async(this, () -> bookings.upcoming(st), bookingTable::setItems);
                break;
            case 3:
                String k = historySearch.getText();
                String hs = (String) historyStatus.getSelectedItem();
                UiKit.async(this, () -> charging.search(k, hs), history::setItems);
                break;
            default:
                break;
        }
    }
}
