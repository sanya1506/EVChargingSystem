package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.model.ReportData;
import com.abcgreen.evcharging.service.ReportService;
import com.abcgreen.evcharging.util.CsvUtil;

/** Web reports: view in the browser or download as CSV (?format=csv). */
public class AdminReportServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient ReportService reports = new ReportService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String type = req.getParameter("type");
        if (type == null) {
            type = ReportService.REPORT_TYPES[0];
        }
        LocalDate day;
        YearMonth month;
        try {
            day = req.getParameter("day") == null ? LocalDate.now() : LocalDate.parse(req.getParameter("day"));
            month = req.getParameter("month") == null ? YearMonth.now() : YearMonth.parse(req.getParameter("month"));
        } catch (DateTimeParseException e) {
            day = LocalDate.now();
            month = YearMonth.now();
        }
        ReportData data = reports.generate(type, day, month);
        if ("csv".equals(req.getParameter("format"))) {
            resp.setContentType("text/csv;charset=UTF-8");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + data.getTitle().replaceAll("[^A-Za-z0-9]+", "_") + ".csv\"");
            CsvUtil.write(resp.getWriter(), data.getColumns(), data.getRows());
            return;
        }
        req.setAttribute("report", data);
        req.setAttribute("types", ReportService.REPORT_TYPES);
        req.setAttribute("type", type);
        req.setAttribute("day", day.toString());
        req.setAttribute("month", month.toString());
        render(req, resp, "admin/reports");
    }
}
