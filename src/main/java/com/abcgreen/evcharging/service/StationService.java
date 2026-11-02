package com.abcgreen.evcharging.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.abcgreen.evcharging.dao.ChargerDao;
import com.abcgreen.evcharging.dao.MaintenanceDao;
import com.abcgreen.evcharging.dao.StationDao;
import com.abcgreen.evcharging.dao.Transaction;
import com.abcgreen.evcharging.exception.DuplicateRecordException;
import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.RecordNotFoundException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.ChargerStatus;
import com.abcgreen.evcharging.model.ChargingStation;
import com.abcgreen.evcharging.model.MaintenanceRecord;
import com.abcgreen.evcharging.util.Validator;

/** FR-04 Charging station management, charger configuration, FR-09 infrastructure monitoring and maintenance. */
public class StationService {

    private final StationDao stationDao = new StationDao();
    private final ChargerDao chargerDao = new ChargerDao();
    private final MaintenanceDao maintenanceDao = new MaintenanceDao();
    private final AuditService audit = new AuditService();
    private final NotificationService notifications = new NotificationService();

    public List<ChargingStation> findAll() {
        return stationDao.findAll();
    }

    public List<ChargingStation> search(String keyword, String city) {
        return stationDao.search(keyword, city);
    }

    public List<String> cities() {
        return stationDao.findCities();
    }

    public ChargingStation findById(int id) throws RecordNotFoundException {
        ChargingStation s = stationDao.findById(id);
        if (s == null) {
            throw new RecordNotFoundException("Station #" + id + " not found.");
        }
        s.getChargers().addAll(chargerDao.findByStation(id));
        return s;
    }

    public void save(ChargingStation s) throws EVChargingException {
        s.setStationCode(Validator.required(s.getStationCode(), "Station code").toUpperCase());
        s.setName(Validator.required(s.getName(), "Station name"));
        s.setCity(Validator.required(s.getCity(), "City"));
        s.setAddress(Validator.required(s.getAddress(), "Address"));
        if (s.getLatitude() < -90 || s.getLatitude() > 90 || s.getLongitude() < -180 || s.getLongitude() > 180) {
            throw new ValidationException("Latitude must be between -90 and 90 and longitude between -180 and 180.");
        }
        if (stationDao.codeExists(s.getStationCode(), s.getStationId())) {
            throw new DuplicateRecordException("Station code " + s.getStationCode() + " already exists.");
        }
        if (s.getStationId() == 0) {
            stationDao.insert(s);
            audit.log("CREATE", "STATION", s.getStationId(), s.getName());
        } else {
            stationDao.update(s);
            audit.log("UPDATE", "STATION", s.getStationId(), s.getName() + " status=" + s.getStatus());
        }
    }

    public void delete(int stationId) {
        stationDao.delete(stationId);
        audit.log("DELETE", "STATION", stationId, null);
    }

    public List<Charger> chargersOf(int stationId) {
        return chargerDao.findByStation(stationId);
    }

    public List<Charger> allChargers() {
        return chargerDao.findAll();
    }

    public void saveCharger(Charger c) throws EVChargingException {
        if (c.getStationId() <= 0) {
            throw new ValidationException("Please select a station.");
        }
        c.setChargerCode(Validator.required(c.getChargerCode(), "Charger code").toUpperCase());
        if (c.getPowerKw() <= 0 || c.getPowerKw() > 400) {
            throw new ValidationException("Power must be between 0 and 400 kW.");
        }
        if (c.getPricePerKwh() <= 0) {
            throw new ValidationException("Price per kWh must be greater than zero.");
        }
        if (chargerDao.codeExists(c.getChargerCode(), c.getChargerId())) {
            throw new DuplicateRecordException("Charger code " + c.getChargerCode() + " already exists.");
        }
        if (c.getChargerId() == 0) {
            chargerDao.insert(c);
            audit.log("CREATE", "CHARGER", c.getChargerId(), c.getChargerCode() + " " + c.getType());
        } else {
            chargerDao.update(c);
            audit.log("UPDATE", "CHARGER", c.getChargerId(), c.getChargerCode());
        }
    }

    public void deleteCharger(int chargerId) {
        chargerDao.delete(chargerId);
        audit.log("DELETE", "CHARGER", chargerId, null);
    }

    /** Manual status change by an operator (e.g. report a fault). Occupied chargers are controlled by sessions. */
    public void changeChargerStatus(int chargerId, ChargerStatus status) throws EVChargingException {
        Charger c = chargerDao.findById(chargerId);
        if (c == null) {
            throw new RecordNotFoundException("Charger not found.");
        }
        if (c.getStatus() == ChargerStatus.OCCUPIED || status == ChargerStatus.OCCUPIED) {
            throw new ValidationException("Occupied status is controlled by charging sessions. Stop the session first.");
        }
        chargerDao.setStatus(chargerId, status);
        audit.log("STATUS", "CHARGER", chargerId, c.getChargerCode() + ": " + c.getStatus() + " -> " + status);
        if (status == ChargerStatus.FAULTY) {
            notifications.notifyOperators("FAULT", "Charger " + c.getChargerCode() + " at " + c.getStationName() + " reported FAULTY.");
        } else if (status == ChargerStatus.AVAILABLE) {
            QueueProcessor.wakeUp(); // a charger became free - waiting customers may be allocated
        }
    }

    public Map<ChargerStatus, Integer> chargerStatusCounts() {
        return chargerDao.countByStatus();
    }

    public List<MaintenanceRecord> maintenance(String status) {
        return maintenanceDao.search(status);
    }

    public void scheduleMaintenance(MaintenanceRecord m) throws EVChargingException {
        if (m.getChargerId() <= 0) {
            throw new ValidationException("Please select a charger.");
        }
        if (m.getScheduledDate() == null || m.getScheduledDate().isBefore(LocalDate.now())) {
            throw new ValidationException("Scheduled date must be today or later.");
        }
        Validator.required(m.getTechnician(), "Technician");
        int id = maintenanceDao.insert(m);
        audit.log("SCHEDULE", "MAINTENANCE", id, m.getMaintenanceType() + " on " + m.getScheduledDate());
        notifications.notifyOperators("MAINTENANCE", m.getMaintenanceType() + " maintenance scheduled on " + m.getScheduledDate()
                + " for charger #" + m.getChargerId() + " (" + m.getTechnician() + ")");
    }

    /** Starting maintenance takes the charger offline; completing it brings it back. */
    public void updateMaintenanceStatus(int maintenanceId, String status) throws EVChargingException {
        MaintenanceRecord m = maintenanceDao.findById(maintenanceId);
        if (m == null) {
            throw new RecordNotFoundException("Maintenance record not found.");
        }
        Charger c = chargerDao.findById(m.getChargerId());
        if ("IN_PROGRESS".equals(status) && c.getStatus() == ChargerStatus.OCCUPIED) {
            throw new ValidationException("Charger " + c.getChargerCode() + " is in use. Stop the session before maintenance.");
        }
        Transaction.execute(() -> {
            maintenanceDao.updateStatus(maintenanceId, status);
            if ("IN_PROGRESS".equals(status)) {
                chargerDao.setStatus(m.getChargerId(), ChargerStatus.MAINTENANCE);
            } else if ("COMPLETED".equals(status)) {
                chargerDao.setStatus(m.getChargerId(), ChargerStatus.AVAILABLE);
            }
            return null;
        });
        audit.log("STATUS", "MAINTENANCE", maintenanceId, status);
        if ("COMPLETED".equals(status)) {
            QueueProcessor.wakeUp();
        }
    }
}
