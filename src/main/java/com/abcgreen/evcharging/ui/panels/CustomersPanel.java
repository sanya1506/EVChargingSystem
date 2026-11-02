package com.abcgreen.evcharging.ui.panels;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.service.CustomerService;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.FormDialog;

/** FR-02 Customer management screen. */
public class CustomersPanel extends CrudPanel<Customer> {

    private static final long serialVersionUID = 1L;
    static final String[] CITIES = { "Bengaluru", "Mumbai", "Delhi", "Hyderabad", "Chennai", "Pune", "Kolkata", "Ahmedabad" };
    private final transient CustomerService service = new CustomerService();

    public CustomersPanel() {
        super("Customers", "Register and manage EV owners. Duplicate emails and phone numbers are rejected.", "Customer",
                new String[] { "ID", "Name", "Email", "Phone", "City", "Vehicles", "Status", "Registered" },
                c -> new Object[] { c.getCustomerId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getCity(), c.getVehicleCount(),
                    c.getStatus(), UiKit.fmt(c.getRegisteredOn()) });
    }

    @Override
    protected List<Customer> load(String keyword) {
        return service.search(keyword);
    }

    @Override
    protected boolean edit(Customer existing) {
        Customer c = existing == null ? new Customer() : existing;
        JTextField name = new JTextField(c.getFullName());
        JTextField email = new JTextField(c.getEmail());
        JTextField phone = new JTextField(c.getPhone());
        JComboBox<String> city = new JComboBox<>(CITIES);
        city.setEditable(true);
        if (c.getCity() != null) {
            city.setSelectedItem(c.getCity());
        }
        JTextField address = new JTextField(c.getAddress());
        JComboBox<String> status = new JComboBox<>(new String[] { "ACTIVE", "BLOCKED" });
        status.setSelectedItem(c.getStatus());
        return new FormDialog(this, existing == null ? "Register customer" : "Edit customer #" + c.getCustomerId())
                .add("Full name *", name).add("Email *", email).add("Mobile (10 digits) *", phone)
                .add("City *", city).add("Address", address).add("Status", status)
                .note("Customers created here can be given a web login by registering on the portal with the same email.")
                .onSave(() -> {
                    c.setFullName(name.getText());
                    c.setEmail(email.getText());
                    c.setPhone(phone.getText());
                    c.setCity((String) city.getSelectedItem());
                    c.setAddress(address.getText());
                    c.setStatus((String) status.getSelectedItem());
                    service.save(c);
                    return null;
                }).showDialog();
    }

    @Override
    protected void delete(Customer c) {
        service.delete(c.getCustomerId());
    }
}
