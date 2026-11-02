package com.abcgreen.evcharging.model;

import java.time.LocalDateTime;

/** A reserved charging slot. */
public class Booking {

    private int bookingId;
    private int customerId;
    private int vehicleId;
    private int stationId;
    private Integer chargerId;
    private LocalDateTime slotStart;
    private LocalDateTime slotEnd;
    private String status = "BOOKED";
    private LocalDateTime createdAt;
    // display fields
    private String customerName;
    private String vehicleRegNo;
    private String stationName;
    private String chargerCode;

    public int getBookingId() { return bookingId; }
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public int getStationId() { return stationId; }
    public void setStationId(int stationId) { this.stationId = stationId; }
    public Integer getChargerId() { return chargerId; }
    public void setChargerId(Integer chargerId) { this.chargerId = chargerId; }
    public LocalDateTime getSlotStart() { return slotStart; }
    public void setSlotStart(LocalDateTime slotStart) { this.slotStart = slotStart; }
    public LocalDateTime getSlotEnd() { return slotEnd; }
    public void setSlotEnd(LocalDateTime slotEnd) { this.slotEnd = slotEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getVehicleRegNo() { return vehicleRegNo; }
    public void setVehicleRegNo(String vehicleRegNo) { this.vehicleRegNo = vehicleRegNo; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getChargerCode() { return chargerCode; }
    public void setChargerCode(String chargerCode) { this.chargerCode = chargerCode; }
}
