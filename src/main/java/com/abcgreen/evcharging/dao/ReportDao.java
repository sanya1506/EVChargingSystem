package com.abcgreen.evcharging.dao;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import com.abcgreen.evcharging.model.DashboardStats;
import com.abcgreen.evcharging.model.ReportData;

/** Aggregate (GROUP BY) queries for dashboards, demand management and reports. */
public class ReportDao extends BaseDao {

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public DashboardStats dashboard() {
        DashboardStats d = new DashboardStats();
        query("SELECT COUNT(*), SUM(status='AVAILABLE'), SUM(status='OCCUPIED'), SUM(status='FAULTY'), SUM(status='MAINTENANCE') FROM chargers", rs -> {
            d.setTotalChargers(rs.getInt(1));
            d.setAvailableChargers(rs.getInt(2));
            d.setOccupiedChargers(rs.getInt(3));
            d.setFaultyChargers(rs.getInt(4));
            d.setMaintenanceChargers(rs.getInt(5));
            return null;
        });
        d.setTotalStations(queryInt("SELECT COUNT(*) FROM stations"));
        d.setTotalCustomers(queryInt("SELECT COUNT(*) FROM customers"));
        d.setTotalVehicles(queryInt("SELECT COUNT(*) FROM vehicles"));
        d.setActiveSessions(queryInt("SELECT COUNT(*) FROM charging_sessions WHERE status='ACTIVE'"));
        d.setQueueLength(queryInt("SELECT COUNT(*) FROM queue_entries WHERE status='WAITING'"));
        query("SELECT COUNT(*), COALESCE(SUM(energy_kwh),0) FROM charging_sessions WHERE DATE(start_time) = CURDATE()", rs -> {
            d.setTodaySessions(rs.getInt(1));
            d.setTodayEnergyKwh(round2(rs.getDouble(2)));
            return null;
        });
        d.setTodayRevenue(round2(queryDouble("SELECT COALESCE(SUM(total_amount),0) FROM payments WHERE status='PAID' AND DATE(paid_at) = CURDATE()")));
        d.setMonthRevenue(round2(queryDouble("SELECT COALESCE(SUM(total_amount),0) FROM payments WHERE status='PAID' "
                + "AND paid_at >= DATE_FORMAT(CURDATE(), '%Y-%m-01')")));
        d.setPendingPayments(queryInt("SELECT COUNT(*) FROM payments WHERE status IN ('PENDING','FAILED')"));
        d.setUnreadNotifications(queryInt("SELECT COUNT(*) FROM notifications WHERE is_read = FALSE"));

        int[] hourly = new int[24];
        query("SELECT HOUR(start_time), COUNT(*) FROM charging_sessions WHERE start_time >= CURDATE() - INTERVAL 30 DAY GROUP BY HOUR(start_time)", rs -> {
            hourly[rs.getInt(1)] = rs.getInt(2);
            return null;
        });
        d.setHourlyDemand(hourly);

        Map<String, Double> daily = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            daily.put(LocalDate.now().minusDays(i).toString().substring(5), 0.0);
        }
        query("SELECT DATE(start_time), SUM(energy_kwh) FROM charging_sessions WHERE start_time >= CURDATE() - INTERVAL 29 DAY "
                + "GROUP BY DATE(start_time)", rs -> {
                    daily.put(rs.getDate(1).toLocalDate().toString().substring(5), round2(rs.getDouble(2)));
                    return null;
                });
        d.setDailyEnergy(daily);

