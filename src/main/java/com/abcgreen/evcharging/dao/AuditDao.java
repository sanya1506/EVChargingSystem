package com.abcgreen.evcharging.dao;

import com.abcgreen.evcharging.model.ReportData;

public class AuditDao extends BaseDao {

    public void log(String username, String action, String entity, Object entityId, String details) {
        update("INSERT INTO audit_log (username, action, entity, entity_id, details) VALUES (?,?,?,?,?)",
                username == null ? "system" : username, action, entity, entityId == null ? null : entityId.toString(),
                details != null && details.length() > 255 ? details.substring(0, 255) : details);
    }

    public ReportData recent(int limit) {
        ReportData report = new ReportData("Audit Log", "Time", "User", "Action", "Entity", "ID", "Details");
        query("SELECT * FROM audit_log ORDER BY created_at DESC, log_id DESC LIMIT ?", rs -> {
            report.addRow(dateTime(rs, "created_at"), rs.getString("username"), rs.getString("action"),
                    rs.getString("entity"), rs.getString("entity_id"), rs.getString("details"));
            return null;
        }, limit);
        return report;
    }
}
