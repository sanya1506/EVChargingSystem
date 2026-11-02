<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Login"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<div class="auth">
    <div class="card">
        <h1>Sign in</h1>
        <p class="sub">Customers, operators and administrators</p>
        <% if (request.getAttribute("error") != null) { %><div class="alert error"><%= h(request.getAttribute("error")) %></div><% } %>
        <form method="post" action="<%= ctx %>/login">
            <label>Username or email</label>
            <input name="username" value="<%= h(request.getAttribute("username")) %>" required autofocus>
            <label>Password</label>
            <input type="password" name="password" required>
            <p><button class="btn block" type="submit">Sign in</button></p>
        </form>
        <p class="small muted">New here? <a href="<%= ctx %>/register">Create a customer account</a></p>
        <p class="small muted">Demo: <b>aarav.sharma</b> / Customer@123 &middot; <b>admin</b> / Admin@123</p>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
