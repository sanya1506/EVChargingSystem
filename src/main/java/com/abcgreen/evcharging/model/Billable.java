package com.abcgreen.evcharging.model;

/** Anything that can price an amount of energy (implemented by chargers). */
public interface Billable {

    double GST_RATE = 0.18;

    double calculateEnergyCharge(double energyKwh);

    double getServiceFee();
}
