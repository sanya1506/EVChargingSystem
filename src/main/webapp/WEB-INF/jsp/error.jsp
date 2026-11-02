<%@ page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<% String pageTitle = "Error"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<%
    Integer code = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    Object msg = request.getAttribute("jakarta.servlet.error.message");
    Throwable ex = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
    if (ex != null) { com.abcgreen.evcharging.util.AppLogger.error("Web error", ex); }
%>
<div class="auth">
    <div class="card">
        <h1><%= code == null ? "Error" : code == 404 ? "Page not found" : code == 403 ? "Access denied" : "Something went wrong" %></h1>
        <p class="sub"><%= ex != null ? h(ex.getMessage()) : h(msg) %></p>
        <a class="btn" href="<%= ctx %>/">Go to home page</a>
    </div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
