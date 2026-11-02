package com.abcgreen.evcharging.web.servlets;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Booking;
import com.abcgreen.evcharging.model.ChargerType;
import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.service.BookingService;
import com.abcgreen.evcharging.service.ChargeRequestResult;
import com.abcgreen.evcharging.service.ChargingService;
import com.abcgreen.evcharging.service.CustomerService;
import com.abcgreen.evcharging.service.QueueService;
import com.abcgreen.evcharging.service.StationService;

/** FR-05 Slot booking, "charge now" with automatic queueing (FR-06) and booking cancellation. */
public class BookingServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient StationService stations = new StationService();
    private final transient CustomerService customers = new CustomerService();
    private final transient BookingService bookings = new BookingService();
    private final transient ChargingService charging = new ChargingService();
    private final transient QueueService queue = new QueueService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer c = customer(req);
        req.setAttribute("stations", stations.findAll());
        req.setAttribute("vehicles", customers.vehiclesOf(c.getCustomerId()));
        req.setAttribute("bookings", bookings.ofCustomer(c.getCustomerId()));
        req.setAttribute("selectedStation", intParam(req, "station", 0));
        req.setAttribute("defaultDate", LocalDate.now().plusDays(1).toString());
        render(req, resp, "customer/book");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Customer c = customer(req);
        String action = req.getParameter("action");
        try {
            if ("cancel".equals(action)) {
                bookings.cancel(intParam(req, "bookingId", 0), c.getCustomerId());
                flash(req, "success", "Booking cancelled.");
            } else if ("leaveQueue".equals(action)) {
                queue.cancel(intParam(req, "queueId", 0));
                flash(req, "success", "You left the queue.");
                redirect(req, resp, "/customer/dashboard");
                return;
            } else {
                int vehicleId = intParam(req, "vehicleId", 0);
                int stationId = intParam(req, "stationId", 0);
                ChargerType type = ChargerType.valueOf(req.getParameter("chargerType"));
                if ("now".equals(action)) {
                    ChargeRequestResult r = charging.chargeNow(c.getCustomerId(), vehicleId, stationId, type, intParam(req, "soc", 20));
                    flash(req, r.isQueued() ? "warning" : "success", r.getMessage());
                    redirect(req, resp, "/customer/dashboard");
                    return;
                }
                LocalDateTime start;
                try {
                    start = LocalDate.parse(req.getParameter("date")).atTime(LocalTime.parse(req.getParameter("time")));
                } catch (DateTimeParseException | NullPointerException e) {
                    throw new ValidationException("Please choose a valid date and time.");
                }
                Booking b = bookings.book(c.getCustomerId(), vehicleId, stationId, type, start, intParam(req, "duration", 60));
                flash(req, "success", "Booking #" + b.getBookingId() + " confirmed at " + b.getStationName() + " on charger " + b.getChargerCode() + ".");
            }
        } catch (EVChargingException | IllegalArgumentException e) {
            flash(req, "error", e.getMessage());
        }
        redirect(req, resp, "/customer/book");
    }
}
