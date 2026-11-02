package com.abcgreen.evcharging.service;

import com.abcgreen.evcharging.dao.AuditDao;
import com.abcgreen.evcharging.model.ReportData;
import com.abcgreen.evcharging.util.AppLogger;

/** Writes the transaction / activity log to both the audit_log table and the log file. */
public class AuditService {

    private final AuditDao auditDao = new AuditDao();

    public void log(String action, String entity, Object entityId, String details) {
        String user = SecurityContext.username();
        AppLogger.info(user + " " + action + " " + entity + (entityId == null ? "" : " #" + entityId) + (details == null ? "" : " - " + details));
        try {
            auditDao.log(user, action, entity, entityId, details);
        } catch (RuntimeException e) {
            AppLogger.error("Audit write failed", e);
        }
    }

    public ReportData recent(int limit) {
        return auditDao.recent(limit);
    }
}
