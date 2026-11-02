package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.RecordNotFoundException;
import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.service.BillingService;

/** Online payment (SIMULATED gateway) for a charging invoice. */
public class PaymentServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient BillingService billing = new BillingService();

    /** Loads the invoice and checks it belongs to the logged-in customer. */
    static Payment ownInvoice(BillingService billing, int paymentId, String customerEmail) throws RecordNotFoundException {
        Payment p = billing.findById(paymentId);
        if (!p.getCustomerEmail().equalsIgnoreCase(customerEmail)) {
            throw new RecordNotFoundException("Invoice not found.");
        }
        return p;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Payment p = ownInvoice(billing, intParam(req, "id", 0), customer(req).getEmail());
            if (p.isPaid()) {
                flash(req, "success", "Invoice " + p.getInvoiceNo() + " is already paid.");
                redirect(req, resp, "/customer/history");
                return;
            }
            req.setAttribute("payment", p);
            render(req, resp, "customer/pay");
        } catch (EVChargingException e) {
            flash(req, "error", e.getMessage());
            redirect(req, resp, "/customer/history");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int id = intParam(req, "id", 0);
        try {
            ownInvoice(billing, id, customer(req).getEmail());
            Payment p = billing.pay(id, req.getParameter("method"), req.getParameter("detail"));
            flash(req, "success", "Payment of Rs " + String.format("%.2f", p.getTotalAmount()) + " successful. Transaction ref " + p.getTransactionRef() + ".");
            redirect(req, resp, "/customer/history");
        } catch (EVChargingException e) {
            flash(req, "error", e.getMessage());
            redirect(req, resp, "/customer/pay?id=" + id);
        }
    }
}
