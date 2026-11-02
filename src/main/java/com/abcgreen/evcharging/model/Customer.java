package com.abcgreen.evcharging.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** An EV owner who books and pays for charging. */
public class Customer extends Person {

    private int customerId;
    private Integer userId;
    private String city;
    private String address;
    private String status = "ACTIVE";
    private LocalDateTime registeredOn;
    private int vehicleCount;
    private final List<Vehicle> vehicles = new ArrayList<>();

    public Customer() {
    }

    public Customer(String fullName, String email, String phone, String city, String address) {
        super(fullName, email, phone);
        this.city = city;
        this.address = address;
    }

    @Override
    public String getDisplayRole() {
        return "Customer";
    }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRegisteredOn() { return registeredOn; }
    public void setRegisteredOn(LocalDateTime registeredOn) { this.registeredOn = registeredOn; }
    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }
    public List<Vehicle> getVehicles() { return vehicles; }

    @Override
    public String toString() {
        return getFullName() + " - " + getPhone();
    }
}
