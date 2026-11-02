package com.abcgreen.evcharging.model;

/** AC slow charger (typically 3.3-22 kW). */
public class SlowCharger extends Charger {

    @Override
    public ChargerType getType() {
        return ChargerType.SLOW;
    }

    @Override
    public double getEfficiency() {
        return 0.88;
    }

    @Override
    public double getServiceFee() {
        return 10.0;
    }
}
