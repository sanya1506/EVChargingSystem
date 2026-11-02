package com.abcgreen.evcharging.ui.panels;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.Vehicle;
import com.abcgreen.evcharging.service.CustomerService;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.FormDialog;

/** FR-03 Electric vehicle registration screen. */
public class VehiclesPanel extends CrudPanel<Vehicle> {

    private static final long serialVersionUID = 1L;
    private final transient CustomerService service = new CustomerService();

    public VehiclesPanel() {
        super("Vehicles", "Electric vehicles registered to customers. Registration numbers are validated and must be unique.", "Vehicle",
                new String[] { "ID", "Registration", "Make", "Model", "Type", "Battery (kWh)", "Connector", "Owner" },
                v -> new Object[] { v.getVehicleId(), v.getRegistrationNo(), v.getMake(), v.getModel(), v.getVehicleType(),
                    v.getBatteryCapacityKwh(), v.getConnectorType(), v.getCustomerName() });
    }

    @Override
    protected List<Vehicle> load(String keyword) {
        return service.searchVehicles(keyword);
    }

    @Override
    protected boolean edit(Vehicle existing) {
        Vehicle v = existing == null ? new Vehicle() : existing;
        JComboBox<Customer> owner = customerPicker(service, v.getCustomerId());
        JTextField reg = new JTextField(v.getRegistrationNo());
        JTextField make = new JTextField(v.getMake());
        JTextField model = new JTextField(v.getModel());
        JComboBox<String> type = new JComboBox<>(Vehicle.TYPES);
        if (v.getVehicleType() != null) {
            type.setSelectedItem(v.getVehicleType());
        }
        JSpinner battery = new JSpinner(new SpinnerNumberModel(v.getBatteryCapacityKwh() > 0 ? v.getBatteryCapacityKwh() : 30.0, 1.0, 500.0, 0.1));
        JComboBox<String> connector = new JComboBox<>(Vehicle.CONNECTORS);
        if (v.getConnectorType() != null) {
            connector.setSelectedItem(v.getConnectorType());
        }
        return new FormDialog(this, existing == null ? "Register vehicle" : "Edit vehicle " + v.getRegistrationNo())
                .add("Owner *", owner).add("Registration no. *", reg).add("Make *", make).add("Model *", model)
                .add("Vehicle type", type).add("Battery capacity (kWh)", battery).add("Connector", connector)
                .note("Registration format: KA01AB1234 (spaces and hyphens are removed automatically).")
                .onSave(() -> {
                    Customer c = (Customer) owner.getSelectedItem();
                    v.setCustomerId(c == null ? 0 : c.getCustomerId());
                    v.setRegistrationNo(reg.getText());
                    v.setMake(make.getText());
                    v.setModel(model.getText());
                    v.setVehicleType((String) type.getSelectedItem());
                    v.setBatteryCapacityKwh(((Number) battery.getValue()).doubleValue());
                    v.setConnectorType((String) connector.getSelectedItem());
                    service.saveVehicle(v);
                    return null;
                }).showDialog();
    }

    /** Combo box of all customers (type to search), pre-selecting the given customer id. */
    static JComboBox<Customer> customerPicker(CustomerService service, int selectedId) {
        List<Customer> all = service.search("");
        JComboBox<Customer> box = new JComboBox<>(all.toArray(new Customer[0]));
        box.setMaximumRowCount(15);
        for (Customer c : all) {
            if (c.getCustomerId() == selectedId) {
                box.setSelectedItem(c);
            }
        }
        if (selectedId == 0) {
            box.setSelectedIndex(-1);
        }
        return box;
    }

    @Override
    protected void delete(Vehicle v) {
        service.deleteVehicle(v.getVehicleId());
    }
}
