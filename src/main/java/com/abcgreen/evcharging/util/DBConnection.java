package com.abcgreen.evcharging.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.abcgreen.evcharging.exception.DataAccessException;

/**
 * Creates JDBC connections to MySQL.
 *
 * Settings are read from db.properties on the classpath, then optionally
 * overridden by db-local.properties in the working folder, and finally by
 * JVM system properties (-Ddb.password=...).
 */
public final class DBConnection {

    private static final Properties CONFIG = loadConfig();

    private DBConnection() {
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            AppLogger.error("Could not read db.properties", e);
        }
        Path local = Paths.get("db-local.properties");
        if (Files.exists(local)) {
            try (InputStream in = new FileInputStream(local.toFile())) {
                props.load(in);
            } catch (IOException e) {
                AppLogger.error("Could not read db-local.properties", e);
            }
        }
        for (String key : new String[] { "db.host", "db.port", "db.name", "db.user", "db.password" }) {
            String value = System.getProperty(key);
            if (value != null) {
                props.setProperty(key, value);
            }
        }
        return props;
    }

    private static String serverUrl() {
        return "jdbc:mysql://" + CONFIG.getProperty("db.host", "localhost") + ":"
                + CONFIG.getProperty("db.port", "3306") + "/";
    }

    private static String options() {
        return "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8"
                + "&rewriteBatchedStatements=true&allowMultiQueries=false";
    }

    /** Connection to the application database (ev_charging_db). */
    public static Connection getConnection() {
        return connect(serverUrl() + CONFIG.getProperty("db.name", "ev_charging_db") + options());
    }

    /** Connection to the MySQL server without selecting a database (used by the setup tool). */
    public static Connection getServerConnection() {
        return connect(serverUrl() + options());
    }

    private static Connection connect(String url) {
        try {
            return DriverManager.getConnection(url, CONFIG.getProperty("db.user"), CONFIG.getProperty("db.password"));
        } catch (SQLException e) {
            throw new DataAccessException("Cannot connect to MySQL at " + url
                    + " - check that MySQL is running and the user/password in db.properties. (" + e.getMessage() + ")", e);
        }
    }

    public static String getDatabaseName() {
        return CONFIG.getProperty("db.name", "ev_charging_db");
    }

    /** Quick health check used by the login screens. */
    public static boolean isAvailable() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }
}
