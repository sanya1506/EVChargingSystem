package com.abcgreen.evcharging.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.abcgreen.evcharging.dao.BookingDao;
import com.abcgreen.evcharging.dao.ChargerDao;
import com.abcgreen.evcharging.dao.StationDao;
import com.abcgreen.evcharging.dao.VehicleDao;
import com.abcgreen.evcharging.exception.ChargerUnavailableException;
import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.RecordNotFoundException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Booking;
import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.ChargerStatus;
import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.ChargingStation;
import com.abcgreen.evcharging.model.Vehicle;

/** FR-05 Charging slot booking with automatic charger allocation and double-booking prevention. */
public class BookingService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM HH:mm");

    private final BookingDao bookingDao = new BookingDao();
    private final ChargerDao chargerDao = new ChargerDao();
    private final StationDao stationDao = new StationDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final AuditService audit = new AuditService();
    private final NotificationService notifications = new NotificationService();

    public Booking book(int customerId, int vehicleId, int stationId, ChargerType type, LocalDateTime start, int durationMinutes)
            throws EVChargingException {
        if (start == null) {
            throw new ValidationException("Please choose the slot date and time.");
        }
        if (start.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new ValidationException("The slot cannot be in the past.");
        }
        if (start.isAfter(LocalDateTime.now().plusDays(30))) {
            throw new ValidationException("Slots can be booked at most 30 days in advance.");
        }
        if (durationMinutes < 15 || durationMinutes > 480) {
            throw new ValidationException("Slot duration must be between 15 and 480 minutes.");
        }
        Vehicle v = vehicleDao.findById(vehicleId);
        if (v == null || v.getCustomerId() != customerId) {
            throw new ValidationException("Please select one of the customer's vehicles.");
        }
        ChargingStation st = stationDao.findById(stationId);
        if (st == null) {
            throw new RecordNotFoundException("Station not found.");
        }
        if (!"ACTIVE".equals(st.getStatus())) {
            throw new ChargerUnavailableException("Station " + st.getName() + " is currently " + st.getStatus() + ".");
        }
        LocalDateTime end = start.plusMinutes(durationMinutes);

        // candidate chargers: right type, not faulty / under maintenance, most powerful first (lambda + stream)
        List<Charger> candidates = chargerDao.findByStation(stationId).stream()
                .filter(c -> c.getType() == type)
                .filter(c -> c.getStatus() != ChargerStatus.FAULTY && c.getStatus() != ChargerStatus.MAINTENANCE)
                .sorted(Comparator.comparingDouble(Charger::getPowerKw).reversed())
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            throw new ChargerUnavailableException("No working " + type + " chargers at " + st.getName() + ".");
        }
        Charger chosen = null;
        for (Charger c : candidates) {
            if (!bookingDao.hasOverlap(c.getChargerId(), start, end)) {
                chosen = c;
                break;
            }
        }
        if (chosen == null) {
            throw new ChargerUnavailableException("All " + type + " chargers at " + st.getName() + " are booked between "
                    + start.format(FMT) + " and " + end.toLocalTime() + ". Try another time or join the waiting queue on arrival.");
        }
        Booking b = new Booking();
        b.setCustomerId(customerId);
        b.setVehicleId(vehicleId);
        b.setStationId(stationId);
        b.setChargerId(chosen.getChargerId());
        b.setSlotStart(start);
        b.setSlotEnd(end);
        bookingDao.insert(b);
        audit.log("BOOK", "BOOKING", b.getBookingId(), v.getRegistrationNo() + " " + chosen.getChargerCode() + " " + start.format(FMT));
        notifications.notifyOperators("BOOKING", "New booking #" + b.getBookingId() + ": " + v.getRegistrationNo() + " at "
                + st.getName() + " on " + start.format(FMT) + " (charger " + chosen.getChargerCode() + ").");
        b.setChargerCode(chosen.getChargerCode());
        b.setStationName(st.getName());
        b.setVehicleRegNo(v.getRegistrationNo());
        return b;
    }

    /** Cancels a booking. When customerId is given it must own the booking (web portal). */
    public void cancel(int bookingId, Integer customerId) throws EVChargingException {
        Booking b = bookingDao.findById(bookingId);
        if (b == null || (customerId != null && b.getCustomerId() != customerId)) {
            throw new RecordNotFoundException("Booking not found.");
        }
        if (!"BOOKED".equals(b.getStatus())) {
            throw new ValidationException("Only BOOKED reservations can be cancelled.");
        }
        bookingDao.updateStatus(bookingId, "CANCELLED");
        audit.log("CANCEL", "BOOKING", bookingId, null);
    }

    public void markNoShow(int bookingId) throws EVChargingException {
        Booking b = bookingDao.findById(bookingId);
        if (b == null || !"BOOKED".equals(b.getStatus())) {
            throw new ValidationException("Only BOOKED reservations can be marked as no-show.");
        }
        bookingDao.updateStatus(bookingId, "NO_SHOW");
        audit.log("NO_SHOW", "BOOKING", bookingId, null);
    }

    public List<Booking> upcoming(String status) {
        return bookingDao.findUpcoming(status);
    }

    public List<Booking> ofCustomer(int customerId) {
        return bookingDao.findByCustomer(customerId);
    }
}
