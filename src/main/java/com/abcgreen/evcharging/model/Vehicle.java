package com.abcgreen.evcharging.model;

/** An electric vehicle registered to a customer. */
public class Vehicle {

    public static final String[] TYPES = { "CAR", "BIKE", "SCOOTER", "AUTO", "BUS" };
    public static final String[] CONNECTORS = { "CCS2", "TYPE2", "CHADEMO", "BHARAT_AC001", "BHARAT_DC001" };

    private int vehicleId;
    private int customerId;
    private String customerName;
    private String registrationNo;
    private String make;
    private String model;
    private String vehicleType;
    private double batteryCapacityKwh;
    private String connectorType;

    public Vehicle() {
    }

    public Vehicle(int customerId, String registrationNo, String make, String model, String vehicleType,
                   double batteryCapacityKwh, String connectorType) {
        this.customerId = customerId;
        this.registrationNo = registrationNo;
        this.make = make;
        this.model = model;
        this.vehicleType = vehicleType;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.connectorType = connectorType;
    }

    public int getVehicleId() { return vehicleId; }
    public void setVehicleId(int vehicleId) { this.vehicleId = vehicleId; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }
    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public double getBatteryCapacityKwh() { return batteryCapacityKwh; }
    public void setBatteryCapacityKwh(double batteryCapacityKwh) { this.batteryCapacityKwh = batteryCapacityKwh; }
    public String getConnectorType() { return connectorType; }
    public void setConnectorType(String connectorType) { this.connectorType = connectorType; }

    @Override
    public String toString() {
        return registrationNo + " - " + make + " " + model;
    }
}
