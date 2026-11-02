package com.abcgreen.evcharging;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.abcgreen.evcharging.exception.AuthenticationException;
import com.abcgreen.evcharging.exception.ChargerUnavailableException;
import com.abcgreen.evcharging.exception.DuplicateRecordException;
import com.abcgreen.evcharging.exception.PaymentException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Charger;
import com.abcgreen.evcharging.model.ChargerStatus;
import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.ChargingSession;
import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.model.QueueEntry;
import com.abcgreen.evcharging.model.ReportData;
import com.abcgreen.evcharging.model.User;
import com.abcgreen.evcharging.service.AuthService;
import com.abcgreen.evcharging.service.BillingService;
import com.abcgreen.evcharging.service.BookingService;
import com.abcgreen.evcharging.service.ChargeRequestResult;
import com.abcgreen.evcharging.service.ChargingService;
import com.abcgreen.evcharging.service.CustomerService;
import com.abcgreen.evcharging.service.QueueService;
import com.abcgreen.evcharging.service.ReportService;
import com.abcgreen.evcharging.service.SecurityContext;
import com.abcgreen.evcharging.service.StationService;
import com.abcgreen.evcharging.util.DBConnection;

/**
 * Integration tests against the MySQL demo database (skipped automatically when MySQL is not reachable).
 * They create a few extra records - run tools.DatabaseSetup afterwards for a clean demo database.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTest {

    private static final AuthService auth = new AuthService();
    private static final ChargingService charging = new ChargingService();
    private static final QueueService queue = new QueueService();
    private static final StationService stations = new StationService();
    private static final CustomerService customers = new CustomerService();

    private static final com.abcgreen.evcharging.dao.SessionDao sessionDao = new com.abcgreen.evcharging.dao.SessionDao();
    private static final com.abcgreen.evcharging.dao.QueueDao queueDao = new com.abcgreen.evcharging.dao.QueueDao();
    private static int nextCustomer = 20 + new java.util.Random().nextInt(400);

    /** Returns {customerId, vehicleId} of a vehicle that is neither charging nor waiting (keeps tests repeatable). */
    private static int[] freeVehicle() {
        while (true) {
            int customerId = nextCustomer++;
            for (com.abcgreen.evcharging.model.Vehicle v : customers.vehiclesOf(customerId)) {
                if (!sessionDao.vehicleHasActiveSession(v.getVehicleId()) && !queueDao.vehicleWaiting(v.getVehicleId())) {
                    return new int[] { customerId, v.getVehicleId() };
                }
            }
        }
    }

    @BeforeAll
    static void requireDatabase() throws Exception {
        assumeTrue(DBConnection.isAvailable(), "MySQL not available - integration tests skipped");
        User admin = auth.login("admin", "Admin@123");
        SecurityContext.setDesktopUser(admin);
    }

    @Test
    @Order(1)
    @DisplayName("TC-INT-01 Login succeeds with valid and fails with invalid credentials")
    void login() throws Exception {
        assertEquals("ADMIN", auth.login("admin", "Admin@123").getRole().name());
        assertThrows(AuthenticationException.class, () -> auth.login("admin", "wrong"));
        auth.login("admin", "Admin@123"); // resets the failed-attempt counter
    }

    @Test
    @Order(2)
    @DisplayName("TC-INT-02 Duplicate customer email / phone is rejected")
    void duplicateCustomer() {
        Customer existing = customers.search("").get(0);
        Customer dup = new Customer("Duplicate Person", existing.getEmail(), "9000000001", "Pune", "");
        assertThrows(DuplicateRecordException.class, () -> customers.save(dup));
    }

    @Test
    @Order(3)
    @DisplayName("TC-INT-03 Start and stop a session: energy, duration, bill, charger freed")
    void startStop() throws Exception {
        Charger free = stations.allChargers().stream().filter(c -> c.getStatus() == ChargerStatus.AVAILABLE && c.getType() == ChargerType.FAST)
                .findFirst().orElseThrow();
        int[] cv = freeVehicle();
        int customerId = cv[0];
        int vehicleId = cv[1];
        ChargingSession s = charging.startSession(customerId, vehicleId, free.getChargerId(), 30, null);
        assertTrue(s.getSessionId() > 0);
        assertThrows(ValidationException.class, () -> charging.startSession(customerId, vehicleId, free.getChargerId(), 30, null),
                "vehicle already charging");
        Thread.sleep(1500);
        Payment bill = charging.stopSession(s.getSessionId());
        assertTrue(bill.getEnergyKwh() > 0);
        assertTrue(bill.getTotalAmount() > bill.getEnergyCharge());
        assertEquals("PENDING", bill.getStatus());
        assertTrue(bill.getInvoiceNo().startsWith("INV-"));
        assertEquals(ChargerStatus.AVAILABLE, stations.chargersOf(free.getStationId()).stream()
                .filter(c -> c.getChargerId() == free.getChargerId()).findFirst().orElseThrow().getStatus());

        BillingService billing = new BillingService();
        assertThrows(PaymentException.class, () -> billing.pay(bill.getPaymentId(), "UPI", "fail@upi"));
        Payment paid = billing.pay(bill.getPaymentId(), "UPI", "aarav@okaxis");
        assertEquals("PAID", paid.getStatus());
        assertThrows(PaymentException.class, () -> billing.pay(bill.getPaymentId(), "UPI", "aarav@okaxis"));
    }

    @Test
    @Order(4)
    @DisplayName("TC-INT-04 Busy station: customer is queued, then auto-allocated when a charger frees up")
    void queueAllocation() throws Exception {
        int stationId = 3; // demo station with 2 fast chargers
        // occupy every free fast charger at the station
        for (Charger c : stations.chargersOf(stationId)) {
            if (c.getType() == ChargerType.FAST && c.getStatus() == ChargerStatus.AVAILABLE) {
                int[] cv = freeVehicle();
                charging.startSession(cv[0], cv[1], c.getChargerId(), 30, null);
            }
        }
        // clear older waiting entries so our customer is first
        for (QueueEntry q : queue.waiting()) {
            if (q.getStationId() == stationId) {
                queue.cancel(q.getQueueId());
            }
        }
        int[] waiting = freeVehicle();
        int vehicleId = waiting[1];
        ChargeRequestResult r = charging.chargeNow(waiting[0], vehicleId, stationId, ChargerType.FAST, 25);
        assertTrue(r.isQueued(), "all fast chargers busy -> queued");
        assertEquals(1, r.getQueueEntry().getPosition());
        assertTrue(r.getQueueEntry().getEstimatedWaitMin() >= 0);

        // free one charger -> the queue processor must allocate it
        List<ChargingSession> active = charging.activeSessions();
        ChargingSession running = active.stream().filter(s -> s.getStationName() != null && "FAST".equals(s.getChargerType())
                && stations.chargersOf(stationId).stream().anyMatch(c -> c.getChargerId() == s.getChargerId())).findFirst().orElseThrow();
        charging.stopSession(running.getSessionId());
        assertEquals(1, queue.processQueue());
        assertTrue(charging.activeSessions().stream().anyMatch(s -> s.getVehicleId() == vehicleId), "queued vehicle is now charging");
    }

    @Test
    @Order(5)
    @DisplayName("TC-INT-05 Booking prevents double booking of the same slot")
    void booking() throws Exception {
        BookingService bookings = new BookingService();
        java.util.Random rnd = new java.util.Random();
        LocalDateTime slot = LocalDateTime.now().plusDays(5 + rnd.nextInt(20)).withHour(rnd.nextInt(5)).withMinute(15 * rnd.nextInt(4))
                .withSecond(0).withNano(0);
        int stationId = 3;
        int booked = 0;
        // station 3 has 2 fast chargers -> the 3rd booking for the same slot must fail
        for (int i = 0; i < 3; i++) {
            int[] cv = freeVehicle();
            try {
                bookings.book(cv[0], cv[1], stationId, ChargerType.FAST, slot, 60);
                booked++;
            } catch (ChargerUnavailableException e) {
                assertTrue(e.getMessage().contains("booked"));
            }
        }
        assertEquals(2, booked);
        int[] cv = freeVehicle();
        assertThrows(ValidationException.class, () -> bookings.book(cv[0], cv[1], stationId,
                ChargerType.FAST, LocalDateTime.now().minusDays(1), 60));
    }

    @Test
    @Order(6)
    @DisplayName("TC-INT-06 Every report is generated in under 10 seconds (NFR)")
    void reports() {
        ReportService rs = new ReportService();
        for (String type : ReportService.REPORT_TYPES) {
            ReportData r = rs.generate(type, java.time.LocalDate.now(), java.time.YearMonth.now());
            assertNotNull(r.getColumns());
            assertTrue(r.getGenerationMillis() < 10_000, type + " took " + r.getGenerationMillis() + " ms");
        }
    }

    @Test
    @Order(7)
    @DisplayName("TC-INT-07 Operators cannot perform admin-only actions")
    void roleCheck() throws Exception {
        User operator = auth.login("operator", "Operator@123");
        SecurityContext.setDesktopUser(operator);
        try {
            assertThrows(AuthenticationException.class, () -> new com.abcgreen.evcharging.service.BackupService()
                    .backup(new java.io.File("target/should-not-exist.sql")));
        } finally {
            SecurityContext.setDesktopUser(auth.login("admin", "Admin@123"));
        }
    }

    @Test
    @Order(8)
    @DisplayName("TC-INT-08 Customer stops own session (only when the setting is enabled); other customers cannot")
    void customerStop() throws Exception {
        Charger free = stations.allChargers().stream().filter(c -> c.getStatus() == ChargerStatus.AVAILABLE).findFirst().orElseThrow();
        int[] owner = freeVehicle();
        int[] other = freeVehicle();
        ChargingSession s = charging.startSession(owner[0], owner[1], free.getChargerId(), 40, null);
        Thread.sleep(1200);
        assertThrows(com.abcgreen.evcharging.exception.RecordNotFoundException.class, () -> charging.stopSessionForCustomer(s.getSessionId(), other[0]));
        com.abcgreen.evcharging.service.ConfigService config = new com.abcgreen.evcharging.service.ConfigService();
        config.set(com.abcgreen.evcharging.service.ConfigService.CUSTOMER_STOP_ENABLED, "false");
        try {
            assertThrows(ValidationException.class, () -> charging.stopSessionForCustomer(s.getSessionId(), owner[0]), "setting disabled");
            assertThrows(ValidationException.class, () -> config.set(com.abcgreen.evcharging.service.ConfigService.CUSTOMER_STOP_ENABLED, "maybe"));
        } finally {
            config.set(com.abcgreen.evcharging.service.ConfigService.CUSTOMER_STOP_ENABLED, "true");
        }
        Payment bill = charging.stopSessionForCustomer(s.getSessionId(), owner[0]);
        assertEquals("PENDING", bill.getStatus());
        assertTrue(bill.getEnergyKwh() > 0);
        assertThrows(ValidationException.class, () -> charging.stopSessionForCustomer(s.getSessionId(), owner[0]), "already stopped");
    }
}
