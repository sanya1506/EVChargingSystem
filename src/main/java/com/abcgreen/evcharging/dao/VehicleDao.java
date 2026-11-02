package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.abcgreen.evcharging.model.Vehicle;

public class VehicleDao extends BaseDao {

    private static final String SELECT = "SELECT v.*, c.full_name AS customer_name FROM vehicles v JOIN customers c ON c.customer_id = v.customer_id ";

    private static Vehicle map(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setVehicleId(rs.getInt("vehicle_id"));
        v.setCustomerId(rs.getInt("customer_id"));
        v.setCustomerName(rs.getString("customer_name"));
        v.setRegistrationNo(rs.getString("registration_no"));
        v.setMake(rs.getString("make"));
        v.setModel(rs.getString("model"));
        v.setVehicleType(rs.getString("vehicle_type"));
        v.setBatteryCapacityKwh(rs.getDouble("battery_capacity_kwh"));
        v.setConnectorType(rs.getString("connector_type"));
        return v;
    }

    public List<Vehicle> search(String keyword, int limit) {
        String k = like(keyword);
        return query(SELECT + "WHERE v.registration_no LIKE ? OR v.make LIKE ? OR v.model LIKE ? OR c.full_name LIKE ? "
                + "ORDER BY v.vehicle_id DESC LIMIT ?", VehicleDao::map, k, k, k, k, limit);
    }

    public List<Vehicle> findByCustomer(int customerId) {
        return query(SELECT + "WHERE v.customer_id = ? ORDER BY v.vehicle_id", VehicleDao::map, customerId);
    }

    public Vehicle findById(int id) {
        return queryOne(SELECT + "WHERE v.vehicle_id = ?", VehicleDao::map, id);
    }

    public boolean registrationExists(String regNo, int excludeId) {
        return queryInt("SELECT COUNT(*) FROM vehicles WHERE registration_no = ? AND vehicle_id <> ?", regNo, excludeId) > 0;
    }

    public int insert(Vehicle v) {
        int id = insert("INSERT INTO vehicles (customer_id, registration_no, make, model, vehicle_type, battery_capacity_kwh, connector_type) VALUES (?,?,?,?,?,?,?)",
                v.getCustomerId(), v.getRegistrationNo(), v.getMake(), v.getModel(), v.getVehicleType(), v.getBatteryCapacityKwh(), v.getConnectorType());
        v.setVehicleId(id);
        return id;
    }

    public void update(Vehicle v) {
        update("UPDATE vehicles SET customer_id=?, registration_no=?, make=?, model=?, vehicle_type=?, battery_capacity_kwh=?, connector_type=? WHERE vehicle_id=?",
                v.getCustomerId(), v.getRegistrationNo(), v.getMake(), v.getModel(), v.getVehicleType(), v.getBatteryCapacityKwh(), v.getConnectorType(), v.getVehicleId());
    }

    public void delete(int id) {
        update("DELETE FROM vehicles WHERE vehicle_id=?", id);
    }

    public int count() {
        return queryInt("SELECT COUNT(*) FROM vehicles");
    }
}
