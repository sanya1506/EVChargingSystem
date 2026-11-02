<%@ page contentType="text/html;charset=UTF-8" %>
<% String pageTitle = "Pay invoice"; %>
<%@ include file="/WEB-INF/jsp/common/header.jspf" %>
<% Payment p = (Payment) request.getAttribute("payment"); %>
<div class="auth wide">
    <div class="card">
        <h1>Pay <%= money(p.getTotalAmount()) %></h1>
        <p class="sub">Invoice <%= h(p.getInvoiceNo()) %> &middot; <%= h(p.getStationName()) %> &middot; <%= h(p.getVehicleRegNo()) %></p>
        <table>
            <tr><td>Energy <%= String.format("%.2f", p.getEnergyKwh()) %> kWh &times; <%= money(p.getPricePerKwh()) %></td><td class="num"><%= money(p.getEnergyCharge()) %></td></tr>
            <tr><td>Service fee</td><td class="num"><%= money(p.getServiceFee()) %></td></tr>
            <tr><td>GST</td><td class="num"><%= money(p.getTaxAmount()) %></td></tr>
            <tr><td><b>Total</b></td><td class="num"><b><%= money(p.getTotalAmount()) %></b></td></tr>
        </table>
        <form method="post" action="<%= ctx %>/customer/pay" style="margin-top:18px">
            <input type="hidden" name="id" value="<%= p.getPaymentId() %>">
            <label>Payment method</label>
            <div class="pay-methods">
                <label><input type="radio" name="method" value="UPI" checked onclick="hint('UPI id, e.g. aarav@okaxis')"> UPI</label>
                <label><input type="radio" name="method" value="CARD" onclick="hint('16-digit card number')"> Card</label>
                <label><input type="radio" name="method" value="NETBANKING" onclick="hint('Bank name')"> Net banking</label>
                <label><input type="radio" name="method" value="WALLET" onclick="hint('Wallet mobile number')"> Wallet</label>
            </div>
            <label id="detailLabel">UPI id, e.g. aarav@okaxis</label>
            <input name="detail" id="detail" value="aarav@okaxis" required>
            <p><button class="btn block" type="submit">Pay securely (simulation)</button></p>
            <p class="small muted">This is a simulated payment gateway - no money is charged. Use <b>fail@upi</b> or a card ending in <b>0000</b> to test a failed payment.</p>
        </form>
    </div>
</div>
<script>
function hint(t) { document.getElementById('detailLabel').textContent = t; document.getElementById('detail').value = ''; }
</script>
<%@ include file="/WEB-INF/jsp/common/footer.jspf" %>
