package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.User;
import com.abcgreen.evcharging.service.AuthService;

/** Customer self-registration (creates user account + customer profile). */
public class RegisterServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient AuthService auth = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        render(req, resp, "register");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Customer c = auth.registerCustomer(req.getParameter("username"), req.getParameter("password"), req.getParameter("confirm"),
                    req.getParameter("fullName"), req.getParameter("email"), req.getParameter("phone"), req.getParameter("city"), req.getParameter("address"));
            User user = auth.login(req.getParameter("username"), req.getParameter("password"));
            HttpSession session = req.getSession(true);
            session.setAttribute("user", user);
            session.setAttribute("customer", c);
            flash(req, "success", "Welcome " + c.getFullName() + "! Your account is ready. Add your vehicle to start booking.");
            redirect(req, resp, "/customer/vehicles");
        } catch (EVChargingException e) {
            req.setAttribute("error", e.getMessage());
            render(req, resp, "register");
        }
    }
}
