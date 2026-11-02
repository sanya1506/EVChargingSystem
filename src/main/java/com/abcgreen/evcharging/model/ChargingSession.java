package com.abcgreen.evcharging.model;

import java.time.LocalDateTime;

/** One charging event on a charger. */
public class ChargingSession {

    private int sessionId;
    private Integer bookingId;
    private int customerId;
    private int vehicleId;
    private int chargerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int startSoc;
    private Integer endSoc;
    private double energyKwh;
    private int durationMinutes;
    private double cost;
    private String status = "ACTIVE";
    // display fields
    private String customerName;
    private String vehicleRegNo;
    private String chargerCode;
    private String chargerType;
    private String stationName;
    private String paymentStatus;

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }
    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public int getChargerId() { return chargerId; }
    public void setChargerId(int chargerId) { this.chargerId = chargerId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public int getStartSoc() { return startSoc; }
    public void setStartSoc(int startSoc) { this.startSoc = startSoc; }
    public Integer getEndSoc() { return endSoc; }
    public void setEndSoc(Integer endSoc) { this.endSoc = endSoc; }
    public double getEnergyKwh() { return energyKwh; }
    public void setEnergyKwh(double energyKwh) { this.energyKwh = energyKwh; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getVehicleRegNo() { return vehicleRegNo; }
    public void setVehicleRegNo(String vehicleRegNo) { this.vehicleRegNo = vehicleRegNo; }
    public String getChargerCode() { return chargerCode; }
    public void setChargerCode(String chargerCode) { this.chargerCode = chargerCode; }
    public String getChargerType() { return chargerType; }
    public void setChargerType(String chargerType) { this.chargerType = chargerType; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
}
