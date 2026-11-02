package com.abcgreen.evcharging.model;

import java.time.LocalDateTime;

/** A login account. */
public class User extends Person {

    private int userId;
    private String username;
    private String passwordHash;
    private Role role;
    private boolean active = true;
    private int failedAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public User() {
    }

    public User(String username, String fullName, String email, String phone, Role role) {
        super(fullName, email, phone);
        this.username = username;
        this.role = role;
    }

    @Override
    public String getDisplayRole() {
        return role == null ? "User" : role.getLabel();
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
}
