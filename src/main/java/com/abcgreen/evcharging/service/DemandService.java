package com.abcgreen.evcharging.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.abcgreen.evcharging.dao.ReportDao;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.ReportData;
import com.abcgreen.evcharging.util.CsvUtil;

/** FR-08 Demand data management: historical demand records and CSV import / export. */
public class DemandService {

    public static final String[] CSV_HEADER = { "station_code", "date", "hour", "sessions", "energy_kwh" };

    private final ReportDao reportDao = new ReportDao();
    private final AuditService audit = new AuditService();

    public ReportData records(Integer stationId, LocalDate from, LocalDate to) {
        return reportDao.demandRecords(stationId, from, to);
    }

    public ReportData peakHours(int days) {
        return reportDao.peakHourDemand(days);
    }

    public ReportData daily(int days) {
        return reportDao.dailyDemand(days);
    }

    public ReportData monthly() {
        return reportDao.monthlyDemand();
    }

    /**
     * Imports demand data from CSV (station_code,date,hour,sessions,energy_kwh).
     * Invalid lines are skipped and counted; the method returns a summary message.
     */
    public String importCsv(File file) throws IOException, ValidationException {
        List<String[]> rows = CsvUtil.read(file);
        if (rows.isEmpty()) {
            throw new ValidationException("The CSV file has no data rows.");
        }
        int imported = 0;
        int skipped = 0;
        for (String[] r : rows) {
            try {
                if (r.length < 5) {
                    throw new IllegalArgumentException("not enough columns");
                }
                Integer stationId = reportDao.stationIdByCode(r[0].trim());
                if (stationId == null) {
                    throw new IllegalArgumentException("unknown station " + r[0]);
                }
                int hour = Integer.parseInt(r[2].trim());
                if (hour < 0 || hour > 23) {
                    throw new IllegalArgumentException("hour out of range");
                }
                reportDao.upsertDemand(stationId, LocalDate.parse(r[1].trim()), hour, Integer.parseInt(r[3].trim()), Double.parseDouble(r[4].trim()));
                imported++;
            } catch (IllegalArgumentException | DateTimeParseException e) {
                skipped++;
            }
        }
        audit.log("IMPORT", "DEMAND", null, file.getName() + ": " + imported + " imported, " + skipped + " skipped");
        return imported + " demand records imported, " + skipped + " invalid rows skipped.";
    }

    public void exportCsv(ReportData data, File file) throws IOException {
        new ReportService().exportCsv(data, file);
    }
}
