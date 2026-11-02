package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.abcgreen.evcharging.model.ChargingStation;

public class StationDao extends BaseDao {

    private static final String SELECT = "SELECT s.*, v.total_chargers, v.fast_chargers, v.slow_chargers, v.available, v.occupied, "
            + "v.faulty, v.maintenance, v.capacity_kw FROM stations s JOIN v_station_capacity v ON v.station_id = s.station_id ";

    private static ChargingStation map(ResultSet rs) throws SQLException {
        ChargingStation s = new ChargingStation();
        s.setStationId(rs.getInt("station_id"));
        s.setStationCode(rs.getString("station_code"));
        s.setName(rs.getString("name"));
        s.setCity(rs.getString("city"));
        s.setAddress(rs.getString("address"));
        s.setLatitude(rs.getDouble("latitude"));
        s.setLongitude(rs.getDouble("longitude"));
        s.setOperatingHours(rs.getString("operating_hours"));
        s.setContactPhone(rs.getString("contact_phone"));
        s.setStatus(rs.getString("status"));
        s.setOpenedOn(date(rs, "opened_on"));
        s.setTotalChargers(rs.getInt("total_chargers"));
        s.setFastChargers(rs.getInt("fast_chargers"));
        s.setSlowChargers(rs.getInt("slow_chargers"));
        s.setAvailableChargers(rs.getInt("available"));
        s.setOccupiedChargers(rs.getInt("occupied"));
        s.setFaultyChargers(rs.getInt("faulty"));
        s.setMaintenanceChargers(rs.getInt("maintenance"));
        s.setCapacityKw(rs.getDouble("capacity_kw"));
        return s;
    }

    public List<ChargingStation> findAll() {
        return query(SELECT + "ORDER BY s.city, s.name", StationDao::map);
    }

    public List<ChargingStation> search(String keyword, String city) {
        String k = like(keyword);
        String c = city == null || city.isBlank() || city.startsWith("All") ? "%" : city;
        return query(SELECT + "WHERE (s.name LIKE ? OR s.station_code LIKE ? OR s.address LIKE ?) AND s.city LIKE ? ORDER BY s.city, s.name",
                StationDao::map, k, k, k, c);
    }

    public ChargingStation findById(int id) {
        return queryOne(SELECT + "WHERE s.station_id = ?", StationDao::map, id);
    }

    public List<String> findCities() {
        return query("SELECT DISTINCT city FROM stations ORDER BY city", rs -> rs.getString(1));
    }

    public boolean codeExists(String code, int excludeId) {
        return queryInt("SELECT COUNT(*) FROM stations WHERE station_code = ? AND station_id <> ?", code, excludeId) > 0;
    }

    public int insert(ChargingStation s) {
        int id = insert("INSERT INTO stations (station_code, name, city, address, latitude, longitude, operating_hours, contact_phone, status, opened_on) VALUES (?,?,?,?,?,?,?,?,?,?)",
                s.getStationCode(), s.getName(), s.getCity(), s.getAddress(), s.getLatitude(), s.getLongitude(),
                s.getOperatingHours(), s.getContactPhone(), s.getStatus(), s.getOpenedOn());
        s.setStationId(id);
        return id;
    }

    public void update(ChargingStation s) {
        update("UPDATE stations SET station_code=?, name=?, city=?, address=?, latitude=?, longitude=?, operating_hours=?, contact_phone=?, status=? WHERE station_id=?",
                s.getStationCode(), s.getName(), s.getCity(), s.getAddress(), s.getLatitude(), s.getLongitude(),
                s.getOperatingHours(), s.getContactPhone(), s.getStatus(), s.getStationId());
    }

    public void delete(int id) {
        update("DELETE FROM stations WHERE station_id=?", id);
    }

    public int count() {
        return queryInt("SELECT COUNT(*) FROM stations");
    }
}
