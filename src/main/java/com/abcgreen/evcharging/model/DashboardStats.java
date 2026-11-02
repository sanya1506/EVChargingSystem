package com.abcgreen.evcharging.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Key figures and chart series shown on the dashboards. */
public class DashboardStats {

    private int totalStations;
    private int totalChargers;
    private int availableChargers;
    private int occupiedChargers;
    private int faultyChargers;
    private int maintenanceChargers;
    private int totalCustomers;
    private int totalVehicles;
    private int activeSessions;
    private int queueLength;
    private int todaySessions;
    private double todayEnergyKwh;
    private double todayRevenue;
    private double monthRevenue;
    private int pendingPayments;
    private int unreadNotifications;
    /** Sessions per hour of day (index 0-23). */
    private int[] hourlyDemand = new int[24];
    /** Date label -> energy kWh, last 30 days. */
    private Map<String, Double> dailyEnergy = new LinkedHashMap<>();
    /** City -> revenue this month. */
    private Map<String, Double> revenueByCity = new LinkedHashMap<>();

    public int getTotalStations() { return totalStations; }
    public void setTotalStations(int totalStations) { this.totalStations = totalStations; }
    public int getTotalChargers() { return totalChargers; }
    public void setTotalChargers(int totalChargers) { this.totalChargers = totalChargers; }
    public int getAvailableChargers() { return availableChargers; }
    public void setAvailableChargers(int availableChargers) { this.availableChargers = availableChargers; }
    public int getOccupiedChargers() { return occupiedChargers; }
    public void setOccupiedChargers(int occupiedChargers) { this.occupiedChargers = occupiedChargers; }
    public int getFaultyChargers() { return faultyChargers; }
    public void setFaultyChargers(int faultyChargers) { this.faultyChargers = faultyChargers; }
    public int getMaintenanceChargers() { return maintenanceChargers; }
    public void setMaintenanceChargers(int maintenanceChargers) { this.maintenanceChargers = maintenanceChargers; }
    public int getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(int totalCustomers) { this.totalCustomers = totalCustomers; }
    public int getTotalVehicles() { return totalVehicles; }
    public void setTotalVehicles(int totalVehicles) { this.totalVehicles = totalVehicles; }
    public int getActiveSessions() { return activeSessions; }
    public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }
    public int getQueueLength() { return queueLength; }
    public void setQueueLength(int queueLength) { this.queueLength = queueLength; }
    public int getTodaySessions() { return todaySessions; }
    public void setTodaySessions(int todaySessions) { this.todaySessions = todaySessions; }
    public double getTodayEnergyKwh() { return todayEnergyKwh; }
    public void setTodayEnergyKwh(double todayEnergyKwh) { this.todayEnergyKwh = todayEnergyKwh; }
    public double getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(double todayRevenue) { this.todayRevenue = todayRevenue; }
    public double getMonthRevenue() { return monthRevenue; }
    public void setMonthRevenue(double monthRevenue) { this.monthRevenue = monthRevenue; }
    public int getPendingPayments() { return pendingPayments; }
    public void setPendingPayments(int pendingPayments) { this.pendingPayments = pendingPayments; }
    public int getUnreadNotifications() { return unreadNotifications; }
    public void setUnreadNotifications(int unreadNotifications) { this.unreadNotifications = unreadNotifications; }
    public int[] getHourlyDemand() { return hourlyDemand; }
    public void setHourlyDemand(int[] hourlyDemand) { this.hourlyDemand = hourlyDemand; }
    public Map<String, Double> getDailyEnergy() { return dailyEnergy; }
    public void setDailyEnergy(Map<String, Double> dailyEnergy) { this.dailyEnergy = dailyEnergy; }
    public Map<String, Double> getRevenueByCity() { return revenueByCity; }
    public void setRevenueByCity(Map<String, Double> revenueByCity) { this.revenueByCity = revenueByCity; }
}
