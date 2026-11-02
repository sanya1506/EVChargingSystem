package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.QueueEntry;

public class QueueDao extends BaseDao {

    private static final String SELECT = "SELECT q.*, c.full_name AS customer_name, v.registration_no, s.name AS station_name, ch.charger_code "
            + "FROM queue_entries q JOIN customers c ON c.customer_id = q.customer_id JOIN vehicles v ON v.vehicle_id = q.vehicle_id "
            + "JOIN stations s ON s.station_id = q.station_id LEFT JOIN chargers ch ON ch.charger_id = q.allocated_charger_id ";

    private static QueueEntry map(ResultSet rs) throws SQLException {
        QueueEntry q = new QueueEntry();
        q.setQueueId(rs.getInt("queue_id"));
        q.setStationId(rs.getInt("station_id"));
        q.setCustomerId(rs.getInt("customer_id"));
        q.setVehicleId(rs.getInt("vehicle_id"));
        q.setChargerType(ChargerType.valueOf(rs.getString("charger_type")));
        q.setRequestedAt(dateTime(rs, "requested_at"));
        q.setEstimatedWaitMin(rs.getInt("estimated_wait_min"));
        q.setStatus(rs.getString("status"));
        q.setAllocatedChargerId(nullableInt(rs, "allocated_charger_id"));
        q.setAllocatedAt(dateTime(rs, "allocated_at"));
        q.setCustomerName(rs.getString("customer_name"));
        q.setVehicleRegNo(rs.getString("registration_no"));
        q.setStationName(rs.getString("station_name"));
        q.setAllocatedChargerCode(rs.getString("charger_code"));
        return q;
    }

    public int insert(QueueEntry q) {
        int id = insert("INSERT INTO queue_entries (station_id, customer_id, vehicle_id, charger_type, requested_at, estimated_wait_min, status) VALUES (?,?,?,?,?,?,'WAITING')",
                q.getStationId(), q.getCustomerId(), q.getVehicleId(), q.getChargerType(), q.getRequestedAt(), q.getEstimatedWaitMin());
        q.setQueueId(id);
        return id;
    }

    /** All waiting entries in arrival order (FIFO). */
    public List<QueueEntry> findWaiting() {
        return query(SELECT + "WHERE q.status = 'WAITING' ORDER BY q.requested_at, q.queue_id", QueueDao::map);
    }

    public List<QueueEntry> findRecent(int limit) {
        return query(SELECT + "ORDER BY q.requested_at DESC LIMIT ?", QueueDao::map, limit);
    }

    public List<QueueEntry> findByCustomer(int customerId) {
        return query(SELECT + "WHERE q.customer_id = ? ORDER BY q.requested_at DESC LIMIT 20", QueueDao::map, customerId);
    }

    public int countWaitingAhead(int stationId, ChargerType type, LocalDateTime before) {
        return queryInt("SELECT COUNT(*) FROM queue_entries WHERE station_id = ? AND charger_type = ? AND status = 'WAITING' AND requested_at < ?",
                stationId, type, before);
    }

    public boolean vehicleWaiting(int vehicleId) {
        return queryInt("SELECT COUNT(*) FROM queue_entries WHERE vehicle_id = ? AND status = 'WAITING'", vehicleId) > 0;
    }

    public int countWaiting() {
        return queryInt("SELECT COUNT(*) FROM queue_entries WHERE status = 'WAITING'");
    }

    public void updateEstimate(int queueId, int minutes) {
        update("UPDATE queue_entries SET estimated_wait_min=? WHERE queue_id=?", minutes, queueId);
    }

    /** Marks the entry allocated only if it is still waiting. @return true on success */
    public boolean allocate(int queueId, int chargerId) {
        return update("UPDATE queue_entries SET status='ALLOCATED', allocated_charger_id=?, allocated_at=?, estimated_wait_min=0 "
                + "WHERE queue_id=? AND status='WAITING'", chargerId, LocalDateTime.now(), queueId) == 1;
    }

    public boolean cancel(int queueId) {
        return update("UPDATE queue_entries SET status='CANCELLED' WHERE queue_id=? AND status='WAITING'", queueId) == 1;
    }
}
