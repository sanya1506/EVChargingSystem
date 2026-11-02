package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.model.DashboardStats;
import com.abcgreen.evcharging.service.NotificationService;
import com.abcgreen.evcharging.service.QueueService;
import com.abcgreen.evcharging.service.ReportService;
import com.abcgreen.evcharging.service.StationService;

/** JSP dashboard for administrators / operators with Chart.js graphs. */
public class AdminDashboardServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient ReportService reports = new ReportService();
    private final transient NotificationService notifications = new NotificationService();
    private final transient StationService stations = new StationService();
    private final transient QueueService queue = new QueueService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        DashboardStats d = reports.dashboard();
        req.setAttribute("d", d);
        req.setAttribute("hourlyJson", Arrays.toString(d.getHourlyDemand()));
        req.setAttribute("dailyLabelsJson", d.getDailyEnergy().keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(",", "[", "]")));
        req.setAttribute("dailyValuesJson", d.getDailyEnergy().values().toString());
        req.setAttribute("cityLabelsJson", d.getRevenueByCity().keySet().stream().map(k -> "\"" + k + "\"").collect(Collectors.joining(",", "[", "]")));
        req.setAttribute("cityValuesJson", d.getRevenueByCity().values().toString());
        req.setAttribute("notifications", notifications.recent(12));
        req.setAttribute("stations", stations.findAll());
        req.setAttribute("queue", queue.waiting());
        render(req, resp, "admin/dashboard");
    }
}
