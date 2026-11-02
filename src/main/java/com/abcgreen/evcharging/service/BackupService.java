package com.abcgreen.evcharging.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.util.DBConnection;
import com.abcgreen.evcharging.util.SqlScriptRunner;

/**
 * FR-12 Database backup and restore, implemented in pure JDBC so it works on any machine
 * without needing mysqldump on the PATH. Backup = data of every table as INSERT statements.
 */
public class BackupService {

    /** Parent tables first so foreign keys are satisfied on restore. */
    static final String[] TABLES = { "users", "customers", "vehicles", "stations", "chargers", "bookings", "charging_sessions",
        "queue_entries", "payments", "maintenance_schedule", "demand_records", "notifications", "audit_log", "system_config" };

    private final AuditService audit = new AuditService();

    public static File defaultBackupFile() {
        return new File("backups", "ev_backup_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql");
    }

    /** @return number of rows written */
    public int backup(File file) throws EVChargingException, IOException {
        AuthService.requireAdmin();
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        int total = 0;
        try (Connection conn = DBConnection.getConnection();
             BufferedWriter out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8);
             Statement st = conn.createStatement()) {
            out.write("-- EV Charging System database backup " + LocalDateTime.now() + "\n");
            out.write("SET FOREIGN_KEY_CHECKS=0;\n");
            for (String table : TABLES) {
                out.write("DELETE FROM " + table + ";\n");
            }
            for (String table : TABLES) {
                total += dumpTable(st, table, out);
            }
            out.write("SET FOREIGN_KEY_CHECKS=1;\n");
        } catch (SQLException e) {
            throw new EVChargingException("Backup failed: " + e.getMessage(), e);
        }
        audit.log("BACKUP", "DATABASE", null, file.getName() + " (" + total + " rows)");
        return total;
    }

    private int dumpTable(Statement st, String table, BufferedWriter out) throws SQLException, IOException {
        int rows = 0;
        try (ResultSet rs = st.executeQuery("SELECT * FROM " + table)) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            StringBuilder header = new StringBuilder("INSERT INTO " + table + " (");
            for (int i = 1; i <= cols; i++) {
                header.append(i > 1 ? "," : "").append(md.getColumnName(i));
            }
            header.append(") VALUES\n");
            boolean open = false;
            while (rs.next()) {
                if (!open) {
                    out.write(header.toString());
                    open = true;
                } else {
                    out.write(",\n");
                }
                StringBuilder row = new StringBuilder("(");
                for (int i = 1; i <= cols; i++) {
                    row.append(i > 1 ? "," : "").append(sqlValue(rs, i, md.getColumnType(i)));
                }
                out.write(row.append(')').toString());
                rows++;
                if (rows % 500 == 0) { // keep statements a reasonable size
                    out.write(";\n");
                    open = false;
                }
            }
            if (open) {
                out.write(";\n");
            }
        }
        return rows;
    }

    private static String sqlValue(ResultSet rs, int i, int type) throws SQLException {
        String v = rs.getString(i);
        if (v == null) {
            return "NULL";
        }
        switch (type) {
            case Types.INTEGER: case Types.SMALLINT: case Types.TINYINT: case Types.BIGINT:
            case Types.DECIMAL: case Types.NUMERIC: case Types.DOUBLE: case Types.FLOAT:
                return v;
            case Types.BIT: case Types.BOOLEAN:
                return rs.getBoolean(i) ? "1" : "0";
            default:
                return "'" + v.replace("\\", "\\\\").replace("'", "''").replace("\n", "\\n").replace("\r", "\\r") + "'";
        }
    }

    /** Replaces all data with the content of a backup file, in a single transaction. */
    public int restore(File file) throws EVChargingException, IOException {
        AuthService.requireAdmin();
        if (!file.exists()) {
            throw new EVChargingException("Backup file not found: " + file);
        }
        String who = SecurityContext.username();
        int statements;
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                statements = SqlScriptRunner.run(conn, file.toPath());
                conn.commit();
            } catch (SQLException | IOException e) {
                conn.rollback();
                throw new EVChargingException("Restore failed, database unchanged: " + e.getMessage(), e);
            } finally {
                try (Statement st = conn.createStatement()) {
                    st.execute("SET FOREIGN_KEY_CHECKS=1");
                }
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new EVChargingException("Restore failed: " + e.getMessage(), e);
        }
        audit.log("RESTORE", "DATABASE", null, file.getName() + " by " + who);
        return statements;
    }
}
