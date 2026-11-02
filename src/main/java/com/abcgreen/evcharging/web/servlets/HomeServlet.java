package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.service.StationService;

/** Public landing page with the station finder (search by name / city, live availability). */
public class HomeServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient StationService stations = new StationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!"/".equals(req.getServletPath()) && !"/stations".equals(req.getServletPath()) && !req.getServletPath().isEmpty()) {
            resp.sendError(404);
            return;
        }
        String q = req.getParameter("q");
        String city = req.getParameter("city");
        req.setAttribute("stations", stations.search(q, city));
        req.setAttribute("cities", stations.cities());
        render(req, resp, "index");
    }
}
