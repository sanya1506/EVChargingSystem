package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.abcgreen.evcharging.model.Payment;

public class PaymentDao extends BaseDao {

    private static final String SELECT = "SELECT p.*, c.full_name AS customer_name, c.email AS customer_email, v.registration_no, "
            + "s.name AS station_name, ch.charger_code, ch.price_per_kwh, cs.energy_kwh, cs.duration_minutes, cs.start_time, cs.end_time, cs.customer_id "
            + "FROM payments p JOIN charging_sessions cs ON cs.session_id = p.session_id JOIN customers c ON c.customer_id = cs.customer_id "
            + "JOIN vehicles v ON v.vehicle_id = cs.vehicle_id JOIN chargers ch ON ch.charger_id = cs.charger_id "
            + "JOIN stations s ON s.station_id = ch.station_id ";

    private static Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setSessionId(rs.getInt("session_id"));
        p.setInvoiceNo(rs.getString("invoice_no"));
        p.setEnergyCharge(rs.getDouble("energy_charge"));
        p.setServiceFee(rs.getDouble("service_fee"));
        p.setTaxAmount(rs.getDouble("tax_amount"));
        p.setTotalAmount(rs.getDouble("total_amount"));
        p.setMethod(rs.getString("method"));
        p.setStatus(rs.getString("status"));
        p.setTransactionRef(rs.getString("transaction_ref"));
        p.setCreatedAt(dateTime(rs, "created_at"));
        p.setPaidAt(dateTime(rs, "paid_at"));
        p.setCustomerName(rs.getString("customer_name"));
        p.setCustomerEmail(rs.getString("customer_email"));
        p.setVehicleRegNo(rs.getString("registration_no"));
        p.setStationName(rs.getString("station_name"));
        p.setChargerCode(rs.getString("charger_code"));
        p.setPricePerKwh(rs.getDouble("price_per_kwh"));
        p.setEnergyKwh(rs.getDouble("energy_kwh"));
        p.setDurationMinutes(rs.getInt("duration_minutes"));
        p.setSessionStart(dateTime(rs, "start_time"));
        p.setSessionEnd(dateTime(rs, "end_time"));
        return p;
    }

    public int insert(Payment p) {
        int id = insert("INSERT INTO payments (session_id, invoice_no, energy_charge, service_fee, tax_amount, total_amount, status) VALUES (?,?,?,?,?,?,'PENDING')",
                p.getSessionId(), p.getInvoiceNo(), p.getEnergyCharge(), p.getServiceFee(), p.getTaxAmount(), p.getTotalAmount());
        p.setPaymentId(id);
        return id;
    }

    public Payment findById(int id) {
        return queryOne(SELECT + "WHERE p.payment_id = ?", PaymentDao::map, id);
    }

    public Payment findBySession(int sessionId) {
        return queryOne(SELECT + "WHERE p.session_id = ?", PaymentDao::map, sessionId);
    }

    public List<Payment> findByCustomer(int customerId) {
        return query(SELECT + "WHERE cs.customer_id = ? ORDER BY p.created_at DESC LIMIT 200", PaymentDao::map, customerId);
    }

    public List<Payment> search(String keyword, String status, int limit) {
        String k = like(keyword);
        String st = status == null || status.startsWith("All") ? "%" : status;
        return query(SELECT + "WHERE (p.invoice_no LIKE ? OR c.full_name LIKE ? OR v.registration_no LIKE ?) AND p.status LIKE ? "
                + "ORDER BY p.created_at DESC LIMIT ?", PaymentDao::map, k, k, k, st, limit);
    }

    public boolean markPaid(int paymentId, String method, String transactionRef) {
        return update("UPDATE payments SET status='PAID', method=?, transaction_ref=?, paid_at=? WHERE payment_id=? AND status IN ('PENDING','FAILED')",
                method, transactionRef, LocalDateTime.now(), paymentId) == 1;
    }

    public void markFailed(int paymentId, String method) {
        update("UPDATE payments SET status='FAILED', method=? WHERE payment_id=? AND status='PENDING'", method, paymentId);
    }
}
