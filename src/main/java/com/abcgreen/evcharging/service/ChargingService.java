package com.abcgreen.evcharging.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.abcgreen.evcharging.dao.BookingDao;
import com.abcgreen.evcharging.dao.ChargerDao;
import com.abcgreen.evcharging.dao.ReportDao;
import com.abcgreen.evcharging.dao.SessionDao;
import com.abcgreen.evcharging.dao.Transaction;
import com.abcgreen.evcharging.dao.VehicleDao;
import com.abcgreen.evcharging.exception.ChargerUnavailableException;
import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.RecordNotFoundException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Booking;
import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.ChargerStatus;
import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.ChargingSession;
import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.model.Vehicle;

/**
 * FR-07 Charging session management: start / stop charging, duration, energy and cost calculation.
 *
 * Simulation: real elapsed time is multiplied by "simulation_speed_factor" (default 60, i.e. one real
 * second = one minute of charging) so a demo session delivers realistic energy in a short time.
 */
public class ChargingService {

    private final SessionDao sessionDao = new SessionDao();
    private final ChargerDao chargerDao = new ChargerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final BookingDao bookingDao = new BookingDao();
    private final ReportDao reportDao = new ReportDao();
    private final BillingService billing = new BillingService();
    private final ConfigService config = new ConfigService();
    private final AuditService audit = new AuditService();

    private Vehicle checkVehicle(int customerId, int vehicleId, int startSoc) throws EVChargingException {
        Vehicle v = vehicleDao.findById(vehicleId);
        if (v == null || v.getCustomerId() != customerId) {
            throw new ValidationException("The selected vehicle does not belong to this customer.");
        }
        if (startSoc < 0 || startSoc > 99) {
            throw new ValidationException("Battery level at start must be between 0 and 99 %.");
        }
        if (sessionDao.vehicleHasActiveSession(vehicleId)) {
            throw new ValidationException("Vehicle " + v.getRegistrationNo() + " is already charging.");
        }
        return v;
    }

    /** Starts charging on a specific charger. The charger is switched AVAILABLE -> OCCUPIED atomically. */
    public ChargingSession startSession(int customerId, int vehicleId, int chargerId, int startSoc, Integer bookingId)
            throws EVChargingException {
        Vehicle v = checkVehicle(customerId, vehicleId, startSoc);
        ChargingSession s = Transaction.execute(() -> {
            if (!chargerDao.updateStatus(chargerId, ChargerStatus.AVAILABLE, ChargerStatus.OCCUPIED)) {
                throw new ChargerUnavailableException("The charger is not available any more. Please choose another charger.");
            }
            ChargingSession session = new ChargingSession();
            session.setCustomerId(customerId);
            session.setVehicleId(vehicleId);
            session.setChargerId(chargerId);
            session.setBookingId(bookingId);
            session.setStartSoc(startSoc);
            session.setStartTime(LocalDateTime.now());
            sessionDao.insert(session);
            if (bookingId != null) {
                bookingDao.updateStatus(bookingId, "IN_PROGRESS");
            }
            return session;
        });
        audit.log("START", "SESSION", s.getSessionId(), v.getRegistrationNo() + " on charger #" + chargerId + " at " + startSoc + "%");
        return s;
    }

    /** Starts the session for a booking, on the booked charger or any free charger of the same type. */
    public ChargingSession startFromBooking(int bookingId, int startSoc) throws EVChargingException {
        Booking b = bookingDao.findById(bookingId);
        if (b == null) {
            throw new RecordNotFoundException("Booking not found.");
        }
        if (!"BOOKED".equals(b.getStatus())) {
            throw new ValidationException("Only BOOKED reservations can be started (current: " + b.getStatus() + ").");
        }
        Integer chargerId = b.getChargerId();
        Charger booked = chargerId == null ? null : chargerDao.findById(chargerId);
        if (booked == null || !booked.isAvailable()) {
            ChargerType type = booked == null ? ChargerType.FAST : booked.getType();
            List<Charger> free = chargerDao.findAvailable(b.getStationId(), type);
            if (free.isEmpty()) {
                throw new ChargerUnavailableException("The booked charger is busy and no other " + type + " charger is free at the station.");
            }
            chargerId = free.get(0).getChargerId();
        }
        return startSession(b.getCustomerId(), b.getVehicleId(), chargerId, startSoc, bookingId);
    }

    /** Walk-in request: start immediately if a charger of the type is free, otherwise join the queue (FR-06). */
    public ChargeRequestResult chargeNow(int customerId, int vehicleId, int stationId, ChargerType type, int startSoc)
            throws EVChargingException {
        checkVehicle(customerId, vehicleId, startSoc);
        List<Charger> free = chargerDao.findAvailable(stationId, type);
        for (Charger c : free) {
            try {
                ChargingSession s = startSession(customerId, vehicleId, c.getChargerId(), startSoc, null);
                return ChargeRequestResult.started(s, c.getChargerCode());
            } catch (ChargerUnavailableException e) {
                // taken by someone else in the meantime - try the next free charger
            }
        }
        return ChargeRequestResult.queued(new QueueService().joinQueue(customerId, vehicleId, stationId, type, startSoc));
    }

