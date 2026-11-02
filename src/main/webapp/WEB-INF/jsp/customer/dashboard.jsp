<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "My Dashboard"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<%
    List<Vehicle> vehicles = (List<Vehicle>) request.getAttribute("vehicles");
    List<ChargingSession> active = (List<ChargingSession>) request.getAttribute("activeSessions");
    List<ChargingSession> recent = (List<ChargingSession>) request.getAttribute("recentSessions");
    List<Booking> upcoming = (List<Booking>) request.getAttribute("upcoming");
    List<Payment> unpaid = (List<Payment>) request.getAttribute("unpaid");
    List<QueueEntry> queue = (List<QueueEntry>) request.getAttribute("queue");
    Map<Integer, Integer> chargedMinutes = (Map<Integer, Integer>) request.getAttribute("chargedMinutes");
    boolean canStop = (Boolean) request.getAttribute("customerCanStop");
    double unpaidTotal = 0;
    for (Payment p : unpaid) { unpaidTotal += p.getTotalAmount(); }
%>
<h1>Hello, <%= h(currentUser.getFullName().split(" ")[0]) %></h1>
<p class="sub">Here is your charging overview.</p>

<div class="grid grid-4">
    <div class="card stat"><div class="label">Vehicles</div><div class="value"><%= vehicles.size() %></div><div class="hint"><a href="<%= ctx %>/customer/vehicles">Manage vehicles</a></div></div>
    <div class="card stat blue"><div class="label">Charging sessions</div><div class="value"><%= request.getAttribute("sessionCount") %></div><div class="hint"><%= String.format("%.1f", (Double) request.getAttribute("totalEnergy")) %> kWh in total</div></div>
    <div class="card stat purple"><div class="label">Upcoming bookings</div><div class="value"><%= upcoming.size() %></div><div class="hint"><a href="<%= ctx %>/customer/book">Book a slot</a></div></div>
    <div class="card stat amber"><div class="label">Amount due</div><div class="value"><%= money(unpaidTotal) %></div><div class="hint"><%= unpaid.size() %> unpaid bill(s)</div></div>
</div>

<% if (!queue.isEmpty()) { %>
<div class="card">
    <h2>You are in the waiting queue</h2>
    <table><tr><th>Station</th><th>Vehicle</th><th>Charger</th><th>Joined</th><th class="num">Est. wait</th><th></th></tr>
    <% for (QueueEntry q : queue) { %>
        <tr><td><%= h(q.getStationName()) %></td><td><%= h(q.getVehicleRegNo()) %></td><td><%= q.getChargerType() %></td><td><%= dt(q.getRequestedAt()) %></td>
            <td class="num"><b><%= q.getEstimatedWaitMin() %> min</b></td>
            <td class="right"><form method="post" action="<%= ctx %>/customer/book"><input type="hidden" name="action" value="leaveQueue"><input type="hidden" name="queueId" value="<%= q.getQueueId() %>"><button class="btn danger small">Leave queue</button></form></td></tr>
    <% } %>
    </table>
    <p class="small muted">A charger is allocated to you automatically as soon as one becomes free. This page refreshes every 20 seconds.</p>
    <script>setTimeout(function(){ location.reload(); }, 20000);</script>
</div>
<% } %>

<% if (!active.isEmpty()) { %>
<div class="card">
    <h2>Charging now</h2>
    <table><tr><th>Vehicle</th><th>Station</th><th>Charger</th><th>Started</th><th class="num">Battery at start</th><th class="num">Charged so far</th><th></th></tr>
    <% for (ChargingSession s : active) { %>
        <tr><td><%= h(s.getVehicleRegNo()) %></td><td><%= h(s.getStationName()) %></td><td><%= h(s.getChargerCode()) %> (<%= h(s.getChargerType()) %>)</td>
            <td><%= dt(s.getStartTime()) %></td><td class="num"><%= s.getStartSoc() %>%</td>
            <td class="num"><span class="badge ACTIVE"><%= chargedMinutes.get(s.getSessionId()) %> min</span></td>
            <td class="right"><% if (canStop) { %><form method="post" action="<%= ctx %>/customer/stop" onsubmit="return confirm('Stop charging <%= h(s.getVehicleRegNo()) %> now? Your bill will be generated.')">
                <input type="hidden" name="sessionId" value="<%= s.getSessionId() %>"><button class="btn danger small">Stop charging</button></form><% } %></td></tr>
    <% } %>
    </table>
    <% if (canStop) { %>
    <p class="small muted">Stop charging whenever you are ready - the bill is generated immediately and you can pay online. Simulation: 1 real second = 1 minute of charging. This page refreshes every 20 seconds.</p>
    <% } else { %>
    <p class="small muted">The station operator stops the session when you unplug; your bill then appears under "Bills to pay". This page refreshes every 20 seconds.</p>
    <% } %>
</div>
<% } %>
<% if (!active.isEmpty() && queue.isEmpty()) { %><script>setTimeout(function(){ location.reload(); }, 20000);</script><% } %>

<div class="grid grid-2">
    <div class="card">
        <h2>Bills to pay</h2>
        <% if (unpaid.isEmpty()) { %><p class="empty">All bills are paid. Thank you!</p><% } else { %>
        <table><tr><th>Invoice</th><th>Station</th><th class="num">Amount</th><th></th></tr>
        <% for (Payment p : unpaid) { %>
            <tr><td><%= h(p.getInvoiceNo()) %><br><span class="small muted"><%= dt(p.getSessionEnd()) %></span></td><td><%= h(p.getStationName()) %></td>
                <td class="num"><b><%= money(p.getTotalAmount()) %></b></td><td class="right"><a class="btn small" href="<%= ctx %>/customer/pay?id=<%= p.getPaymentId() %>">Pay now</a></td></tr>
        <% } %>
        </table><% } %>
    </div>
    <div class="card">
        <h2>Upcoming bookings</h2>
        <% if (upcoming.isEmpty()) { %><p class="empty">No upcoming bookings. <a href="<%= ctx %>/customer/book">Book a slot</a></p><% } else { %>
        <table><tr><th>When</th><th>Station</th><th>Charger</th><th>Vehicle</th></tr>
        <% for (Booking b : upcoming) { %>
            <tr><td><%= dt(b.getSlotStart()) %></td><td><%= h(b.getStationName()) %></td><td><%= h(b.getChargerCode()) %></td><td><%= h(b.getVehicleRegNo()) %></td></tr>
        <% } %>
        </table><% } %>
    </div>
</div>

<div class="card">
    <h2>Recent charging</h2>
    <% if (recent.isEmpty()) { %><p class="empty">No charging sessions yet.</p><% } else { %>
    <table><tr><th>Date</th><th>Station</th><th>Vehicle</th><th class="num">Energy</th><th class="num">Duration</th><th class="num">Battery</th><th>Payment</th></tr>
    <% for (ChargingSession s : recent) { %>
        <tr><td><%= dt(s.getStartTime()) %></td><td><%= h(s.getStationName()) %></td><td><%= h(s.getVehicleRegNo()) %></td>
            <td class="num"><%= String.format("%.2f", s.getEnergyKwh()) %> kWh</td><td class="num"><%= s.getDurationMinutes() %> min</td>
            <td class="num"><%= s.getStartSoc() %>% &rarr; <%= s.getEndSoc() %>%</td><td><span class="badge <%= h(s.getPaymentStatus()) %>"><%= h(s.getPaymentStatus()) %></span></td></tr>
    <% } %>
    </table><% } %>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
