package com.abcgreen.evcharging.dao;

import java.util.List;

import com.abcgreen.evcharging.model.Notification;

public class NotificationDao extends BaseDao {

    public void insert(String type, String message) {
        update("INSERT INTO notifications (type, message) VALUES (?, ?)", type, message.length() > 255 ? message.substring(0, 255) : message);
    }

    public List<Notification> findRecent(int limit) {
        return query("SELECT * FROM notifications ORDER BY created_at DESC, notification_id DESC LIMIT ?", rs -> {
            Notification n = new Notification(rs.getString("type"), rs.getString("message"));
            n.setNotificationId(rs.getInt("notification_id"));
            n.setRead(rs.getBoolean("is_read"));
            n.setCreatedAt(dateTime(rs, "created_at"));
            return n;
        }, limit);
    }

    public int countUnread() {
        return queryInt("SELECT COUNT(*) FROM notifications WHERE is_read = FALSE");
    }

    public void markAllRead() {
        update("UPDATE notifications SET is_read = TRUE WHERE is_read = FALSE");
    }
}
