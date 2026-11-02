package com.abcgreen.evcharging.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

import com.abcgreen.evcharging.dao.ReportDao;
import com.abcgreen.evcharging.model.DashboardStats;
import com.abcgreen.evcharging.model.ReportData;
import com.abcgreen.evcharging.util.AppLogger;
import com.abcgreen.evcharging.util.CsvUtil;

/** FR-11 Report generation and CSV export. Generation time is measured (NFR: under 10 seconds). */
public class ReportService {

    public static final String[] REPORT_TYPES = {
        "Daily Operations Report",
        "Monthly Utilisation Report",
        "Peak Hour Demand (30 days)",
        "Daily Demand (30 days)",
        "Monthly Demand",
        "Station Utilisation (live)",
        "Revenue Report (30 days)",
        "Top Customers",
        "Audit Log"
    };

    private final ReportDao reportDao = new ReportDao();
    private final AuditService audit = new AuditService();

    public DashboardStats dashboard() {
        return reportDao.dashboard();
    }

    public ReportData generate(String type, LocalDate day, YearMonth month) {
        long start = System.currentTimeMillis();
        ReportData data;
        switch (type) {
            case "Daily Operations Report": data = reportDao.dailyReport(day); break;
            case "Monthly Utilisation Report": data = reportDao.monthlyUtilisation(month); break;
            case "Peak Hour Demand (30 days)": data = reportDao.peakHourDemand(30); break;
            case "Daily Demand (30 days)": data = reportDao.dailyDemand(30); break;
            case "Monthly Demand": data = reportDao.monthlyDemand(); break;
            case "Station Utilisation (live)": data = reportDao.stationUtilisation(); break;
            case "Revenue Report (30 days)": data = reportDao.revenueReport(30); break;
            case "Top Customers": data = reportDao.topCustomers(50); break;
            case "Audit Log": data = audit.recent(500); break;
            default: throw new IllegalArgumentException("Unknown report: " + type);
        }
        data.setGenerationMillis(System.currentTimeMillis() - start);
        AppLogger.info("Report '" + data.getTitle() + "' generated in " + data.getGenerationMillis() + " ms");
        return data;
    }

    public File exportCsv(ReportData data, File file) throws IOException {
        CsvUtil.write(file, data.getColumns(), data.getRows());
        audit.log("EXPORT", "REPORT", null, data.getTitle() + " -> " + file.getName());
        return file;
    }

    /** Default export location: reports/<title>_<timestamp>.csv */
    public static File defaultFile(ReportData data) {
        String name = data.getTitle().replaceAll("[^A-Za-z0-9]+", "_") + "_"
                + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        return new File("reports", name);
    }
}
