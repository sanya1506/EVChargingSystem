<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Find a charging station"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<%
    List<ChargingStation> stations = (List<ChargingStation>) request.getAttribute("stations");
    List<String> cities = (List<String>) request.getAttribute("cities");
    String q = request.getParameter("q") == null ? "" : request.getParameter("q");
    String city = request.getParameter("city") == null ? "" : request.getParameter("city");
    int totalAvail = 0;
    for (ChargingStation s : stations) { totalAvail += s.getAvailableChargers(); }
%>
<% if (currentUser == null) { %>
<div class="hero">
    <h1>Charge smarter. Wait less.</h1>
    <p>Find a free fast or slow charger across <%= cities.size() %> cities, book a slot in advance or join the live queue - and pay online in seconds.</p>
    <a class="btn light" href="<%= ctx %>/register">Create free account</a>
    <a class="btn secondary" href="<%= ctx %>/login">Sign in</a>
</div>
<% } %>
<div class="card">
    <form class="inline-form" method="get" action="<%= ctx %>/stations">
        <div style="flex:2"><label>Search</label><input name="q" value="<%= h(q) %>" placeholder="Station name or area"></div>
        <div style="flex:1"><label>City</label>
            <select name="city">
                <option value="">All cities</option>
                <% for (String c : cities) { %><option <%= c.equals(city) ? "selected" : "" %>><%= h(c) %></option><% } %>
            </select>
        </div>
        <button class="btn" type="submit">Search</button>
    </form>
    <p class="muted small" style="margin:10px 0 0"><%= stations.size() %> stations &middot; <%= totalAvail %> chargers available right now</p>
</div>
<div class="grid grid-3">
<% for (ChargingStation s : stations) {
       int working = s.getAvailableChargers() + s.getOccupiedChargers();
       int pct = working == 0 ? 0 : s.getAvailableChargers() * 100 / working; %>
    <div class="card station-card">
        <div style="display:flex;justify-content:space-between;align-items:start;gap:8px">
            <h3><%= h(s.getName()) %></h3><span class="badge <%= h(s.getStatus()) %>"><%= h(s.getStatus()) %></span>
        </div>
        <div class="meta"><%= h(s.getStationCode()) %> &middot; <%= h(s.getAddress()) %></div>
        <div class="meta">Open <%= h(s.getOperatingHours()) %></div>
        <div class="avail"><span style="width:<%= pct %>%"></span></div>
        <div class="nums">
            <span><b><%= s.getAvailableChargers() %></b> free</span>
            <span><b><%= s.getOccupiedChargers() %></b> in use</span>
            <span><%= s.getFastChargers() %> fast &middot; <%= s.getSlowChargers() %> slow</span>
        </div>
        <% if (currentUser != null && currentUser.getRole() == Role.CUSTOMER && "ACTIVE".equals(s.getStatus())) { %>
            <p style="margin:12px 0 0"><a class="btn small" href="<%= ctx %>/customer/book?station=<%= s.getStationId() %>">Book / Charge here</a></p>
        <% } %>
    </div>
<% } %>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
