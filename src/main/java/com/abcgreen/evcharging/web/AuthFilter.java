package com.abcgreen.evcharging.web;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.abcgreen.evcharging.model.Role;
import com.abcgreen.evcharging.model.User;
import com.abcgreen.evcharging.service.SecurityContext;

/**
 * Role-based access control for the web portal:
 * /customer/* requires a CUSTOMER login, /admin/* requires ADMIN or OPERATOR.
 * Also sets UTF-8 encoding and the SecurityContext user for auditing.
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        HttpSession session = req.getSession(false);
        User user = session == null ? null : (User) session.getAttribute("user");
        String path = req.getServletPath();

        boolean customerArea = path.startsWith("/customer");
        boolean adminArea = path.startsWith("/admin");
        if ((customerArea || adminArea) && user == null) {
            resp.sendRedirect(req.getContextPath() + "/login?next=" + path);
            return;
        }
        if (customerArea && user.getRole() != Role.CUSTOMER || adminArea && !user.getRole().isStaff()) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have access to this page.");
            return;
        }
        SecurityContext.setRequestUser(user);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContext.clearRequestUser();
        }
    }
}
