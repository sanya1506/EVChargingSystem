package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.abcgreen.evcharging.model.Customer;

public class CustomerDao extends BaseDao {

    private static final String SELECT = "SELECT c.*, (SELECT COUNT(*) FROM vehicles v WHERE v.customer_id = c.customer_id) AS vehicle_count FROM customers c ";

    private static Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getInt("customer_id"));
        c.setUserId(nullableInt(rs, "user_id"));
        c.setFullName(rs.getString("full_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setCity(rs.getString("city"));
        c.setAddress(rs.getString("address"));
        c.setStatus(rs.getString("status"));
        c.setRegisteredOn(dateTime(rs, "registered_on"));
        c.setVehicleCount(rs.getInt("vehicle_count"));
        return c;
    }

    public List<Customer> search(String keyword, int limit) {
        String k = like(keyword);
        return query(SELECT + "WHERE c.full_name LIKE ? OR c.email LIKE ? OR c.phone LIKE ? OR c.city LIKE ? "
                + "ORDER BY c.customer_id DESC LIMIT ?", CustomerDao::map, k, k, k, k, limit);
    }

    public Customer findById(int id) {
        return queryOne(SELECT + "WHERE c.customer_id = ?", CustomerDao::map, id);
    }

    public Customer findByUserId(int userId) {
        return queryOne(SELECT + "WHERE c.user_id = ?", CustomerDao::map, userId);
    }

    public boolean emailExists(String email, int excludeId) {
        return queryInt("SELECT COUNT(*) FROM customers WHERE email = ? AND customer_id <> ?", email, excludeId) > 0;
    }

    public boolean phoneExists(String phone, int excludeId) {
        return queryInt("SELECT COUNT(*) FROM customers WHERE phone = ? AND customer_id <> ?", phone, excludeId) > 0;
    }

    public int insert(Customer c) {
        int id = insert("INSERT INTO customers (user_id, full_name, email, phone, city, address, status) VALUES (?,?,?,?,?,?,?)",
                c.getUserId(), c.getFullName(), c.getEmail(), c.getPhone(), c.getCity(), c.getAddress(), c.getStatus());
        c.setCustomerId(id);
        return id;
    }

    public void update(Customer c) {
        update("UPDATE customers SET full_name=?, email=?, phone=?, city=?, address=?, status=? WHERE customer_id=?",
                c.getFullName(), c.getEmail(), c.getPhone(), c.getCity(), c.getAddress(), c.getStatus(), c.getCustomerId());
    }

    public void delete(int id) {
        update("DELETE FROM customers WHERE customer_id=?", id);
    }

    public int count() {
        return queryInt("SELECT COUNT(*) FROM customers");
    }
}
