package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.service.BillingService;
import com.abcgreen.evcharging.service.ChargingService;

/** Customer charging history and invoices. */
public class HistoryServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient ChargingService charging = new ChargingService();
    private final transient BillingService billing = new BillingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int id = customer(req).getCustomerId();
        req.setAttribute("sessions", charging.historyOf(id));
        req.setAttribute("payments", billing.paymentsOf(id));
        render(req, resp, "customer/history");
    }
}
