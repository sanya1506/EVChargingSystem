package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import com.abcgreen.evcharging.model.Role;
import com.abcgreen.evcharging.model.User;

public class UserDao extends BaseDao {

    private static User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        u.setFailedAttempts(rs.getInt("failed_attempts"));
        u.setCreatedAt(dateTime(rs, "created_at"));
        u.setLastLogin(dateTime(rs, "last_login"));
        return u;
    }

    public User findByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username = ? OR email = ?", UserDao::map, username, username);
    }

    public User findById(int id) {
        return queryOne("SELECT * FROM users WHERE user_id = ?", UserDao::map, id);
    }

    /** Staff accounts only (customer accounts are managed from the Customers screen). */
    public List<User> findStaff() {
        return query("SELECT * FROM users WHERE role <> 'CUSTOMER' ORDER BY role, username", UserDao::map);
    }

    public boolean usernameExists(String username) {
        return queryInt("SELECT COUNT(*) FROM users WHERE username = ?", username) > 0;
    }

    public boolean emailExists(String email) {
        return queryInt("SELECT COUNT(*) FROM users WHERE email = ?", email) > 0;
    }

    public int insert(User u) {
        int id = insert("INSERT INTO users (username, password_hash, full_name, email, phone, role, active) VALUES (?,?,?,?,?,?,?)",
                u.getUsername(), u.getPasswordHash(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), u.isActive());
        u.setUserId(id);
        return id;
    }

    public void update(User u) {
        update("UPDATE users SET full_name=?, email=?, phone=?, role=?, active=? WHERE user_id=?",
                u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), u.isActive(), u.getUserId());
    }

    public void updatePassword(int userId, String hash) {
        update("UPDATE users SET password_hash=?, failed_attempts=0 WHERE user_id=?", hash, userId);
    }

    public void recordLogin(int userId) {
        update("UPDATE users SET last_login=?, failed_attempts=0 WHERE user_id=?", LocalDateTime.now(), userId);
    }

    public void recordFailedAttempt(int userId, boolean lock) {
        update("UPDATE users SET failed_attempts = failed_attempts + 1, active = IF(?, FALSE, active) WHERE user_id=?", lock, userId);
    }

    public void delete(int userId) {
        update("DELETE FROM users WHERE user_id=?", userId);
    }
}
