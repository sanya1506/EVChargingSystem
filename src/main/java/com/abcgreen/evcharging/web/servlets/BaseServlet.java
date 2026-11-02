package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.User;

/** Common helpers for all servlets: logged-in user, flash messages, JSP forwarding. */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected User user(HttpServletRequest req) {
        return (User) req.getSession().getAttribute("user");
    }

    protected Customer customer(HttpServletRequest req) {
        return (Customer) req.getSession().getAttribute("customer");
    }

    protected void render(HttpServletRequest req, HttpServletResponse resp, String jsp) throws ServletException, IOException {
        Object flash = req.getSession().getAttribute("flash");
        if (flash != null) {
            req.setAttribute("flash", flash);
            req.setAttribute("flashType", req.getSession().getAttribute("flashType"));
            req.getSession().removeAttribute("flash");
            req.getSession().removeAttribute("flashType");
        }
        req.getRequestDispatcher("/WEB-INF/jsp/" + jsp + ".jsp").forward(req, resp);
    }

    /** Stores a message shown once on the next page (Post/Redirect/Get pattern). */
    protected void flash(HttpServletRequest req, String type, String message) {
        req.getSession().setAttribute("flash", message);
        req.getSession().setAttribute("flashType", type);
    }

    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    protected int intParam(HttpServletRequest req, String name, int def) {
        try {
            return Integer.parseInt(req.getParameter(name).trim());
        } catch (RuntimeException e) {
            return def;
        }
    }
}
