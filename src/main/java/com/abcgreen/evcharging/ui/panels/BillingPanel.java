package com.abcgreen.evcharging.ui.panels;

import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.service.BillingService;
import com.abcgreen.evcharging.ui.UiKit;
import com.abcgreen.evcharging.ui.components.FormDialog;

/** FR-10 Billing management: invoices, simulated payments and receipts. */
public class BillingPanel extends CrudPanel<Payment> {

    private static final long serialVersionUID = 1L;
    private final transient BillingService billing = new BillingService();
    private final JComboBox<String> status = new JComboBox<>(new String[] { "All statuses", "PENDING", "PAID", "FAILED", "REFUNDED" });

    public BillingPanel() {
        super("Billing & Payments", "Bills are generated automatically when a charging session stops. Payments are simulated.", "Invoice",
                new String[] { "Invoice", "Customer", "Vehicle", "Station", "kWh", "Energy (Rs)", "Fee (Rs)", "GST (Rs)", "Total (Rs)", "Status", "Method", "Paid at" },
                p -> new Object[] { p.getInvoiceNo(), p.getCustomerName(), p.getVehicleRegNo(), p.getStationName(), p.getEnergyKwh(),
                    p.getEnergyCharge(), p.getServiceFee(), p.getTaxAmount(), p.getTotalAmount(), p.getStatus(), p.getMethod(), UiKit.fmt(p.getPaidAt()) });
        status.addActionListener(e -> refresh());
        JButton view = UiKit.primaryButton("View Receipt");
        view.addActionListener(e -> {
            Payment p = selected();
            if (p != null) {
                ReceiptDialog.show(this, p);
            }
        });
        JButton pay = UiKit.button("Record Payment");
        pay.addActionListener(e -> recordPayment());
        toolbar.add(status, 1);
        toolbar.add(view, 3);
        toolbar.add(pay, 4);
    }

    @Override
    protected boolean canModify() {
        return false;
    }

    @Override
    protected List<Payment> load(String keyword) {
        return billing.search(keyword, (String) status.getSelectedItem());
    }

    private void recordPayment() {
        Payment p = selected();
        if (p == null) {
            return;
        }
        if (p.isPaid()) {
            UiKit.info(this, "Invoice " + p.getInvoiceNo() + " is already paid.");
            return;
        }
        JComboBox<String> method = new JComboBox<>(new String[] { "CASH", "UPI", "CARD", "WALLET", "NETBANKING" });
        JTextField detail = new JTextField();
        boolean ok = new FormDialog(this, "Record payment for " + p.getInvoiceNo() + " - " + UiKit.money(p.getTotalAmount()))
                .add("Payment method", method).add("UPI id / card no.", detail)
                .note("Simulated gateway. UPI requires name@bank, CARD requires 16 digits. Test failures: fail@upi or a card ending in 0000.")
                .onSave(() -> {
                    Payment paid = billing.pay(p.getPaymentId(), (String) method.getSelectedItem(), detail.getText());
                    UiKit.info(this, "Payment successful. Transaction ref: " + paid.getTransactionRef());
                    return null;
                }).showDialog();
        if (ok) {
            refresh();
        }
    }

    @Override
    protected boolean edit(Payment item) {
        return false;
    }

    @Override
    protected void delete(Payment item) {
        // invoices are never deleted (audit requirement)
    }
}
