<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Reports"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<%
    ReportData r = (ReportData) request.getAttribute("report");
    String[] types = (String[]) request.getAttribute("types");
    String type = (String) request.getAttribute("type");
%>
<h1>Reports</h1>
<p class="sub">Generate operational reports and download them as CSV.</p>
<div class="card">
    <form class="inline-form" method="get" action="<%= ctx %>/admin/reports">
        <div style="flex:2"><label>Report</label><select name="type"><% for (String t : types) { %><option <%= t.equals(type) ? "selected" : "" %>><%= h(t) %></option><% } %></select></div>
        <div><label>Day</label><input type="date" name="day" value="<%= request.getAttribute("day") %>"></div>
        <div><label>Month</label><input type="month" name="month" value="<%= request.getAttribute("month") %>"></div>
        <button class="btn" type="submit">Generate</button>
        <button class="btn secondary" type="submit" name="format" value="csv">Download CSV</button>
    </form>
</div>
<div class="card">
    <h2><%= h(r.getTitle()) %></h2>
    <p class="small muted"><%= r.getRows().size() %> rows &middot; generated in <%= r.getGenerationMillis() %> ms</p>
    <div class="table-wrap"><table>
        <tr><% for (String c : r.getColumns()) { %><th><%= h(c) %></th><% } %></tr>
        <% for (Object[] row : r.getRows()) { %>
        <tr><% for (Object v : row) { %><td class="<%= v instanceof Number ? "num" : "" %>"><%= h(v) %></td><% } %></tr>
        <% } %>
    </table></div>
</div>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
