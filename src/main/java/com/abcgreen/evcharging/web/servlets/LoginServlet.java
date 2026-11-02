package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.model.Role;
import com.abcgreen.evcharging.model.User;
import com.abcgreen.evcharging.service.AuthService;
import com.abcgreen.evcharging.service.CustomerService;

/** FR-01 Web login (customers and staff). Handles HTTP GET (form) and POST (credentials). */
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient AuthService auth = new AuthService();
    private final transient CustomerService customers = new CustomerService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        render(req, resp, "login");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            User user = auth.login(req.getParameter("username"), req.getParameter("password"));
            // new session id after login prevents session fixation
            HttpSession old = req.getSession(false);
            if (old != null) {
                old.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setMaxInactiveInterval(30 * 60);
            session.setAttribute("user", user);
            if (user.getRole() == Role.CUSTOMER) {
                session.setAttribute("customer", customers.findByUserId(user.getUserId()));
                redirect(req, resp, "/customer/dashboard");
            } else {
                redirect(req, resp, "/admin/dashboard");
            }
        } catch (EVChargingException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("username", req.getParameter("username"));
            render(req, resp, "login");
        }
    }
}
