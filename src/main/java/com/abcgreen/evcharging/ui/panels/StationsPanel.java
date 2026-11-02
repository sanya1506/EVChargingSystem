package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.ChargerStatus;
import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.ChargingStation;
import com.abcgreen.evcharging.model.Vehicle;
import com.abcgreen.evcharging.service.StationService;
import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.BeanTable;
import com.abcgreen.evcharging.ui.components.FormDialog;

/**
 * FR-04 Charging station management + charger configuration + FR-09 status monitoring.
 * Charger status changes are pushed on a Stack so the operator can undo them (Module IV - Stack).
 */
public class StationsPanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private final transient StationService service = new StationService();
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> city = new JComboBox<>();
    private final JLabel chargerTitle = new JLabel("Select a station to see its chargers");
    private final JButton undo = UiKit.button("Undo status change");

    /** Undo history: each entry = {chargerId, previous status}. */
    private final Stack<Object[]> statusHistory = new Stack<>();

    private final BeanTable<ChargingStation> stations = new BeanTable<>(
            new String[] { "Code", "Name", "City", "Status", "Fast", "Slow", "Available", "Occupied", "Faulty", "Maint.", "kW", "Util %" },
            s -> new Object[] { s.getStationCode(), s.getName(), s.getCity(), s.getStatus(), s.getFastChargers(), s.getSlowChargers(),
                s.getAvailableChargers(), s.getOccupiedChargers(), s.getFaultyChargers(), s.getMaintenanceChargers(), s.getCapacityKw(),
                Math.round(s.getUtilisationPercent()) });

    private final BeanTable<Charger> chargers = new BeanTable<>(
            new String[] { "Code", "Type", "Connector", "Power (kW)", "Rs / kWh", "Status", "Installed", "Service fee", "Efficiency" },
            c -> new Object[] { c.getChargerCode(), c.getType(), c.getConnectorType(), c.getPowerKw(), c.getPricePerKwh(), c.getStatus(),
                UiKit.fmt(c.getInstalledOn()), c.getServiceFee(), c.getEfficiency() });

    public StationsPanel() {
        setLayout(new BorderLayout());
        city.addItem("All cities");
        for (String c : CustomersPanel.CITIES) {
            city.addItem(c);
        }
        search.putClientProperty("JTextField.placeholderText", "Search station...");
        search.addActionListener(e -> refresh());
        city.addActionListener(e -> refresh());

        JButton addStation = UiKit.primaryButton("+ Add Station");
        addStation.addActionListener(e -> editStation(null));
        JButton editStation = UiKit.button("Edit");
        editStation.addActionListener(e -> {
            ChargingStation s = stations.getSelectedItem();
            if (s != null) {
                editStation(s);
            }
        });
        JButton deleteStation = UiKit.dangerButton("Delete");
        deleteStation.addActionListener(e -> deleteStation());

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(UiKit.toolbar(search, city, addStation, editStation, deleteStation), BorderLayout.NORTH);
        top.add(UiKit.card(stations.scroll()), BorderLayout.CENTER);

        JButton addCharger = UiKit.primaryButton("+ Add Charger");
        addCharger.addActionListener(e -> editCharger(null));
        JButton editCharger = UiKit.button("Edit");
        editCharger.addActionListener(e -> {
            Charger c = chargers.getSelectedItem();
            if (c != null) {
                editCharger(c);
            }
        });
        JButton deleteCharger = UiKit.dangerButton("Delete");
        deleteCharger.addActionListener(e -> deleteCharger());
        JButton available = UiKit.button("Mark Available");
        available.addActionListener(e -> setStatus(ChargerStatus.AVAILABLE));
        JButton faulty = UiKit.button("Report Fault");
        faulty.addActionListener(e -> setStatus(ChargerStatus.FAULTY));
        JButton maintenance = UiKit.button("Under Maintenance");
        maintenance.addActionListener(e -> setStatus(ChargerStatus.MAINTENANCE));
        undo.setEnabled(false);
        undo.addActionListener(e -> undoStatus());
        chargerTitle.setFont(UiKit.HEADING);

        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);
        JPanel bottomHead = new JPanel(new BorderLayout());
        bottomHead.setOpaque(false);
        bottomHead.add(chargerTitle, BorderLayout.NORTH);
        bottomHead.add(UiKit.toolbar(addCharger, editCharger, deleteCharger, new JLabel("   Status:"), available, faulty, maintenance, undo), BorderLayout.SOUTH);
        bottom.add(bottomHead, BorderLayout.NORTH);
        bottom.add(UiKit.card(chargers.scroll()), BorderLayout.CENTER);

        stations.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadChargers();
            }
        });

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setOpaque(false);
        add(UiKit.page("Stations & Chargers", "Manage charging stations, fast (DC) and slow (AC) chargers and their live status", split));
    }

    @Override
    public void refresh() {
        String k = search.getText();
        String c = (String) city.getSelectedItem();
        ChargingStation selected = stations.getSelectedItem();
        int selectedId = selected == null ? -1 : selected.getStationId();
        UiKit.async(this, () -> service.search(k, c), list -> {
            stations.setItems(list);
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getStationId() == selectedId) {
                    int view = stations.convertRowIndexToView(i);
                    stations.setRowSelectionInterval(view, view);
                }
            }
        });
    }

    private void loadChargers() {
        ChargingStation s = stations.getSelectedItem();
        if (s == null) {
            chargers.setItems(new ArrayList<>());
            return;
        }
        chargerTitle.setText("Chargers at " + s.getName() + "  (" + s.getAddress() + ", " + s.getOperatingHours() + ")");
        UiKit.async(this, () -> service.chargersOf(s.getStationId()), chargers::setItems);
    }

    private void editStation(ChargingStation existing) {
        ChargingStation s = existing == null ? new ChargingStation() : existing;
        JTextField code = new JTextField(s.getStationCode());
        JTextField name = new JTextField(s.getName());
        JComboBox<String> cityBox = new JComboBox<>(CustomersPanel.CITIES);
        cityBox.setEditable(true);
        if (s.getCity() != null) {
            cityBox.setSelectedItem(s.getCity());
        }
        JTextField address = new JTextField(s.getAddress());
        JTextField lat = new JTextField(String.valueOf(s.getLatitude()));
        JTextField lon = new JTextField(String.valueOf(s.getLongitude()));
        JTextField hours = new JTextField(s.getOperatingHours());
        JTextField phone = new JTextField(s.getContactPhone());
        JComboBox<String> status = new JComboBox<>(ChargingStation.STATUSES);
        status.setSelectedItem(s.getStatus());
        boolean saved = new FormDialog(this, existing == null ? "Add charging station" : "Edit station " + s.getStationCode())
                .add("Station code *", code).add("Name *", name).add("City *", cityBox).add("Address *", address)
                .add("Latitude", lat).add("Longitude", lon).add("Operating hours", hours).add("Contact phone", phone).add("Status", status)
                .note("Latitude / longitude are stored for the future GIS and route-optimisation modules.")
                .onSave(() -> {
                    s.setStationCode(code.getText());
                    s.setName(name.getText());
                    s.setCity((String) cityBox.getSelectedItem());
                    s.setAddress(address.getText());
                    try {
                        s.setLatitude(Double.parseDouble(lat.getText().trim()));
                        s.setLongitude(Double.parseDouble(lon.getText().trim()));
                    } catch (NumberFormatException ex) {
                        throw new com.abcgreen.evcharging.exception.ValidationException("Latitude and longitude must be numbers.");
                    }
                    s.setOperatingHours(hours.getText().isBlank() ? "00:00-23:59" : hours.getText().trim());
                    s.setContactPhone(phone.getText());
                    s.setStatus((String) status.getSelectedItem());
                    service.save(s);
                    return null;
                }).showDialog();
        if (saved) {
            refresh();
        }
    }

    private void deleteStation() {
        ChargingStation s = stations.getSelectedItem();
        if (s != null && UiKit.confirm(this, "Delete station " + s.getName() + " with all its chargers, sessions and bookings?")) {
            UiKit.async(this, () -> {
                service.delete(s.getStationId());
                return null;
            }, x -> refresh());
        }
    }

    private void editCharger(Charger existing) {
        ChargingStation station = stations.getSelectedItem();
        if (station == null && existing == null) {
            UiKit.info(this, "Select the station first.");
            return;
        }
        JComboBox<ChargerType> type = new JComboBox<>(ChargerType.values());
        JTextField code = new JTextField(existing == null ? station.getStationCode() + "-" : existing.getChargerCode());
        JComboBox<String> connector = new JComboBox<>(Vehicle.CONNECTORS);
        JSpinner power = new JSpinner(new SpinnerNumberModel(existing == null ? 60.0 : existing.getPowerKw(), 1.0, 400.0, 0.1));
        JSpinner price = new JSpinner(new SpinnerNumberModel(existing == null ? 20.0 : existing.getPricePerKwh(), 1.0, 100.0, 0.5));
        if (existing != null) {
            type.setSelectedItem(existing.getType());
            connector.setSelectedItem(existing.getConnectorType());
        }
        boolean saved = new FormDialog(this, existing == null ? "Add charger to " + station.getName() : "Edit charger " + existing.getChargerCode())
                .add("Charger type", type).add("Charger code *", code).add("Connector", connector)
                .add("Power (kW)", power).add("Price per kWh (Rs)", price)
                .note("FAST = DC charger (30-150 kW, Rs 30 service fee).  SLOW = AC charger (3.3-22 kW, Rs 10 service fee).")
                .onSave(() -> {
                    Charger c = Charger.create((ChargerType) type.getSelectedItem());
                    if (existing != null) {
                        c.setChargerId(existing.getChargerId());
                        c.setStationId(existing.getStationId());
                        c.setStatus(existing.getStatus());
                        c.setInstalledOn(existing.getInstalledOn());
                    } else {
                        c.setStationId(station.getStationId());
                    }
                    c.setChargerCode(code.getText());
                    c.setConnectorType((String) connector.getSelectedItem());
                    c.setPowerKw(((Number) power.getValue()).doubleValue());
                    c.setPricePerKwh(((Number) price.getValue()).doubleValue());
                    service.saveCharger(c);
                    return null;
                }).showDialog();
        if (saved) {
            loadChargers();
            refresh();
        }
    }

    private void deleteCharger() {
        Charger c = chargers.getSelectedItem();
        if (c != null && UiKit.confirm(this, "Delete charger " + c.getChargerCode() + "?")) {
            UiKit.async(this, () -> {
                service.deleteCharger(c.getChargerId());
                return null;
            }, x -> {
                loadChargers();
                refresh();
            });
        }
    }

    private void setStatus(ChargerStatus status) {
        Charger c = chargers.getSelectedItem();
        if (c == null) {
            UiKit.info(this, "Select a charger first.");
            return;
        }
        ChargerStatus previous = c.getStatus();
        UiKit.async(this, () -> {
            service.changeChargerStatus(c.getChargerId(), status);
            return null;
        }, x -> {
            statusHistory.push(new Object[] { c.getChargerId(), previous, c.getChargerCode() });
            undo.setEnabled(true);
            undo.setText("Undo (" + statusHistory.size() + ")");
            loadChargers();
            refresh();
        });
    }

    private void undoStatus() {
        if (statusHistory.isEmpty()) {
            return;
        }
        Object[] last = statusHistory.pop();
        UiKit.async(this, () -> {
            service.changeChargerStatus((Integer) last[0], (ChargerStatus) last[1]);
            return null;
        }, x -> {
            UiKit.info(this, "Charger " + last[2] + " set back to " + last[1] + ".");
            undo.setEnabled(!statusHistory.isEmpty());
            undo.setText(statusHistory.isEmpty() ? "Undo status change" : "Undo (" + statusHistory.size() + ")");
            loadChargers();
            refresh();
        });
    }

    List<ChargingStation> currentStations() {
        return stations.getItems();
    }
}
