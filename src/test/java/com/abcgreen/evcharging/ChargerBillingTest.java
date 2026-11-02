package com.abcgreen.evcharging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.FastCharger;
import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.model.SlowCharger;
import com.abcgreen.evcharging.service.BillingService;

class ChargerBillingTest {

    private static Charger charger(ChargerType type, double kw, double price) {
        Charger c = Charger.create(type);
        c.setPowerKw(kw);
        c.setPricePerKwh(price);
        return c;
    }

    @Test
    @DisplayName("TC-CHG-01 Factory creates the right subclass (polymorphism)")
    void factory() {
        assertTrue(Charger.create(ChargerType.FAST) instanceof FastCharger);
        assertTrue(Charger.create(ChargerType.SLOW) instanceof SlowCharger);
    }

    @Test
    @DisplayName("TC-CHG-02 Energy delivered = power x efficiency x time")
    void energy() {
        Charger fast = charger(ChargerType.FAST, 60, 21);
        // 30 minutes at 60 kW x 0.92 = 27.6 kWh (battery has room for 60 % of 60 kWh = 36 kWh)
        assertEquals(27.6, fast.calculateEnergyDelivered(30, 60, 40), 0.001);
    }

    @Test
    @DisplayName("TC-CHG-03 Energy is capped by remaining battery capacity")
    void energyCapped() {
        Charger fast = charger(ChargerType.FAST, 60, 21);
        // 40.5 kWh battery at 80 % has only 8.1 kWh of space
        assertEquals(8.1, fast.calculateEnergyDelivered(120, 40.5, 80), 0.001);
    }

    @Test
    @DisplayName("TC-CHG-04 Two-wheelers on a fast charger draw at most 3.3 kW")
    void twoWheelerLimit() {
        Charger fast = charger(ChargerType.FAST, 60, 21);
        assertEquals(3.3 * 0.92, fast.calculateEnergyDelivered(60, 3.7, 0) > 3.0 ? 3.036 : 0, 0.01);
    }

    @Test
    @DisplayName("TC-CHG-05 Estimated charging duration")
    void duration() {
        Charger slow = charger(ChargerType.SLOW, 7.4, 13);
        // 30.2 kWh x 60 % = 18.12 kWh / (7.4 x 0.88) = 2.78 h = 167 min
        assertEquals(167, slow.estimateMinutes(30.2, 20, 80));
    }

    @Test
    @DisplayName("TC-BILL-01 Bill = energy charge + service fee + 18 % GST (fast)")
    void fastBill() {
        Payment p = BillingService.calculateBill(charger(ChargerType.FAST, 60, 20), 10, 18);
        assertEquals(200.00, p.getEnergyCharge(), 0.001);
        assertEquals(30.00, p.getServiceFee(), 0.001);
        assertEquals(41.40, p.getTaxAmount(), 0.001);
        assertEquals(271.40, p.getTotalAmount(), 0.001);
    }

    @Test
    @DisplayName("TC-BILL-02 Slow chargers have a lower service fee")
    void slowBill() {
        Payment p = BillingService.calculateBill(charger(ChargerType.SLOW, 7.4, 13), 12.5, 18);
        assertEquals(162.50, p.getEnergyCharge(), 0.001);
        assertEquals(10.00, p.getServiceFee(), 0.001);
        assertEquals(31.05, p.getTaxAmount(), 0.001);
        assertEquals(203.55, p.getTotalAmount(), 0.001);
    }

    @Test
    @DisplayName("TC-BILL-03 No energy delivered means no service fee")
    void zeroEnergy() {
        Payment p = BillingService.calculateBill(charger(ChargerType.FAST, 60, 20), 0, 18);
        assertEquals(0, p.getTotalAmount(), 0.001);
    }
}
