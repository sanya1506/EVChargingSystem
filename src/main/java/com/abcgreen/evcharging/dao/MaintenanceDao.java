package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import com.abcgreen.evcharging.model.MaintenanceRecord;

public class MaintenanceDao extends BaseDao {

    private static final String SELECT = "SELECT m.*, ch.charger_code, s.name AS station_name FROM maintenance_schedule m "
            + "JOIN chargers ch ON ch.charger_id = m.charger_id JOIN stations s ON s.station_id = ch.station_id ";

    private static MaintenanceRecord map(ResultSet rs) throws SQLException {
        MaintenanceRecord m = new MaintenanceRecord();
        m.setMaintenanceId(rs.getInt("maintenance_id"));
        m.setChargerId(rs.getInt("charger_id"));
        m.setChargerCode(rs.getString("charger_code"));
        m.setStationName(rs.getString("station_name"));
        m.setScheduledDate(date(rs, "scheduled_date"));
        m.setMaintenanceType(rs.getString("maintenance_type"));
        m.setDescription(rs.getString("description"));
        m.setTechnician(rs.getString("technician"));
        m.setStatus(rs.getString("status"));
        m.setCompletedOn(date(rs, "completed_on"));
        return m;
    }

    public List<MaintenanceRecord> search(String status) {
        String st = status == null || status.startsWith("All") ? "%" : status;
        return query(SELECT + "WHERE m.status LIKE ? ORDER BY (m.status = 'COMPLETED'), m.scheduled_date DESC LIMIT 500", MaintenanceDao::map, st);
    }

    public MaintenanceRecord findById(int id) {
        return queryOne(SELECT + "WHERE m.maintenance_id = ?", MaintenanceDao::map, id);
    }

    public int insert(MaintenanceRecord m) {
        return insert("INSERT INTO maintenance_schedule (charger_id, scheduled_date, maintenance_type, description, technician, status) VALUES (?,?,?,?,?,?)",
                m.getChargerId(), m.getScheduledDate(), m.getMaintenanceType(), m.getDescription(), m.getTechnician(), m.getStatus());
    }

    public void updateStatus(int id, String status) {
        update("UPDATE maintenance_schedule SET status=?, completed_on=? WHERE maintenance_id=?",
                status, "COMPLETED".equals(status) ? LocalDate.now() : null, id);
    }
}
