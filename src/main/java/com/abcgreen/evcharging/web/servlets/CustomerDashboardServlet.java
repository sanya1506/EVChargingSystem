package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.model.Booking;
import com.abcgreen.evcharging.model.ChargingSession;
import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.service.BillingService;
import com.abcgreen.evcharging.service.BookingService;
import com.abcgreen.evcharging.service.ChargingService;
import com.abcgreen.evcharging.service.CustomerService;
import com.abcgreen.evcharging.service.QueueService;

/** Customer home: vehicles, active charging, queue position, upcoming bookings, unpaid bills. */
public class CustomerDashboardServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient CustomerService customers = new CustomerService();
    private final transient ChargingService charging = new ChargingService();
    private final transient BookingService bookings = new BookingService();
    private final transient BillingService billing = new BillingService();
    private final transient QueueService queue = new QueueService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer c = customer(req);
        int id = c.getCustomerId();
        List<ChargingSession> sessions = charging.historyOf(id);
        req.setAttribute("vehicles", customers.vehiclesOf(id));
        List<ChargingSession> active = sessions.stream().filter(ChargingSession::isActive).collect(Collectors.toList());
        req.setAttribute("activeSessions", active);
        // charged time so far (simulation factor applied), keyed by session id
        java.util.Map<Integer, Integer> charged = new java.util.HashMap<>();
        for (ChargingSession s : active) {
            charged.put(s.getSessionId(), charging.simulatedMinutes(s.getStartTime(), java.time.LocalDateTime.now()));
        }
        req.setAttribute("chargedMinutes", charged);
        req.setAttribute("customerCanStop", charging.customerStopEnabled());
        req.setAttribute("recentSessions", sessions.stream().filter(s -> !s.isActive()).limit(5).collect(Collectors.toList()));
        List<Booking> upcoming = bookings.ofCustomer(id).stream().filter(b -> "BOOKED".equals(b.getStatus())).collect(Collectors.toList());
        req.setAttribute("upcoming", upcoming);
        List<Payment> unpaid = billing.paymentsOf(id).stream().filter(p -> !p.isPaid()).collect(Collectors.toList());
        req.setAttribute("unpaid", unpaid);
        req.setAttribute("queue", queue.ofCustomer(id).stream().filter(q -> "WAITING".equals(q.getStatus())).collect(Collectors.toList()));
        req.setAttribute("totalEnergy", sessions.stream().mapToDouble(ChargingSession::getEnergyKwh).sum());
        req.setAttribute("sessionCount", sessions.size());
        render(req, resp, "customer/dashboard");
    }
}
