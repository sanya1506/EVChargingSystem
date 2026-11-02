package com.abcgreen.evcharging.model;

import java.time.LocalDateTime;

/** A customer waiting for a charger at a station. Ordered by request time (FIFO). */
public class QueueEntry implements Comparable<QueueEntry> {

    private int queueId;
    private int stationId;
    private int customerId;
    private int vehicleId;
    private ChargerType chargerType;
    private LocalDateTime requestedAt;
    private int estimatedWaitMin;
    private String status = "WAITING";
    private Integer allocatedChargerId;
    private LocalDateTime allocatedAt;
    private int position;
    // display fields
    private String customerName;
    private String vehicleRegNo;
    private String stationName;
    private String allocatedChargerCode;

    @Override
    public int compareTo(QueueEntry other) {
        return requestedAt.compareTo(other.requestedAt);
    }

    public int getQueueId() { return queueId; }
    public void setQueueId(int queueId) { this.queueId = queueId; }
    public int getStationId() { return stationId; }
    public void setStationId(int stationId) { this.stationId = stationId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public ChargerType getChargerType() { return chargerType; }
    public void setChargerType(ChargerType chargerType) { this.chargerType = chargerType; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public int getEstimatedWaitMin() { return estimatedWaitMin; }
    public void setEstimatedWaitMin(int estimatedWaitMin) { this.estimatedWaitMin = estimatedWaitMin; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getAllocatedChargerId() { return allocatedChargerId; }
    public void setAllocatedChargerId(Integer allocatedChargerId) { this.allocatedChargerId = allocatedChargerId; }
    public LocalDateTime getAllocatedAt() { return allocatedAt; }
    public void setAllocatedAt(LocalDateTime allocatedAt) { this.allocatedAt = allocatedAt; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getVehicleRegNo() { return vehicleRegNo; }
    public void setVehicleRegNo(String vehicleRegNo) { this.vehicleRegNo = vehicleRegNo; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getAllocatedChargerCode() { return allocatedChargerCode; }
    public void setAllocatedChargerCode(String allocatedChargerCode) { this.allocatedChargerCode = allocatedChargerCode; }
}
