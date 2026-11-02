package com.abcgreen.evcharging.service;

import com.abcgreen.evcharging.model.ChargingSession;
import com.abcgreen.evcharging.model.QueueEntry;

/** Result of a "charge now" request: either a started session or a place in the waiting queue. */
public class ChargeRequestResult {

    private final ChargingSession session;
    private final QueueEntry queueEntry;
    private final String message;

    private ChargeRequestResult(ChargingSession session, QueueEntry queueEntry, String message) {
        this.session = session;
        this.queueEntry = queueEntry;
        this.message = message;
    }

    public static ChargeRequestResult started(ChargingSession s, String chargerCode) {
        return new ChargeRequestResult(s, null, "Charging started on charger " + chargerCode + " (session #" + s.getSessionId() + ").");
    }

    public static ChargeRequestResult queued(QueueEntry q) {
        return new ChargeRequestResult(null, q, "All chargers are busy. Added to the waiting queue at position " + q.getPosition()
                + ". Estimated waiting time: " + q.getEstimatedWaitMin() + " minutes.");
    }

    public boolean isQueued() {
        return queueEntry != null;
    }

    public ChargingSession getSession() { return session; }
    public QueueEntry getQueueEntry() { return queueEntry; }
    public String getMessage() { return message; }
}
