package com.abcgreen.evcharging.model;

import java.time.LocalDate;

/**
 * Abstract charger. FastCharger and SlowCharger override the charging physics
 * and pricing rules (polymorphism).
 */
public abstract class Charger implements Billable {

    private int chargerId;
    private int stationId;
    private String stationName;
    private String chargerCode;
    private String connectorType;
    private double powerKw;
    private double pricePerKwh;
    private ChargerStatus status = ChargerStatus.AVAILABLE;
    private LocalDate installedOn = LocalDate.now();

    /** Factory method - creates the right subclass for a charger type. */
    public static Charger create(ChargerType type) {
        return type == ChargerType.FAST ? new FastCharger() : new SlowCharger();
    }

    public abstract ChargerType getType();

    /** Fraction of grid energy that reaches the battery. */
    public abstract double getEfficiency();

    /**
     * Energy delivered (kWh) for a charging duration, limited by the space left in the battery.
     */
    public double calculateEnergyDelivered(double minutes, double batteryCapacityKwh, int startSoc) {
        double spaceLeft = batteryCapacityKwh * (100 - startSoc) / 100.0;
        double energy = effectivePowerKw(batteryCapacityKwh) * getEfficiency() * minutes / 60.0;
        return Math.max(0, Math.min(energy, spaceLeft));
    }

    /** Power actually accepted by the vehicle. Subclasses may limit this. */
    protected double effectivePowerKw(double batteryCapacityKwh) {
        return powerKw;
    }

    /** Minutes needed to charge from startSoc to targetSoc. */
    public int estimateMinutes(double batteryCapacityKwh, int startSoc, int targetSoc) {
        double energy = batteryCapacityKwh * Math.max(0, targetSoc - startSoc) / 100.0;
        return (int) Math.ceil(energy / (effectivePowerKw(batteryCapacityKwh) * getEfficiency()) * 60);
    }

    @Override
    public double calculateEnergyCharge(double energyKwh) {
        return energyKwh * pricePerKwh;
    }

    public boolean isAvailable() {
        return status == ChargerStatus.AVAILABLE;
    }

    public int getChargerId() { return chargerId; }
    public void setChargerId(int chargerId) { this.chargerId = chargerId; }
    public int getStationId() { return stationId; }
    public void setStationId(int stationId) { this.stationId = stationId; }
    public String getStationName() { return stationName; }
    public void setStationName(String stationName) { this.stationName = stationName; }
    public String getChargerCode() { return chargerCode; }
    public void setChargerCode(String chargerCode) { this.chargerCode = chargerCode; }
    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }
    public double getPowerKw() { return powerKw; }
    public void setPowerKw(double powerKw) { this.powerKw = powerKw; }
    public double getPricePerKwh() { return pricePerKwh; }
    public void setPricePerKwh(double pricePerKwh) { this.pricePerKwh = pricePerKwh; }
    public ChargerStatus getStatus() { return status; }
    public void setStatus(ChargerStatus status) { this.status = status; }
    public LocalDate getInstalledOn() { return installedOn; }
    public void setInstalledOn(LocalDate installedOn) { this.installedOn = installedOn; }

    @Override
    public String toString() {
        return chargerCode + " (" + getType() + " " + (int) powerKw + " kW, " + status + ")";
    }
}
