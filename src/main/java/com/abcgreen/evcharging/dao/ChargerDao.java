package com.abcgreen.evcharging.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.ChargerStatus;
import com.abcgreen.evcharging.model.ChargerType;

public class ChargerDao extends BaseDao {

    private static final String SELECT = "SELECT ch.*, s.name AS station_name FROM chargers ch JOIN stations s ON s.station_id = ch.station_id ";

    /** Uses the Charger factory, so each row becomes a FastCharger or SlowCharger object. */
    private static Charger map(ResultSet rs) throws SQLException {
        Charger c = Charger.create(ChargerType.valueOf(rs.getString("charger_type")));
        c.setChargerId(rs.getInt("charger_id"));
        c.setStationId(rs.getInt("station_id"));
        c.setStationName(rs.getString("station_name"));
        c.setChargerCode(rs.getString("charger_code"));
        c.setConnectorType(rs.getString("connector_type"));
        c.setPowerKw(rs.getDouble("power_kw"));
        c.setPricePerKwh(rs.getDouble("price_per_kwh"));
        c.setStatus(ChargerStatus.valueOf(rs.getString("status")));
        c.setInstalledOn(date(rs, "installed_on"));
        return c;
    }

    public List<Charger> findByStation(int stationId) {
        return query(SELECT + "WHERE ch.station_id = ? ORDER BY ch.charger_code", ChargerDao::map, stationId);
    }

    public List<Charger> findAll() {
        return query(SELECT + "ORDER BY ch.charger_code", ChargerDao::map);
    }

    public Charger findById(int id) {
        return queryOne(SELECT + "WHERE ch.charger_id = ?", ChargerDao::map, id);
    }

    /** Free chargers of a type at a station, locked for update when called inside a transaction. */
    public List<Charger> findAvailable(int stationId, ChargerType type) {
        return query(SELECT + "WHERE ch.station_id = ? AND ch.charger_type = ? AND ch.status = 'AVAILABLE' "
                + "AND s.status = 'ACTIVE' ORDER BY ch.power_kw DESC, ch.charger_id FOR UPDATE", ChargerDao::map, stationId, type);
    }

    public int countByType(int stationId, ChargerType type) {
        return queryInt("SELECT COUNT(*) FROM chargers WHERE station_id = ? AND charger_type = ? AND status IN ('AVAILABLE','OCCUPIED')",
                stationId, type);
    }

    public boolean codeExists(String code, int excludeId) {
        return queryInt("SELECT COUNT(*) FROM chargers WHERE charger_code = ? AND charger_id <> ?", code, excludeId) > 0;
    }

    public int insert(Charger c) {
        int id = insert("INSERT INTO chargers (station_id, charger_code, charger_type, connector_type, power_kw, price_per_kwh, status, installed_on) VALUES (?,?,?,?,?,?,?,?)",
                c.getStationId(), c.getChargerCode(), c.getType(), c.getConnectorType(), c.getPowerKw(), c.getPricePerKwh(), c.getStatus(), c.getInstalledOn());
        c.setChargerId(id);
        return id;
    }

    public void update(Charger c) {
        update("UPDATE chargers SET station_id=?, charger_code=?, charger_type=?, connector_type=?, power_kw=?, price_per_kwh=?, status=? WHERE charger_id=?",
                c.getStationId(), c.getChargerCode(), c.getType(), c.getConnectorType(), c.getPowerKw(), c.getPricePerKwh(), c.getStatus(), c.getChargerId());
    }

    /** @return true if the row changed (used for optimistic allocation). */
    public boolean updateStatus(int chargerId, ChargerStatus from, ChargerStatus to) {
        return update("UPDATE chargers SET status=? WHERE charger_id=? AND status=?", to, chargerId, from) == 1;
    }

    public void setStatus(int chargerId, ChargerStatus status) {
        update("UPDATE chargers SET status=? WHERE charger_id=?", status, chargerId);
    }

    public void delete(int id) {
        update("DELETE FROM chargers WHERE charger_id=?", id);
    }

    /** Status -> count, collected into a HashMap (Module IV). */
    public Map<ChargerStatus, Integer> countByStatus() {
        Map<ChargerStatus, Integer> map = new HashMap<>();
        for (ChargerStatus s : ChargerStatus.values()) {
            map.put(s, 0);
        }
        query("SELECT status, COUNT(*) FROM chargers GROUP BY status", rs -> {
            map.put(ChargerStatus.valueOf(rs.getString(1)), rs.getInt(2));
            return null;
        });
        return map;
    }
}
