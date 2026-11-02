package com.abcgreen.evcharging.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.abcgreen.evcharging.exception.DataAccessException;
import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.util.DBConnection;

/**
 * Runs several DAO calls in one JDBC transaction (commit on success, rollback on any error).
 * The connection is kept in a ThreadLocal so every DAO used inside the block shares it.
 */
public final class Transaction {

    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();

    @FunctionalInterface
    public interface Work<T> {
        T execute() throws EVChargingException;
    }

    private Transaction() {
    }

    static Connection current() {
        return CURRENT.get();
    }

    public static <T> T execute(Work<T> work) throws EVChargingException {
        if (CURRENT.get() != null) {
            return work.execute(); // already inside a transaction
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            CURRENT.set(conn);
            try {
                T result = work.execute();
                conn.commit();
                return result;
            } catch (EVChargingException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                CURRENT.remove();
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaction failed: " + e.getMessage(), e);
        }
    }
}