        Map<String, Double> byCity = new LinkedHashMap<>();
        query("SELECT s.city, SUM(p.total_amount) FROM payments p JOIN charging_sessions cs ON cs.session_id = p.session_id "
                + "JOIN chargers ch ON ch.charger_id = cs.charger_id JOIN stations s ON s.station_id = ch.station_id "
                + "WHERE p.status = 'PAID' AND p.paid_at >= CURDATE() - INTERVAL 30 DAY GROUP BY s.city ORDER BY 2 DESC", rs -> {
                    byCity.put(rs.getString(1), round2(rs.getDouble(2)));
                    return null;
                });
        d.setRevenueByCity(byCity);
        return d;
    }

    /** FR-11: Daily operations report for one day, per station. */
    public ReportData dailyReport(LocalDate day) {
        ReportData r = new ReportData("Daily Operations Report - " + day, "Station", "City", "Sessions", "Energy (kWh)",
                "Avg Duration (min)", "Revenue (Rs)");
        query("SELECT s.name, s.city, COUNT(cs.session_id), COALESCE(SUM(cs.energy_kwh),0), COALESCE(AVG(cs.duration_minutes),0), "
                + "COALESCE(SUM(CASE WHEN p.status='PAID' THEN p.total_amount END),0) "
                + "FROM stations s JOIN chargers ch ON ch.station_id = s.station_id "
                + "LEFT JOIN charging_sessions cs ON cs.charger_id = ch.charger_id AND DATE(cs.start_time) = ? "
                + "LEFT JOIN payments p ON p.session_id = cs.session_id GROUP BY s.station_id, s.name, s.city ORDER BY 3 DESC", rs -> {
                    r.addRow(rs.getString(1), rs.getString(2), rs.getInt(3), round2(rs.getDouble(4)), Math.round(rs.getDouble(5)), round2(rs.getDouble(6)));
                    return null;
                }, day);
        return r;
    }

    /** FR-11: Monthly utilisation per station = charging minutes / available charger minutes. */
    public ReportData monthlyUtilisation(YearMonth month) {
        ReportData r = new ReportData("Monthly Utilisation Report - " + month, "Station", "City", "Chargers", "Sessions",
                "Energy (kWh)", "Charging Hours", "Utilisation %", "Revenue (Rs)");
        int minutesInMonth = month.lengthOfMonth() * 24 * 60;
        query("SELECT s.name, s.city, (SELECT COUNT(*) FROM chargers c2 WHERE c2.station_id = s.station_id) AS chargers, "
                + "COUNT(cs.session_id), COALESCE(SUM(cs.energy_kwh),0), COALESCE(SUM(cs.duration_minutes),0), "
                + "COALESCE(SUM(CASE WHEN p.status='PAID' THEN p.total_amount END),0) "
                + "FROM stations s JOIN chargers ch ON ch.station_id = s.station_id "
                + "LEFT JOIN charging_sessions cs ON cs.charger_id = ch.charger_id AND cs.start_time >= ? AND cs.start_time < ? "
                + "LEFT JOIN payments p ON p.session_id = cs.session_id GROUP BY s.station_id, s.name, s.city ORDER BY 6 DESC", rs -> {
                    int chargers = rs.getInt(3);
                    double minutes = rs.getDouble(6);
                    double util = chargers == 0 ? 0 : minutes * 100.0 / (chargers * (double) minutesInMonth);
                    r.addRow(rs.getString(1), rs.getString(2), chargers, rs.getInt(4), round2(rs.getDouble(5)),
                            round2(minutes / 60.0), round2(util), round2(rs.getDouble(7)));
                    return null;
                }, month.atDay(1), month.plusMonths(1).atDay(1));
        return r;
    }

    /** Peak hour demand across the network for the last N days. */
    public ReportData peakHourDemand(int days) {
        ReportData r = new ReportData("Peak Hour Demand - last " + days + " days", "Hour", "Sessions", "Energy (kWh)", "Avg Sessions / Day", "Demand Level");
        int[] sessions = new int[24];
        double[] energy = new double[24];
        query("SELECT HOUR(start_time), COUNT(*), COALESCE(SUM(energy_kwh),0) FROM charging_sessions "
                + "WHERE start_time >= CURDATE() - INTERVAL ? DAY GROUP BY HOUR(start_time)", rs -> {
                    sessions[rs.getInt(1)] = rs.getInt(2);
                    energy[rs.getInt(1)] = rs.getDouble(3);
                    return null;
                }, days);
        int max = 1;
        for (int s : sessions) {
            max = Math.max(max, s);
        }
        for (int h = 0; h < 24; h++) {
            double ratio = sessions[h] / (double) max;
            String level = ratio >= 0.75 ? "PEAK" : ratio >= 0.4 ? "MEDIUM" : "LOW";
            r.addRow(String.format("%02d:00-%02d:59", h, h), sessions[h], round2(energy[h]), round2(sessions[h] / (double) days), level);
        }
        return r;
    }

    public ReportData dailyDemand(int days) {
        ReportData r = new ReportData("Daily Demand - last " + days + " days", "Date", "Sessions", "Energy (kWh)", "Unique Customers", "Revenue (Rs)");
        query("SELECT DATE(cs.start_time) d, COUNT(*), COALESCE(SUM(cs.energy_kwh),0), COUNT(DISTINCT cs.customer_id), "
                + "COALESCE(SUM(CASE WHEN p.status='PAID' THEN p.total_amount END),0) FROM charging_sessions cs "
                + "LEFT JOIN payments p ON p.session_id = cs.session_id WHERE cs.start_time >= CURDATE() - INTERVAL ? DAY "
                + "GROUP BY d ORDER BY d DESC", rs -> {
                    r.addRow(rs.getDate(1).toLocalDate(), rs.getInt(2), round2(rs.getDouble(3)), rs.getInt(4), round2(rs.getDouble(5)));
                    return null;
                }, days);
        return r;
    }

    public ReportData monthlyDemand() {
        ReportData r = new ReportData("Monthly Demand", "Month", "Sessions", "Energy (kWh)", "Unique Customers", "Revenue (Rs)");
        query("SELECT DATE_FORMAT(cs.start_time, '%Y-%m') m, COUNT(*), COALESCE(SUM(cs.energy_kwh),0), COUNT(DISTINCT cs.customer_id), "
                + "COALESCE(SUM(CASE WHEN p.status='PAID' THEN p.total_amount END),0) FROM charging_sessions cs "
                + "LEFT JOIN payments p ON p.session_id = cs.session_id GROUP BY m ORDER BY m DESC", rs -> {
                    r.addRow(rs.getString(1), rs.getInt(2), round2(rs.getDouble(3)), rs.getInt(4), round2(rs.getDouble(5)));
                    return null;
                });
        return r;
    }

    /** Historical hourly demand records (demand_records table) for a station. */
    public ReportData demandRecords(Integer stationId, LocalDate from, LocalDate to) {
        ReportData r = new ReportData("Historical Demand Records", "Station Code", "Station", "Date", "Hour", "Sessions", "Energy (kWh)");
        String stationFilter = stationId == null ? "" : " AND d.station_id = " + stationId.intValue();
        query("SELECT s.station_code, s.name, d.demand_date, d.hour_of_day, d.sessions_count, d.energy_kwh FROM demand_records d "
                + "JOIN stations s ON s.station_id = d.station_id WHERE d.demand_date BETWEEN ? AND ?" + stationFilter
                + " ORDER BY d.demand_date DESC, s.station_code, d.hour_of_day LIMIT 5000", rs -> {
                    r.addRow(rs.getString(1), rs.getString(2), rs.getDate(3).toLocalDate(), rs.getInt(4), rs.getInt(5), round2(rs.getDouble(6)));
                    return null;
                }, from, to);
        return r;
    }

    /** Inserts or adds to an hourly demand record (used when a session completes and by CSV import). */
    public void upsertDemand(int stationId, LocalDate date, int hour, int sessions, double energy) {
        update("INSERT INTO demand_records (station_id, demand_date, hour_of_day, sessions_count, energy_kwh) VALUES (?,?,?,?,?) "
                + "ON DUPLICATE KEY UPDATE sessions_count = sessions_count + VALUES(sessions_count), energy_kwh = energy_kwh + VALUES(energy_kwh)",
                stationId, date, hour, sessions, energy);
    }

    public Integer stationIdByCode(String code) {
        return queryOne("SELECT station_id FROM stations WHERE station_code = ?", rs -> rs.getInt(1), code);
    }

    public ReportData stationUtilisation() {
        ReportData r = new ReportData("Station Utilisation (live)", "Code", "Station", "City", "Total", "Available", "Occupied",
                "Faulty", "Maintenance", "Capacity (kW)", "Utilisation %");
        query("SELECT * FROM v_station_capacity ORDER BY city, name", rs -> {
            int avail = rs.getInt("available");
            int occ = rs.getInt("occupied");
            double util = avail + occ == 0 ? 0 : occ * 100.0 / (avail + occ);
            r.addRow(rs.getString("station_code"), rs.getString("name"), rs.getString("city"), rs.getInt("total_chargers"), avail, occ,
                    rs.getInt("faulty"), rs.getInt("maintenance"), round2(rs.getDouble("capacity_kw")), round2(util));
            return null;
        });
        return r;
    }

    public ReportData revenueReport(int days) {
        ReportData r = new ReportData("Revenue by City and Payment Method - last " + days + " days", "City", "Method", "Payments", "Energy Charges (Rs)",
                "Tax (Rs)", "Total (Rs)");
        query("SELECT s.city, p.method, COUNT(*), SUM(p.energy_charge + p.service_fee), SUM(p.tax_amount), SUM(p.total_amount) FROM payments p "
                + "JOIN charging_sessions cs ON cs.session_id = p.session_id JOIN chargers ch ON ch.charger_id = cs.charger_id "
                + "JOIN stations s ON s.station_id = ch.station_id WHERE p.status = 'PAID' AND p.paid_at >= CURDATE() - INTERVAL ? DAY "
                + "GROUP BY s.city, p.method ORDER BY s.city, 6 DESC", rs -> {
                    r.addRow(rs.getString(1), rs.getString(2), rs.getInt(3), round2(rs.getDouble(4)), round2(rs.getDouble(5)), round2(rs.getDouble(6)));
                    return null;
                }, days);
        return r;
    }

    public ReportData topCustomers(int limit) {
        ReportData r = new ReportData("Top Customers by Energy", "Customer", "City", "Sessions", "Energy (kWh)", "Amount Paid (Rs)");
        query("SELECT c.full_name, c.city, COUNT(cs.session_id), COALESCE(SUM(cs.energy_kwh),0), "
                + "COALESCE(SUM(CASE WHEN p.status='PAID' THEN p.total_amount END),0) FROM customers c "
                + "JOIN charging_sessions cs ON cs.customer_id = c.customer_id LEFT JOIN payments p ON p.session_id = cs.session_id "
                + "GROUP BY c.customer_id, c.full_name, c.city ORDER BY 4 DESC LIMIT ?", rs -> {
                    r.addRow(rs.getString(1), rs.getString(2), rs.getInt(3), round2(rs.getDouble(4)), round2(rs.getDouble(5)));
                    return null;
                }, limit);
        return r;
    }
}
