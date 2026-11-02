package com.abcgreen.evcharging.service;

import java.util.Map;

import com.abcgreen.evcharging.dao.ConfigDao;
import com.abcgreen.evcharging.exception.ValidationException;

/** System configuration stored in the system_config table (FR: Configure System). */
public class ConfigService {

    public static final String GST_RATE = "gst_rate_percent";
    public static final String SIMULATION_SPEED = "simulation_speed_factor";
    public static final String MAX_FAILED_LOGINS = "max_failed_logins";
    public static final String QUEUE_POLL_SECONDS = "queue_poll_seconds";
    public static final String DEFAULT_SLOT_MINUTES = "default_slot_minutes";
    public static final String COMPANY_NAME = "company_name";
    public static final String COMPANY_GSTIN = "company_gstin";
    /** true = customers may stop their own session from the web portal; false = only operators stop sessions. */
    public static final String CUSTOMER_STOP_ENABLED = "customer_stop_charging_enabled";

    private final ConfigDao dao = new ConfigDao();
    private final AuditService audit = new AuditService();

    public Map<String, String[]> all() {
        return dao.findAll();
    }

    public String get(String key, String def) {
        try {
            return dao.get(key, def);
        } catch (RuntimeException e) {
            return def;
        }
    }

    public double getDouble(String key, double def) {
        try {
            return Double.parseDouble(get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        return Boolean.parseBoolean(get(key, String.valueOf(def)).trim());
    }

    public int getInt(String key, int def) {
        return (int) getDouble(key, def);
    }

    public void set(String key, String value) throws ValidationException {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Value for " + key + " cannot be empty.");
        }
        if (key.endsWith("_enabled")) {
            String v = value.trim().toLowerCase();
            if (!v.equals("true") && !v.equals("false")) {
                throw new ValidationException(key + " must be true or false.");
            }
            value = v;
        } else if (!key.startsWith("company")) {
            try {
                if (Double.parseDouble(value) < 0) {
                    throw new ValidationException(key + " cannot be negative.");
                }
            } catch (NumberFormatException e) {
                throw new ValidationException(key + " must be a number.");
            }
        }
        dao.set(key, value.trim());
        audit.log("UPDATE", "CONFIG", key, "Set to " + value);
    }
}
