package com.abcgreen.evcharging.util;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Application-wide file logger (logs/application.log) built on java.util.logging.
 * Database-level activity is additionally recorded in the audit_log table.
 */
public final class AppLogger {

    private static final Logger LOGGER = Logger.getLogger("EVChargingSystem");

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT [%4$s] %5$s%6$s%n");
        try {
            new File("logs").mkdirs();
            FileHandler handler = new FileHandler("logs/application.log", 2_000_000, 3, true);
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
        } catch (IOException e) {
            System.err.println("File logging disabled: " + e.getMessage());
        }
    }

    private AppLogger() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warning(message);
    }

    public static void error(String message, Throwable t) {
        LOGGER.log(Level.SEVERE, message, t);
    }
}
