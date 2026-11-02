package com.abcgreen.evcharging.model;

/** DC fast charger (typically 30-150 kW). */
public class FastCharger extends Charger {

    @Override
    public ChargerType getType() {
        return ChargerType.FAST;
    }

    @Override
    public double getEfficiency() {
        return 0.92;
    }

    /** Small two-wheeler batteries cannot accept full DC power. */
    @Override
    protected double effectivePowerKw(double batteryCapacityKwh) {
        return batteryCapacityKwh < 10 ? Math.min(getPowerKw(), 3.3) : getPowerKw();
    }

    @Override
    public double getServiceFee() {
        return 30.0;
    }
}
