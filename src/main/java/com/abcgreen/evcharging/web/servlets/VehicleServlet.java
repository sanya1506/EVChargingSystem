package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.Vehicle;
import com.abcgreen.evcharging.service.CustomerService;

/** FR-03 Vehicle registration from the customer portal. */
public class VehicleServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient CustomerService customers = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("vehicles", customers.vehiclesOf(customer(req).getCustomerId()));
        render(req, resp, "customer/vehicles");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer c = customer(req);
        try {
            if ("delete".equals(req.getParameter("action"))) {
                Vehicle v = customers.findVehicle(intParam(req, "vehicleId", 0));
                if (v.getCustomerId() != c.getCustomerId()) {
                    throw new EVChargingException("Vehicle not found.");
                }
                customers.deleteVehicle(v.getVehicleId());
                flash(req, "success", "Vehicle " + v.getRegistrationNo() + " removed.");
            } else {
                double battery;
                try {
                    battery = Double.parseDouble(req.getParameter("battery"));
                } catch (RuntimeException e) {
                    throw new EVChargingException("Battery capacity must be a number.");
                }
                Vehicle v = new Vehicle(c.getCustomerId(), req.getParameter("registrationNo"), req.getParameter("make"), req.getParameter("model"),
                        req.getParameter("type"), battery, req.getParameter("connector"));
                customers.saveVehicle(v);
                flash(req, "success", "Vehicle " + v.getRegistrationNo() + " registered.");
            }
        } catch (EVChargingException e) {
            flash(req, "error", e.getMessage());
        }
        redirect(req, resp, "/customer/vehicles");
    }
}
