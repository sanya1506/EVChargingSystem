package com.abcgreen.evcharging.ui.panels;

import java.awt.BorderLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.MaintenanceRecord;
import com.abcgreen.evcharging.service.StationService;
import com.abcgreen.evcharging.ui.Refreshable;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.BeanTable;
import com.abcgreen.evcharging.ui.components.FormDialog;

/** Infrastructure management: maintenance schedule of chargers. */
public class MaintenancePanel extends JPanel implements Refreshable {

    private static final long serialVersionUID = 1L;
    private final transient StationService service = new StationService();
    private final JComboBox<String> status = new JComboBox<>(new String[] { "All statuses", "SCHEDULED", "IN_PROGRESS", "COMPLETED" });
    private final BeanTable<MaintenanceRecord> table = new BeanTable<>(
            new String[] { "ID", "Charger", "Station", "Scheduled", "Type", "Description", "Technician", "Status", "Completed" },
            m -> new Object[] { m.getMaintenanceId(), m.getChargerCode(), m.getStationName(), UiKit.fmt(m.getScheduledDate()), m.getMaintenanceType(),
                m.getDescription(), m.getTechnician(), m.getStatus(), UiKit.fmt(m.getCompletedOn()) });

    public MaintenancePanel() {
        setLayout(new BorderLayout());
        status.addActionListener(e -> refresh());
        JButton schedule = UiKit.primaryButton("+ Schedule Maintenance");
        schedule.addActionListener(e -> schedule());
        JButton start = UiKit.button("Start (take charger offline)");
        start.addActionListener(e -> update("IN_PROGRESS"));
        JButton complete = UiKit.button("Complete (charger available)");
        complete.addActionListener(e -> update("COMPLETED"));
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);
        body.add(UiKit.toolbar(status, schedule, start, complete), BorderLayout.NORTH);
        body.add(UiKit.card(table.scroll()), BorderLayout.CENTER);
        add(UiKit.page("Maintenance Schedule", "Plan preventive and corrective maintenance. Chargers go offline while maintenance is in progress.", body));
    }

    @Override
    public void refresh() {
        String s = (String) status.getSelectedItem();
        UiKit.async(this, () -> service.maintenance(s), table::setItems);
    }

    private void schedule() {
        JComboBox<Charger> charger = new JComboBox<>(service.allChargers().toArray(new Charger[0]));
        JTextField date = new JTextField(LocalDate.now().plusDays(1).toString());
        JComboBox<String> type = new JComboBox<>(MaintenanceRecord.TYPES);
        JTextField desc = new JTextField();
        JTextField tech = new JTextField();
        boolean ok = new FormDialog(this, "Schedule maintenance")
                .add("Charger", charger).add("Date (yyyy-mm-dd)", date).add("Type", type).add("Description", desc).add("Technician *", tech)
                .onSave(() -> {
                    MaintenanceRecord m = new MaintenanceRecord();
                    Charger c = (Charger) charger.getSelectedItem();
                    m.setChargerId(c == null ? 0 : c.getChargerId());
                    try {
                        m.setScheduledDate(LocalDate.parse(date.getText().trim()));
                    } catch (DateTimeParseException ex) {
                        throw new ValidationException("Enter the date as yyyy-mm-dd.");
                    }
                    m.setMaintenanceType((String) type.getSelectedItem());
                    m.setDescription(desc.getText());
                    m.setTechnician(tech.getText());
                    service.scheduleMaintenance(m);
                    return null;
                }).showDialog();
        if (ok) {
            refresh();
        }
    }

    private void update(String newStatus) {
        MaintenanceRecord m = table.getSelectedItem();
        if (m == null) {
            UiKit.info(this, "Select a maintenance record first.");
            return;
        }
        UiKit.async(this, () -> {
            service.updateMaintenanceStatus(m.getMaintenanceId(), newStatus);
            return null;
        }, x -> refresh());
    }
}
