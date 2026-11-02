package com.abcgreen.evcharging.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.abcgreen.evcharging.exception.DataAccessException;
import com.abcgreen.evcharging.util.DBConnection;

/**
 * Common JDBC plumbing shared by all DAOs: PreparedStatement parameter binding,
 * query/update helpers and conversion of SQL date types.
 * All SQL uses PreparedStatement parameters, which prevents SQL injection.
 */
public abstract class BaseDao {

    @FunctionalInterface
    private interface ConnectionWork<T> {
        T run(Connection conn) throws SQLException;
    }

    private <T> T withConnection(String sql, ConnectionWork<T> work) {
        Connection tx = Transaction.current();
        try {
            if (tx != null) {
                return work.run(tx);
            }
            try (Connection conn = DBConnection.getConnection()) {
                return work.run(conn);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Database error: " + e.getMessage() + " [SQL: " + sql + "]", e);
        }
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        return withConnection(sql, conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(mapper.map(rs));
                    }
                    return list;
                }
            }
        });
    }

    protected <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    protected int queryInt(String sql, Object... params) {
        Integer v = queryOne(sql, rs -> rs.getInt(1), params);
        return v == null ? 0 : v;
    }

    protected double queryDouble(String sql, Object... params) {
        Double v = queryOne(sql, rs -> rs.getDouble(1), params);
        return v == null ? 0 : v;
    }

    /** INSERT / UPDATE / DELETE. @return affected rows */
    protected int update(String sql, Object... params) {
        return withConnection(sql, conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bind(ps, params);
                return ps.executeUpdate();
            }
        });
    }

    /** INSERT returning the generated AUTO_INCREMENT key. */
    protected int insert(String sql, Object... params) {
        return withConnection(sql, conn -> {
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bind(ps, params);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            if (p instanceof LocalDateTime) {
                ps.setTimestamp(i + 1, Timestamp.valueOf((LocalDateTime) p));
            } else if (p instanceof LocalDate) {
                ps.setDate(i + 1, Date.valueOf((LocalDate) p));
            } else if (p instanceof Enum<?>) {
                ps.setString(i + 1, ((Enum<?>) p).name());
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    protected static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    protected static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }

    protected static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    protected static String like(String keyword) {
        return "%" + (keyword == null ? "" : keyword.trim()) + "%";
    }
}
