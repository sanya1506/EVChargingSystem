<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "History & Bills"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<%
    List<ChargingSession> sessions = (List<ChargingSession>) request.getAttribute("sessions");
    List<Payment> payments = (List<Payment>) request.getAttribute("payments");
%>
<h1>History &amp; Bills</h1>
<p class="sub">Your invoices and complete charging history.</p>
<div class="card">
    <h2>Invoices</h2>
    <% if (payments.isEmpty()) { %><p class="empty">No invoices yet.</p><% } else { %>
    <div class="table-wrap"><table><tr><th>Invoice</th><th>Date</th><th>Station</th><th class="num">kWh</th><th class="num">Total</th><th>Status</th><th>Method</th><th></th></tr>
    <% for (Payment p : payments) { %>
        <tr><td><%= h(p.getInvoiceNo()) %></td><td><%= dt(p.getSessionEnd()) %></td><td><%= h(p.getStationName()) %></td>
            <td class="num"><%= String.format("%.2f", p.getEnergyKwh()) %></td><td class="num"><b><%= money(p.getTotalAmount()) %></b></td>
            <td><span class="badge <%= h(p.getStatus()) %>"><%= h(p.getStatus()) %></span></td><td><%= h(p.getMethod()) %></td>
            <td class="right" style="white-space:nowrap">
                <% if (!p.isPaid()) { %><a class="btn small" href="<%= ctx %>/customer/pay?id=<%= p.getPaymentId() %>">Pay</a><% } %>
                <a class="btn secondary small" target="_blank" href="<%= ctx %>/customer/invoice?id=<%= p.getPaymentId() %>">View</a>
                <a class="btn secondary small" href="<%= ctx %>/customer/invoice?id=<%= p.getPaymentId() %>&download=1">Download</a>
            </td></tr>
    <% } %>
    </table></div><% } %>
</div>
<div class="card">
    <h2>Charging sessions</h2>
    <% if (sessions.isEmpty()) { %><p class="empty">No charging sessions yet.</p><% } else { %>
    <div class="table-wrap"><table><tr><th>Start</th><th>End</th><th>Station</th><th>Charger</th><th>Vehicle</th><th class="num">Energy</th><th class="num">Duration</th><th class="num">Battery</th><th>Status</th></tr>
    <% for (ChargingSession s : sessions) { %>
        <tr><td><%= dt(s.getStartTime()) %></td><td><%= dt(s.getEndTime()) %></td><td><%= h(s.getStationName()) %></td><td><%= h(s.getChargerCode()) %></td><td><%= h(s.getVehicleRegNo()) %></td>
            <td class="num"><%= String.format("%.2f", s.getEnergyKwh()) %> kWh</td><td class="num"><%= s.getDurationMinutes() %> min</td>
            <td class="num"><%= s.getStartSoc() %>%<%= s.getEndSoc() == null ? "" : " &rarr; " + s.getEndSoc() + "%" %></td><td><span class="badge <%= h(s.getStatus()) %>"><%= h(s.getStatus()) %></span></td></tr>
    <% } %>
    </table></div><% } %>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
