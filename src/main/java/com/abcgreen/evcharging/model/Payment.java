package com.abcgreen.evcharging.model;

import java.time.LocalDateTime;

/** Bill / invoice for a charging session and its (simulated) payment. */
public class Payment {

    public static final String[] METHODS = { "UPI", "CARD", "NETBANKING", "WALLET", "CASH" };

    private int paymentId;
    private int sessionId;
    private String invoiceNo;
    private double energyCharge;
    private double serviceFee;
    private double taxAmount;
    private double totalAmount;
    private String method;
    private String status = "PENDING";
    private String transactionRef;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    // display fields
    private String customerName;
    private String customerEmail;
    private String vehicleRegNo;
    private String stationName;
    private String chargerCode;
    private double energyKwh;
    private int durationMinutes;
    private double pricePerKwh;
    private LocalDateTime sessionStart;
    private LocalDateTime sessionEnd;

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public int getPaymentId() { return paymentId; }
    public void setPaymentId(int paymentId) { this.paymentId = paymentId; }
    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }
    public double getEnergyCharge() { return energyCharge; }
    public void setEnergyCharge(double energyCharge) { this.energyCharge = energyCharge; }
    public double getServiceFee() { return serviceFee; }
    public void setServiceFee(double serviceFee) { this.serviceFee = serviceFee; }
    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }
    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getVehicleRegNo() { return vehicleRegNo; }
    public void setVehicleRegNo(String vehicleRegNo) { this.vehicleRegNo = vehicleRegNo; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getChargerCode() { return chargerCode; }
    public void setChargerCode(String chargerCode) { this.chargerCode = chargerCode; }
    public double getEnergyKwh() { return energyKwh; }
    public void setEnergyKwh(double energyKwh) { this.energyKwh = energyKwh; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public double getPricePerKwh() { return pricePerKwh; }
    public void setPricePerKwh(double pricePerKwh) { this.pricePerKwh = pricePerKwh; }
    public LocalDateTime getSessionStart() { return sessionStart; }
    public void setSessionStart(LocalDateTime sessionStart) { this.sessionStart = sessionStart; }
    public LocalDateTime getSessionEnd() { return sessionEnd; }
    public void setSessionEnd(LocalDateTime sessionEnd) { this.sessionEnd = sessionEnd; }
}
