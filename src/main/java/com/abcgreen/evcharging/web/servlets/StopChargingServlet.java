package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.service.ChargingService;

/**
 * Customer stops their own charging session (POST /customer/stop).
 * The bill is generated immediately and the customer is taken straight to the payment page.
 */
public class StopChargingServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient ChargingService charging = new ChargingService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            Payment bill = charging.stopSessionForCustomer(intParam(req, "sessionId", 0), customer(req).getCustomerId());
            flash(req, "success", String.format("Charging stopped: %d min, %.2f kWh delivered. Your bill %s is ready - total Rs %.2f.",
                    bill.getDurationMinutes(), bill.getEnergyKwh(), bill.getInvoiceNo(), bill.getTotalAmount()));
            redirect(req, resp, "/customer/pay?id=" + bill.getPaymentId());
        } catch (EVChargingException e) {
            flash(req, "error", e.getMessage());
            redirect(req, resp, "/customer/dashboard");
        }
    }
}
