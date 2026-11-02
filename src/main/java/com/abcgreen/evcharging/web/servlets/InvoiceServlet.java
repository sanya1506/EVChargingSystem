package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.model.Role;
import com.abcgreen.evcharging.service.BillingService;
import com.abcgreen.evcharging.service.InvoiceGenerator;

/** View or download an invoice (HTML, printable to PDF from the browser). */
public class InvoiceServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient BillingService billing = new BillingService();
    private final transient InvoiceGenerator generator = new InvoiceGenerator();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int id = intParam(req, "id", 0);
            Payment p = user(req).getRole() == Role.CUSTOMER ? PaymentServlet.ownInvoice(billing, id, customer(req).getEmail()) : billing.findById(id);
            resp.setContentType("text/html;charset=UTF-8");
            if (req.getParameter("download") != null) {
                resp.setHeader("Content-Disposition", "attachment; filename=\"" + p.getInvoiceNo() + ".html\"");
            }
            resp.getWriter().write(generator.toHtml(p));
        } catch (EVChargingException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        }
    }
}
