package com.abcgreen.evcharging.model;

import java.time.LocalDateTime;

/** In-app notification shown on the operator dashboard. */
public class Notification {

    private int notificationId;
    private String type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {
    }

    public Notification(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
