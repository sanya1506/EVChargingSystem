package com.abcgreen.evcharging.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.abcgreen.evcharging.dao.PaymentDao;
import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.PaymentException;
import com.abcgreen.evcharging.exception.RecordNotFoundException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Billable;
import com.abcgreen.evcharging.model.Payment;

/**
 * FR-10 Billing management: bill calculation, invoice numbers and a SIMULATED payment gateway.
 * No real money is moved - payments succeed unless a documented test value is used.
 */
public class BillingService {

    private final PaymentDao paymentDao = new PaymentDao();
    private final ConfigService config = new ConfigService();
    private final AuditService audit = new AuditService();
    private final NotificationService notifications = new NotificationService();

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Calculates a bill for energy delivered on a charger (polymorphic pricing through Billable). */
    public Payment calculateBill(Billable charger, double energyKwh) {
        double gstPercent = config.getDouble(ConfigService.GST_RATE, Billable.GST_RATE * 100);
        return calculateBill(charger, energyKwh, gstPercent);
    }

    public static Payment calculateBill(Billable charger, double energyKwh, double gstPercent) {
        Payment p = new Payment();
        double energyCharge = round2(charger.calculateEnergyCharge(energyKwh));
        double serviceFee = energyKwh > 0 ? charger.getServiceFee() : 0;
        double tax = round2((energyCharge + serviceFee) * gstPercent / 100.0);
        p.setEnergyCharge(energyCharge);
        p.setServiceFee(serviceFee);
        p.setTaxAmount(tax);
        p.setTotalAmount(round2(energyCharge + serviceFee + tax));
        return p;
    }

    /** Creates the PENDING bill for a completed session (called inside the stop-session transaction). */
    public Payment createBill(int sessionId, Billable charger, double energyKwh) {
        Payment p = calculateBill(charger, energyKwh);
        p.setSessionId(sessionId);
        p.setInvoiceNo(invoiceNo(sessionId));
        paymentDao.insert(p);
        return p;
    }

    /** Invoice number = date + session id (one bill per session, so it is always unique). */
    static String invoiceNo(int sessionId) {
        return "INV-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + String.format("%06d", sessionId);
    }

    public Payment findById(int paymentId) throws RecordNotFoundException {
        Payment p = paymentDao.findById(paymentId);
        if (p == null) {
            throw new RecordNotFoundException("Invoice not found.");
        }
        return p;
    }

    public Payment findBySession(int sessionId) {
        return paymentDao.findBySession(sessionId);
    }

    public List<Payment> search(String keyword, String status) {
        return paymentDao.search(keyword, status, 1000);
    }

    public List<Payment> paymentsOf(int customerId) {
        return paymentDao.findByCustomer(customerId);
    }

    /**
     * Simulated payment gateway.
     * Test values that FAIL: UPI id "fail@upi", card numbers ending in 0000.
     *
     * @param detail UPI id, card number, bank name... depending on the method
     */
    public Payment pay(int paymentId, String method, String detail) throws EVChargingException {
        Payment p = findById(paymentId);
        if (p.isPaid()) {
            throw new PaymentException("Invoice " + p.getInvoiceNo() + " is already paid.");
        }
        if (method == null || method.isBlank()) {
            throw new ValidationException("Please choose a payment method.");
        }
        String d = detail == null ? "" : detail.trim().replace(" ", "");
        if ("UPI".equals(method) && !d.matches("^[\\w.-]+@[\\w]+$")) {
            throw new ValidationException("Enter a valid UPI id, e.g. name@okaxis");
        }
        if ("CARD".equals(method) && !d.matches("^\\d{16}$")) {
            throw new ValidationException("Card number must have 16 digits.");
        }
        boolean declined = d.equalsIgnoreCase("fail@upi") || ("CARD".equals(method) && d.endsWith("0000"));
        if (declined) {
            paymentDao.markFailed(paymentId, method);
            audit.log("PAYMENT_FAILED", "PAYMENT", paymentId, p.getInvoiceNo() + " via " + method);
            throw new PaymentException("Payment was declined by the (simulated) gateway. Please try another method.");
        }
        String txn = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        if (!paymentDao.markPaid(paymentId, method, txn)) {
            throw new PaymentException("Payment could not be recorded. Please refresh and try again.");
        }
        audit.log("PAYMENT", "PAYMENT", paymentId, p.getInvoiceNo() + " Rs " + p.getTotalAmount() + " via " + method + " " + txn);
        notifications.notifyOperators("PAYMENT", "Payment of Rs " + p.getTotalAmount() + " received for " + p.getInvoiceNo() + " (" + method + ")");
        return findById(paymentId);
    }
}
