package com.abcgreen.evcharging.service;

import java.util.List;

import com.abcgreen.evcharging.dao.NotificationDao;
import com.abcgreen.evcharging.model.Notification;
import com.abcgreen.evcharging.util.AppLogger;

/** In-app operator notifications (shown in the dashboard notification panel). */
public class NotificationService {

    private final NotificationDao dao = new NotificationDao();

    public void notifyOperators(String type, String message) {
        AppLogger.info("NOTIFY [" + type + "] " + message);
        dao.insert(type, message);
    }

    public List<Notification> recent(int limit) {
        return dao.findRecent(limit);
    }

    public int unreadCount() {
        return dao.countUnread();
    }

    public void markAllRead() {
        dao.markAllRead();
    }
}
