<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "My Vehicles"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<% List<Vehicle> vehicles = (List<Vehicle>) request.getAttribute("vehicles"); %>
<h1>My Vehicles</h1>
<p class="sub">Register your electric vehicles. Each registration number can be registered only once.</p>
<div class="grid grid-2">
    <div class="card">
        <h2>Registered vehicles</h2>
        <% if (vehicles.isEmpty()) { %><p class="empty">No vehicles yet - add your first EV.</p><% } else { %>
        <table><tr><th>Registration</th><th>Vehicle</th><th class="num">Battery</th><th>Connector</th><th></th></tr>
        <% for (Vehicle v : vehicles) { %>
            <tr><td><b><%= h(v.getRegistrationNo()) %></b></td><td><%= h(v.getMake()) %> <%= h(v.getModel()) %><br><span class="small muted"><%= h(v.getVehicleType()) %></span></td>
                <td class="num"><%= v.getBatteryCapacityKwh() %> kWh</td><td><%= h(v.getConnectorType()) %></td>
                <td class="right"><form method="post" onsubmit="return confirm('Remove this vehicle?')"><input type="hidden" name="action" value="delete">
                    <input type="hidden" name="vehicleId" value="<%= v.getVehicleId() %>"><button class="btn danger small">Remove</button></form></td></tr>
        <% } %>
        </table><% } %>
    </div>
    <div class="card">
        <h2>Add a vehicle</h2>
        <form method="post" action="<%= ctx %>/customer/vehicles">
            <label>Registration number *</label><input name="registrationNo" placeholder="KA01AB1234" required>
            <div class="row">
                <div><label>Make *</label><input name="make" placeholder="Tata" required></div>
                <div><label>Model *</label><input name="model" placeholder="Nexon EV" required></div>
            </div>
            <div class="row">
                <div><label>Type</label><select name="type"><% for (String t : Vehicle.TYPES) { %><option><%= t %></option><% } %></select></div>
                <div><label>Battery (kWh) *</label><input name="battery" type="number" step="0.1" min="1" max="500" value="40.5" required></div>
                <div><label>Connector</label><select name="connector"><% for (String c : Vehicle.CONNECTORS) { %><option><%= c %></option><% } %></select></div>
            </div>
            <p><button class="btn" type="submit">Add vehicle</button></p>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
