package com.abcgreen.evcharging.model;

import java.time.LocalDate;

/** Scheduled maintenance of a charger. */
public class MaintenanceRecord {

    public static final String[] TYPES = { "PREVENTIVE", "CORRECTIVE", "INSPECTION", "UPGRADE" };

    private int maintenanceId;
    private int chargerId;
    private String chargerCode;
    private String stationName;
    private LocalDate scheduledDate;
    private String maintenanceType;
    private String description;
    private String technician;
    private String status = "SCHEDULED";
    private LocalDate completedOn;

    public int getMaintenanceId() { return maintenanceId; }
    public void setMaintenanceId(int maintenanceId) { this.maintenanceId = maintenanceId; }
    public int getChargerId() { return chargerId; }
    public void setChargerId(int chargerId) { this.chargerId = chargerId; }
    public String getChargerCode() { return chargerCode; }
    public void setChargerCode(String chargerCode) { this.chargerCode = chargerCode; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }
    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTechnician() { return technician; }
    public void setTechnician(String technician) { this.technician = technician; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getCompletedOn() { return completedOn; }
    public void setCompletedOn(LocalDate completedOn) { this.completedOn = completedOn; }
}
