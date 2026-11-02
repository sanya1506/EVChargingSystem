package com.abcgreen.evcharging.dao;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigDao extends BaseDao {

    public Map<String, String[]> findAll() {
        Map<String, String[]> map = new LinkedHashMap<>();
        query("SELECT * FROM system_config ORDER BY config_key", rs -> {
            map.put(rs.getString("config_key"), new String[] { rs.getString("config_value"), rs.getString("description") });
            return null;
        });
        return map;
    }

    public String get(String key, String defaultValue) {
        String v = queryOne("SELECT config_value FROM system_config WHERE config_key = ?", rs -> rs.getString(1), key);
        return v == null ? defaultValue : v;
    }

    public void set(String key, String value) {
        update("UPDATE system_config SET config_value = ? WHERE config_key = ?", value, key);
    }
}
