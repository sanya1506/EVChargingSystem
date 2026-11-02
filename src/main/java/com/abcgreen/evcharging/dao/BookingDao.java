package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.abcgreen.evcharging.model.Booking;

public class BookingDao extends BaseDao {

    private static final String SELECT = "SELECT b.*, c.full_name AS customer_name, v.registration_no, s.name AS station_name, ch.charger_code "
            + "FROM bookings b JOIN customers c ON c.customer_id = b.customer_id JOIN vehicles v ON v.vehicle_id = b.vehicle_id "
            + "JOIN stations s ON s.station_id = b.station_id LEFT JOIN chargers ch ON ch.charger_id = b.charger_id ";

    private static Booking map(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getInt("booking_id"));
        b.setCustomerId(rs.getInt("customer_id"));
        b.setVehicleId(rs.getInt("vehicle_id"));
        b.setStationId(rs.getInt("station_id"));
        b.setChargerId(nullableInt(rs, "charger_id"));
        b.setSlotStart(dateTime(rs, "slot_start"));
        b.setSlotEnd(dateTime(rs, "slot_end"));
        b.setStatus(rs.getString("status"));
        b.setCreatedAt(dateTime(rs, "created_at"));
        b.setCustomerName(rs.getString("customer_name"));
        b.setVehicleRegNo(rs.getString("registration_no"));
        b.setStationName(rs.getString("station_name"));
        b.setChargerCode(rs.getString("charger_code"));
        return b;
    }

    public int insert(Booking b) {
        int id = insert("INSERT INTO bookings (customer_id, vehicle_id, station_id, charger_id, slot_start, slot_end, status) VALUES (?,?,?,?,?,?,?)",
                b.getCustomerId(), b.getVehicleId(), b.getStationId(), b.getChargerId(), b.getSlotStart(), b.getSlotEnd(), b.getStatus());
        b.setBookingId(id);
        return id;
    }

    public Booking findById(int id) {
        return queryOne(SELECT + "WHERE b.booking_id = ?", BookingDao::map, id);
    }

    public List<Booking> findByCustomer(int customerId) {
        return query(SELECT + "WHERE b.customer_id = ? ORDER BY b.slot_start DESC LIMIT 100", BookingDao::map, customerId);
    }

    /** Bookings from today onwards (operator view). */
    public List<Booking> findUpcoming(String status) {
        String st = status == null || status.startsWith("All") ? "%" : status;
        return query(SELECT + "WHERE b.slot_end >= CURDATE() AND b.status LIKE ? ORDER BY b.slot_start LIMIT 500", BookingDao::map, st);
    }

    /** True when the charger already has an active booking overlapping the requested slot. */
    public boolean hasOverlap(int chargerId, LocalDateTime start, LocalDateTime end) {
        return queryInt("SELECT COUNT(*) FROM bookings WHERE charger_id = ? AND status IN ('BOOKED','IN_PROGRESS') "
                + "AND slot_start < ? AND slot_end > ?", chargerId, end, start) > 0;
    }

    public void updateStatus(int bookingId, String status) {
        update("UPDATE bookings SET status=? WHERE booking_id=?", status, bookingId);
    }
}
