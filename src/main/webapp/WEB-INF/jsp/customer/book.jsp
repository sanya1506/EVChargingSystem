<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Book a charger"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<%
    List<ChargingStation> stations = (List<ChargingStation>) request.getAttribute("stations");
    List<Vehicle> vehicles = (List<Vehicle>) request.getAttribute("vehicles");
    List<Booking> bookings = (List<Booking>) request.getAttribute("bookings");
    int selected = (Integer) request.getAttribute("selectedStation");
%>
<h1>Book or charge now</h1>
<p class="sub">Reserve a slot in advance, or charge immediately - if every charger is busy you join the live queue automatically.</p>
<% if (vehicles.isEmpty()) { %>
    <div class="alert warning">Please <a href="<%= ctx %>/customer/vehicles">register a vehicle</a> first.</div>
<% } else { %>
<div class="grid grid-2">
    <div class="card">
        <h2>Reserve a slot</h2>
        <form method="post" action="<%= ctx %>/customer/book">
            <input type="hidden" name="action" value="book">
            <label>Vehicle</label>
            <select name="vehicleId"><% for (Vehicle v : vehicles) { %><option value="<%= v.getVehicleId() %>"><%= h(v) %></option><% } %></select>
            <label>Station</label>
            <select name="stationId">
                <% for (ChargingStation s : stations) { if (!"ACTIVE".equals(s.getStatus())) continue; %>
                <option value="<%= s.getStationId() %>" <%= s.getStationId() == selected ? "selected" : "" %>><%= h(s.getCity()) %> - <%= h(s.getName()) %> (<%= s.getAvailableChargers() %> free)</option>
                <% } %>
            </select>
            <div class="row">
                <div><label>Charger</label><select name="chargerType"><option value="FAST">Fast (DC)</option><option value="SLOW">Slow (AC)</option></select></div>
                <div><label>Duration</label><select name="duration"><option value="30">30 min</option><option value="60" selected>1 hour</option><option value="90">1.5 hours</option><option value="120">2 hours</option><option value="240">4 hours</option></select></div>
            </div>
            <div class="row">
                <div><label>Date</label><input type="date" name="date" value="<%= request.getAttribute("defaultDate") %>" required></div>
                <div><label>Start time</label><input type="time" name="time" value="10:00" required></div>
            </div>
            <p><button class="btn" type="submit">Confirm booking</button></p>
        </form>
    </div>
    <div class="card">
        <h2>Charge now</h2>
        <form method="post" action="<%= ctx %>/customer/book">
            <input type="hidden" name="action" value="now">
            <label>Vehicle</label>
            <select name="vehicleId"><% for (Vehicle v : vehicles) { %><option value="<%= v.getVehicleId() %>"><%= h(v) %></option><% } %></select>
            <label>Station</label>
            <select name="stationId">
                <% for (ChargingStation s : stations) { if (!"ACTIVE".equals(s.getStatus())) continue; %>
                <option value="<%= s.getStationId() %>" <%= s.getStationId() == selected ? "selected" : "" %>><%= h(s.getCity()) %> - <%= h(s.getName()) %> (<%= s.getAvailableChargers() %> free)</option>
                <% } %>
            </select>
            <div class="row">
                <div><label>Charger</label><select name="chargerType"><option value="FAST">Fast (DC)</option><option value="SLOW">Slow (AC)</option></select></div>
                <div><label>Battery now (%)</label><input type="number" name="soc" min="0" max="99" value="20"></div>
            </div>
            <p><button class="btn" type="submit">Start charging / join queue</button></p>
            <p class="small muted">You will see your estimated waiting time if all chargers of the chosen type are busy.</p>
        </form>
    </div>
</div>
<% } %>
<div class="card">
    <h2>My bookings</h2>
    <% if (bookings.isEmpty()) { %><p class="empty">No bookings yet.</p><% } else { %>
    <div class="table-wrap"><table><tr><th>#</th><th>Slot</th><th>Station</th><th>Charger</th><th>Vehicle</th><th>Status</th><th></th></tr>
    <% for (Booking b : bookings) { %>
        <tr><td><%= b.getBookingId() %></td><td><%= dt(b.getSlotStart()) %></td><td><%= h(b.getStationName()) %></td><td><%= h(b.getChargerCode()) %></td>
            <td><%= h(b.getVehicleRegNo()) %></td><td><span class="badge <%= h(b.getStatus()) %>"><%= h(b.getStatus()) %></span></td>
            <td class="right"><% if ("BOOKED".equals(b.getStatus())) { %><form method="post" onsubmit="return confirm('Cancel this booking?')"><input type="hidden" name="action" value="cancel">
                <input type="hidden" name="bookingId" value="<%= b.getBookingId() %>"><button class="btn danger small">Cancel</button></form><% } %></td></tr>
    <% } %>
    </table></div><% } %>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
