package com.abcgreen.evcharging.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A charging station location that owns a list of chargers (composition). */
public class ChargingStation {

    public static final String[] STATUSES = { "ACTIVE", "INACTIVE", "MAINTENANCE" };

    private int stationId;
    private String stationCode;
    private String name;
    private String city;
    private String address;
    private double latitude;
    private double longitude;
    private String operatingHours = "00:00-23:59";
    private String contactPhone;
    private String status = "ACTIVE";
    private LocalDate openedOn = LocalDate.now();

    // Capacity figures (filled from the v_station_capacity view)
    private int totalChargers;
    private int fastChargers;
    private int slowChargers;
    private int availableChargers;
    private int occupiedChargers;
    private int faultyChargers;
    private int maintenanceChargers;
    private double capacityKw;

    private final List<Charger> chargers = new ArrayList<>();

    /** Percentage of working chargers currently in use. */
    public double getUtilisationPercent() {
        int working = availableChargers + occupiedChargers;
        return working == 0 ? 0 : occupiedChargers * 100.0 / working;
    }

    /** Counts chargers of a given status using a lambda (Module IV). */
    public long countChargers(ChargerStatus status) {
        return chargers.stream().filter(c -> c.getStatus() == status).count();
    }

    public int getStationId() { return stationId; }
    public void setStationId(int stationId) { this.stationId = stationId; }
    public String getStationCode() { return stationCode; }
    public void setStationCode(String stationCode) { this.stationCode = stationCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getOpenedOn() { return openedOn; }
    public void setOpenedOn(LocalDate openedOn) { this.openedOn = openedOn; }
    public int getTotalChargers() { return totalChargers; }
    public void setTotalChargers(int totalChargers) { this.totalChargers = totalChargers; }
    public int getFastChargers() { return fastChargers; }
    public void setFastChargers(int fastChargers) { this.fastChargers = fastChargers; }
    public int getSlowChargers() { return slowChargers; }
    public void setSlowChargers(int slowChargers) { this.slowChargers = slowChargers; }
    public int getAvailableChargers() { return availableChargers; }
    public void setAvailableChargers(int availableChargers) { this.availableChargers = availableChargers; }
    public int getOccupiedChargers() { return occupiedChargers; }
    public void setOccupiedChargers(int occupiedChargers) { this.occupiedChargers = occupiedChargers; }
    public int getFaultyChargers() { return faultyChargers; }
    public void setFaultyChargers(int faultyChargers) { this.faultyChargers = faultyChargers; }
    public int getMaintenanceChargers() { return maintenanceChargers; }
    public void setMaintenanceChargers(int maintenanceChargers) { this.maintenanceChargers = maintenanceChargers; }
    public double getCapacityKw() { return capacityKw; }
    public void setCapacityKw(double capacityKw) { this.capacityKw = capacityKw; }
    public List<Charger> getChargers() { return chargers; }

    @Override
    public String toString() {
        return stationCode + " - " + name + " (" + city + ")";
    }
}
