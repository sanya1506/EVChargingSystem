package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.model.ChargingStation;
import com.abcgreen.evcharging.service.StationService;

/**
 * Small JSON API: GET /api/stations?city=Pune returns live charger availability.
 * Useful for a future mobile app / GIS map integration.
 */
public class StationApiServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient StationService stations = new StationService();

    private static String json(String s) {
        return s == null ? "null" : "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<ChargingStation> list = stations.search(req.getParameter("q"), req.getParameter("city"));
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.print("[");
        for (int i = 0; i < list.size(); i++) {
            ChargingStation s = list.get(i);
            out.printf(java.util.Locale.ROOT, "%s{\"id\":%d,\"code\":%s,\"name\":%s,\"city\":%s,\"status\":%s,\"latitude\":%.6f,\"longitude\":%.6f,"
                    + "\"totalChargers\":%d,\"available\":%d,\"occupied\":%d}",
                    i == 0 ? "" : ",", s.getStationId(), json(s.getStationCode()), json(s.getName()), json(s.getCity()), json(s.getStatus()),
                    s.getLatitude(), s.getLongitude(), s.getTotalChargers(), s.getAvailableChargers(), s.getOccupiedChargers());
        }
        out.print("]");
    }
}
