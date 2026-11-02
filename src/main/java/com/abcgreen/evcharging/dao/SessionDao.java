package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.abcgreen.evcharging.model.ChargingSession;

public class SessionDao extends BaseDao {

    private static final String SELECT = "SELECT cs.*, c.full_name AS customer_name, v.registration_no, ch.charger_code, ch.charger_type, "
            + "s.name AS station_name, p.status AS payment_status FROM charging_sessions cs "
            + "JOIN customers c ON c.customer_id = cs.customer_id JOIN vehicles v ON v.vehicle_id = cs.vehicle_id "
            + "JOIN chargers ch ON ch.charger_id = cs.charger_id JOIN stations s ON s.station_id = ch.station_id "
            + "LEFT JOIN payments p ON p.session_id = cs.session_id ";

    private static ChargingSession map(ResultSet rs) throws SQLException {
        ChargingSession s = new ChargingSession();
        s.setSessionId(rs.getInt("session_id"));
        s.setBookingId(nullableInt(rs, "booking_id"));
        s.setCustomerId(rs.getInt("customer_id"));
        s.setVehicleId(rs.getInt("vehicle_id"));
        s.setChargerId(rs.getInt("charger_id"));
        s.setStartTime(dateTime(rs, "start_time"));
        s.setEndTime(dateTime(rs, "end_time"));
        s.setStartSoc(rs.getInt("start_soc"));
        s.setEndSoc(nullableInt(rs, "end_soc"));
        s.setEnergyKwh(rs.getDouble("energy_kwh"));
        s.setDurationMinutes(rs.getInt("duration_minutes"));
        s.setCost(rs.getDouble("cost"));
        s.setStatus(rs.getString("status"));
        s.setCustomerName(rs.getString("customer_name"));
        s.setVehicleRegNo(rs.getString("registration_no"));
        s.setChargerCode(rs.getString("charger_code"));
        s.setChargerType(rs.getString("charger_type"));
        s.setStationName(rs.getString("station_name"));
        s.setPaymentStatus(rs.getString("payment_status"));
        return s;
    }

    public int insert(ChargingSession s) {
        int id = insert("INSERT INTO charging_sessions (booking_id, customer_id, vehicle_id, charger_id, start_time, start_soc, status) VALUES (?,?,?,?,?,?,'ACTIVE')",
                s.getBookingId(), s.getCustomerId(), s.getVehicleId(), s.getChargerId(), s.getStartTime(), s.getStartSoc());
        s.setSessionId(id);
        return id;
    }

    public void complete(ChargingSession s) {
        update("UPDATE charging_sessions SET end_time=?, end_soc=?, energy_kwh=?, duration_minutes=?, cost=?, status=? WHERE session_id=?",
                s.getEndTime(), s.getEndSoc(), s.getEnergyKwh(), s.getDurationMinutes(), s.getCost(), s.getStatus(), s.getSessionId());
    }

    public ChargingSession findById(int id) {
        return queryOne(SELECT + "WHERE cs.session_id = ?", SessionDao::map, id);
    }

    public List<ChargingSession> findActive() {
        return query(SELECT + "WHERE cs.status = 'ACTIVE' ORDER BY cs.start_time", SessionDao::map);
    }

    public boolean vehicleHasActiveSession(int vehicleId) {
        return queryInt("SELECT COUNT(*) FROM charging_sessions WHERE vehicle_id = ? AND status = 'ACTIVE'", vehicleId) > 0;
    }

    public List<ChargingSession> findByCustomer(int customerId) {
        return query(SELECT + "WHERE cs.customer_id = ? ORDER BY cs.start_time DESC LIMIT 200", SessionDao::map, customerId);
    }

    public List<ChargingSession> search(String keyword, String status, int limit) {
        String k = like(keyword);
        String st = status == null || status.startsWith("All") ? "%" : status;
        return query(SELECT + "WHERE (c.full_name LIKE ? OR v.registration_no LIKE ? OR s.name LIKE ? OR ch.charger_code LIKE ?) "
                + "AND cs.status LIKE ? ORDER BY cs.start_time DESC LIMIT ?", SessionDao::map, k, k, k, k, st, limit);
    }

    /** Average duration of completed sessions on a charger type (used for queue wait estimates). */
    public double averageDuration(int stationId, String chargerType) {
        double avg = queryDouble("SELECT AVG(cs.duration_minutes) FROM charging_sessions cs JOIN chargers ch ON ch.charger_id = cs.charger_id "
                + "WHERE ch.station_id = ? AND ch.charger_type = ? AND cs.status = 'COMPLETED'", stationId, chargerType);
        return avg > 0 ? avg : ("FAST".equals(chargerType) ? 45 : 180);
    }

    /** Minutes already elapsed for each active session on a station's chargers of a type. */
    public List<Integer> activeElapsedMinutes(int stationId, String chargerType) {
        return query("SELECT TIMESTAMPDIFF(MINUTE, cs.start_time, NOW()) FROM charging_sessions cs JOIN chargers ch ON ch.charger_id = cs.charger_id "
                + "WHERE ch.station_id = ? AND ch.charger_type = ? AND cs.status = 'ACTIVE'", rs -> rs.getInt(1), stationId, chargerType);
    }
}
