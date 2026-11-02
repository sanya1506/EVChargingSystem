<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Admin Dashboard"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<%
    DashboardStats d = (DashboardStats) request.getAttribute("d");
    List<Notification> notes = (List<Notification>) request.getAttribute("notifications");
    List<ChargingStation> stations = (List<ChargingStation>) request.getAttribute("stations");
    List<QueueEntry> queue = (List<QueueEntry>) request.getAttribute("queue");
%>
<h1>Network Dashboard</h1>
<p class="sub">Live status of all stations &middot; refreshes every 30 seconds</p>
<div class="grid grid-4">
    <div class="card stat"><div class="label">Available chargers</div><div class="value"><%= d.getAvailableChargers() %> / <%= d.getTotalChargers() %></div><div class="hint"><%= d.getFaultyChargers() %> faulty &middot; <%= d.getMaintenanceChargers() %> maintenance</div></div>
    <div class="card stat blue"><div class="label">Charging now</div><div class="value"><%= d.getActiveSessions() %></div><div class="hint"><%= d.getTodaySessions() %> sessions today</div></div>
    <div class="card stat amber"><div class="label">Waiting in queue</div><div class="value"><%= d.getQueueLength() %></div><div class="hint"><%= d.getUnreadNotifications() %> unread notifications</div></div>
    <div class="card stat purple"><div class="label">Revenue today</div><div class="value"><%= money(d.getTodayRevenue()) %></div><div class="hint">Month: <%= money(d.getMonthRevenue()) %></div></div>
</div>
<div class="grid grid-2">
    <div class="card"><h2>Peak hour demand (last 30 days)</h2><div class="chart-box"><canvas id="hourly"></canvas></div></div>
    <div class="card"><h2>Energy delivered per day (kWh)</h2><div class="chart-box"><canvas id="daily"></canvas></div></div>
</div>
<div class="grid grid-2">
    <div class="card"><h2>Revenue by city (30 days)</h2><div class="chart-box"><canvas id="city"></canvas></div></div>
    <div class="card">
        <h2>Notifications</h2>
        <table>
        <% for (Notification n : notes) { %>
            <tr><td style="white-space:nowrap" class="small muted"><%= dt(n.getCreatedAt()) %></td><td><span class="badge"><%= h(n.getType()) %></span></td><td class="small"><%= n.isRead() ? "" : "<b>" %><%= h(n.getMessage()) %><%= n.isRead() ? "" : "</b>" %></td></tr>
        <% } %>
        </table>
    </div>
</div>
<% if (!queue.isEmpty()) { %>
<div class="card">
    <h2>Waiting queue</h2>
    <table><tr><th>Pos.</th><th>Customer</th><th>Vehicle</th><th>Station</th><th>Type</th><th>Since</th><th class="num">Est. wait</th></tr>
    <% for (QueueEntry q : queue) { %>
        <tr><td><%= q.getPosition() %></td><td><%= h(q.getCustomerName()) %></td><td><%= h(q.getVehicleRegNo()) %></td><td><%= h(q.getStationName()) %></td><td><%= q.getChargerType() %></td><td><%= dt(q.getRequestedAt()) %></td><td class="num"><%= q.getEstimatedWaitMin() %> min</td></tr>
    <% } %>
    </table>
</div>
<% } %>
<div class="card">
    <h2>Station utilisation</h2>
    <div class="table-wrap"><table><tr><th>Code</th><th>Station</th><th>City</th><th>Status</th><th class="num">Chargers</th><th class="num">Free</th><th class="num">In use</th><th class="num">Faulty</th><th class="num">Maint.</th><th class="num">Capacity</th><th class="num">Utilisation</th></tr>
    <% for (ChargingStation s : stations) { %>
        <tr><td><%= h(s.getStationCode()) %></td><td><%= h(s.getName()) %></td><td><%= h(s.getCity()) %></td><td><span class="badge <%= h(s.getStatus()) %>"><%= h(s.getStatus()) %></span></td>
            <td class="num"><%= s.getTotalChargers() %></td><td class="num"><%= s.getAvailableChargers() %></td><td class="num"><%= s.getOccupiedChargers() %></td>
            <td class="num"><%= s.getFaultyChargers() %></td><td class="num"><%= s.getMaintenanceChargers() %></td><td class="num"><%= String.format("%.0f kW", s.getCapacityKw()) %></td>
            <td class="num"><%= Math.round(s.getUtilisationPercent()) %>%</td></tr>
    <% } %>
    </table></div>
</div>
<script src="<%= ctx %>/assets/js/chart.umd.min.js"></script>
<script>
const green = '#059669', amber = '#d97706', blue = '#2563eb';
const hourly = <%= request.getAttribute("hourlyJson") %>;
const maxH = Math.max(...hourly);
new Chart(document.getElementById('hourly'), {
    type: 'bar',
    data: { labels: [...Array(24).keys()].map(h => h + ':00'), datasets: [{ label: 'Sessions', data: hourly,
        backgroundColor: hourly.map(v => v >= 0.75 * maxH ? amber : green), borderRadius: 4 }] },
    options: { maintainAspectRatio: false, plugins: { legend: { display: false } } }
});
new Chart(document.getElementById('daily'), {
    type: 'line',
    data: { labels: <%= request.getAttribute("dailyLabelsJson") %>, datasets: [{ label: 'kWh', data: <%= request.getAttribute("dailyValuesJson") %>,
        borderColor: blue, backgroundColor: 'rgba(37,99,235,.12)', fill: true, tension: .3, pointRadius: 0 }] },
    options: { maintainAspectRatio: false, plugins: { legend: { display: false } } }
});
new Chart(document.getElementById('city'), {
    type: 'doughnut',
    data: { labels: <%= request.getAttribute("cityLabelsJson") %>, datasets: [{ data: <%= request.getAttribute("cityValuesJson") %>,
        backgroundColor: ['#059669', '#2563eb', '#d97706', '#7c3aed', '#dc2626', '#0891b2', '#65a30d', '#db2777'] }] },
    options: { maintainAspectRatio: false, plugins: { legend: { position: 'right' } } }
});
setTimeout(() => location.reload(), 30000);
</script>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
