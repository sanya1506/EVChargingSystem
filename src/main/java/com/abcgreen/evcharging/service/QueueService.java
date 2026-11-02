package com.abcgreen.evcharging.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import com.abcgreen.evcharging.dao.ChargerDao;
import com.abcgreen.evcharging.dao.QueueDao;
import com.abcgreen.evcharging.dao.SessionDao;
import com.abcgreen.evcharging.dao.StationDao;
import com.abcgreen.evcharging.dao.Transaction;
import com.abcgreen.evcharging.exception.ChargerUnavailableException;
import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.ChargingStation;
import com.abcgreen.evcharging.model.QueueEntry;
import com.abcgreen.evcharging.util.AppLogger;

/**
 * FR-06 Charging queue management.
 * Customers wait in FIFO order per station and charger type. When a charger becomes free the
 * {@link QueueProcessor} thread calls {@link #processQueue()} which allocates it automatically.
 */
public class QueueService {

    /** Shared lock so two threads never allocate the same queue in parallel. */
    private static final Object ALLOCATION_LOCK = new Object();
    /** Battery level assumed for vehicles waiting in the queue when the session is auto-started. */
    private static final int DEFAULT_QUEUE_SOC = 20;

    private final QueueDao queueDao = new QueueDao();
    private final ChargerDao chargerDao = new ChargerDao();
    private final SessionDao sessionDao = new SessionDao();
    private final StationDao stationDao = new StationDao();
    private final NotificationService notifications = new NotificationService();
    private final AuditService audit = new AuditService();

    public QueueEntry joinQueue(int customerId, int vehicleId, int stationId, ChargerType type, int startSoc) throws EVChargingException {
        if (queueDao.vehicleWaiting(vehicleId)) {
            throw new ValidationException("This vehicle is already waiting in a queue.");
        }
        if (chargerDao.countByType(stationId, type) == 0) {
            throw new ChargerUnavailableException("This station has no working " + type + " chargers. Please choose another station.");
        }
        QueueEntry q = new QueueEntry();
        q.setStationId(stationId);
        q.setCustomerId(customerId);
        q.setVehicleId(vehicleId);
        q.setChargerType(type);
        q.setRequestedAt(LocalDateTime.now());
        int position = queueDao.countWaitingAhead(stationId, type, q.getRequestedAt().plusSeconds(1)) + 1;
        q.setPosition(position);
        q.setEstimatedWaitMin(estimateWait(stationId, type, position));
        queueDao.insert(q);

        ChargingStation st = stationDao.findById(stationId);
        audit.log("JOIN", "QUEUE", q.getQueueId(), "Station " + stationId + " " + type + " position " + position);
        notifications.notifyOperators("QUEUE", "Customer #" + customerId + " joined the " + type + " queue at "
                + (st == null ? "station " + stationId : st.getName()) + " - position " + position + ", est. wait " + q.getEstimatedWaitMin() + " min.");
        return q;
    }

    /**
     * Estimated waiting time using a PriorityQueue of "minutes until each charger is free".
     * The customer at position p gets the p-th charger that frees up; each served customer occupies
     * the charger for an average session length again.
     */
    public int estimateWait(int stationId, ChargerType type, int position) {
        int working = chargerDao.countByType(stationId, type);
        if (working == 0) {
            return -1;
        }
        double avg = sessionDao.averageDuration(stationId, type.name());
        PriorityQueue<Double> freeAt = new PriorityQueue<>();
        for (int elapsed : sessionDao.activeElapsedMinutes(stationId, type.name())) {
            freeAt.add(Math.max(5, avg - elapsed));
        }
        while (freeAt.size() < working) {
            freeAt.add(0.0); // available charger
        }
        double wait = 0;
        for (int i = 1; i <= position; i++) {
            wait = freeAt.poll();
            freeAt.add(wait + avg);
        }
        return (int) Math.ceil(wait);
    }

    /** Waiting entries with their position inside each station / charger-type queue. */
    public List<QueueEntry> waiting() {
        List<QueueEntry> list = queueDao.findWaiting();
        Map<String, Integer> positions = new HashMap<>();
        for (QueueEntry q : list) {
            String key = q.getStationId() + "-" + q.getChargerType();
            q.setPosition(positions.merge(key, 1, Integer::sum));
        }
        return list;
    }

    public List<QueueEntry> recent(int limit) {
        return queueDao.findRecent(limit);
    }

    public List<QueueEntry> ofCustomer(int customerId) {
        return queueDao.findByCustomer(customerId);
    }

    public int waitingCount() {
        return queueDao.countWaiting();
    }

    public void cancel(int queueId) throws EVChargingException {
        if (!queueDao.cancel(queueId)) {
            throw new ValidationException("Queue entry is no longer waiting.");
        }
        audit.log("CANCEL", "QUEUE", queueId, null);
    }

    /**
     * Allocates free chargers to waiting customers (FIFO). Called by the QueueProcessor thread.
     * @return number of customers allocated
     */
    public int processQueue() {
        synchronized (ALLOCATION_LOCK) {
            int allocated = 0;
            List<QueueEntry> stillWaiting = new ArrayList<>();
            ChargingService charging = new ChargingService();
            for (QueueEntry q : waiting()) {
                try {
                    String chargerCode = Transaction.execute(() -> {
                        List<Charger> free = chargerDao.findAvailable(q.getStationId(), q.getChargerType());
                        if (free.isEmpty()) {
                            return null;
                        }
                        Charger c = free.get(0);
                        if (!queueDao.allocate(q.getQueueId(), c.getChargerId())) {
                            return null;
                        }
                        charging.startSession(q.getCustomerId(), q.getVehicleId(), c.getChargerId(), DEFAULT_QUEUE_SOC, null);
                        return c.getChargerCode();
                    });
                    if (chargerCode != null) {
                        allocated++;
                        notifications.notifyOperators("ALLOCATION", "Queue: " + q.getCustomerName() + " (" + q.getVehicleRegNo()
                                + ") allocated to charger " + chargerCode + " at " + q.getStationName() + ".");
                    } else {
                        stillWaiting.add(q);
                    }
                } catch (EVChargingException | RuntimeException e) {
                    AppLogger.error("Queue allocation failed for entry " + q.getQueueId() + ": " + e.getMessage(), null);
                    stillWaiting.add(q);
                }
            }
            // refresh waiting-time estimates for the customers that are still waiting
            Map<String, Integer> positions = new HashMap<>();
            for (QueueEntry q : stillWaiting) {
                int pos = positions.merge(q.getStationId() + "-" + q.getChargerType(), 1, Integer::sum);
                queueDao.updateEstimate(q.getQueueId(), Math.max(0, estimateWait(q.getStationId(), q.getChargerType(), pos)));
            }
            return allocated;
        }
    }
}
