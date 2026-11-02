<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Register"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<div class="auth wide">
    <div class="card">
        <h1>Create your account</h1>
        <p class="sub">Register once, then add your EV and start booking chargers.</p>
        <% if (request.getAttribute("error") != null) { %><div class="alert error"><%= h(request.getAttribute("error")) %></div><% } %>
        <form method="post" action="<%= ctx %>/register">
            <div class="row">
                <div><label>Full name *</label><input name="fullName" value="<%= h(request.getParameter("fullName")) %>" required></div>
                <div><label>Username *</label><input name="username" value="<%= h(request.getParameter("username")) %>" required></div>
            </div>
            <div class="row">
                <div><label>Email *</label><input type="email" name="email" value="<%= h(request.getParameter("email")) %>" required></div>
                <div><label>Mobile (10 digits) *</label><input name="phone" value="<%= h(request.getParameter("phone")) %>" required pattern="[6-9][0-9]{9}"></div>
            </div>
            <div class="row">
                <div><label>City *</label>
                    <select name="city">
                        <% for (String c : new String[]{"Bengaluru","Mumbai","Delhi","Hyderabad","Chennai","Pune","Kolkata","Ahmedabad"}) { %>
                        <option <%= c.equals(request.getParameter("city")) ? "selected" : "" %>><%= c %></option><% } %>
                    </select>
                </div>
                <div><label>Address</label><input name="address" value="<%= h(request.getParameter("address")) %>"></div>
            </div>
            <div class="row">
                <div><label>Password *</label><input type="password" name="password" required minlength="8"></div>
                <div><label>Confirm password *</label><input type="password" name="confirm" required minlength="8"></div>
            </div>
            <p class="small muted">Password: at least 8 characters with upper case, lower case, a digit and a special character.</p>
            <button class="btn" type="submit">Create account</button>
        </form>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