    /** Minutes of charging represented by the real time elapsed since the start (simulation factor applied). */
    public int simulatedMinutes(LocalDateTime start, LocalDateTime end) {
        double factor = config.getDouble(ConfigService.SIMULATION_SPEED, 60);
        double realMinutes = Duration.between(start, end).getSeconds() / 60.0;
        return (int) Math.max(1, Math.round(realMinutes * factor));
    }

    /**
     * Customer stops their own charging session from the web portal.
     * The session must belong to the customer; the operator is notified.
     */
    public Payment stopSessionForCustomer(int sessionId, int customerId) throws EVChargingException {
        if (!customerStopEnabled()) {
            throw new ValidationException("Stopping charging from the portal is disabled. Please ask the station operator to stop your session.");
        }
        ChargingSession s = sessionDao.findById(sessionId);
        if (s == null || s.getCustomerId() != customerId) {
            throw new RecordNotFoundException("Charging session not found.");
        }
        Payment bill = stopSession(sessionId);
        new NotificationService().notifyOperators("SYSTEM", "Customer " + s.getCustomerName() + " stopped charging " + s.getVehicleRegNo()
                + " on " + s.getChargerCode() + " from the web portal (bill " + bill.getInvoiceNo() + ").");
        return bill;
    }

    /** System setting: may customers stop their own session from the web portal? */
    public boolean customerStopEnabled() {
        return config.getBoolean(ConfigService.CUSTOMER_STOP_ENABLED, true);
    }

    /** Stops charging, calculates duration / energy / cost, creates the bill and frees the charger. */
    public Payment stopSession(int sessionId) throws EVChargingException {
        ChargingSession s = sessionDao.findById(sessionId);
        if (s == null) {
            throw new RecordNotFoundException("Session #" + sessionId + " not found.");
        }
        if (!s.isActive()) {
            throw new ValidationException("Session #" + sessionId + " is already " + s.getStatus() + ".");
        }
        Charger charger = chargerDao.findById(s.getChargerId());
        Vehicle vehicle = vehicleDao.findById(s.getVehicleId());
        LocalDateTime end = LocalDateTime.now();

        int minutes = simulatedMinutes(s.getStartTime(), end);
        int minutesToFull = charger.estimateMinutes(vehicle.getBatteryCapacityKwh(), s.getStartSoc(), 100);
        minutes = Math.max(1, Math.min(minutes, minutesToFull)); // a full battery stops drawing power
        double energy = BillingService.round2(charger.calculateEnergyDelivered(minutes, vehicle.getBatteryCapacityKwh(), s.getStartSoc()));
        int endSoc = (int) Math.min(100, Math.round(s.getStartSoc() + energy * 100 / vehicle.getBatteryCapacityKwh()));

        s.setEndTime(end);
        s.setDurationMinutes(minutes);
        s.setEnergyKwh(energy);
        s.setEndSoc(endSoc);
        s.setCost(BillingService.round2(charger.calculateEnergyCharge(energy)));
        s.setStatus("COMPLETED");

        Payment bill = Transaction.execute(() -> {
            sessionDao.complete(s);
            chargerDao.setStatus(charger.getChargerId(), ChargerStatus.AVAILABLE);
            if (s.getBookingId() != null) {
                bookingDao.updateStatus(s.getBookingId(), "COMPLETED");
            }
            reportDao.upsertDemand(charger.getStationId(), s.getStartTime().toLocalDate(), s.getStartTime().getHour(), 1, energy);
            return billing.createBill(sessionId, charger, energy);
        });
        audit.log("STOP", "SESSION", sessionId, String.format("%d min, %.2f kWh, %d%% -> %d%%, bill %s Rs %.2f",
                minutes, energy, s.getStartSoc(), endSoc, bill.getInvoiceNo(), bill.getTotalAmount()));
        QueueProcessor.wakeUp(); // the charger is free again - serve the waiting queue
        return billing.findById(bill.getPaymentId());
    }

    public List<ChargingSession> activeSessions() {
        return sessionDao.findActive();
    }

    public List<ChargingSession> search(String keyword, String status) {
        return sessionDao.search(keyword, status, 1000);
    }

    public List<ChargingSession> historyOf(int customerId) {
        return sessionDao.findByCustomer(customerId);
    }

    public ChargingSession findById(int sessionId) {
        return sessionDao.findById(sessionId);
    }
}
