package com.abcgreen.evcharging.service;

import java.time.format.DateTimeFormatter;

import com.abcgreen.evcharging.model.Payment;
import com.abcgreen.evcharging.util.Validator;

/** Renders an invoice / receipt as printable HTML (web download) or plain text (desktop receipt). */
public class InvoiceGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    private final ConfigService config = new ConfigService();

    private static String fmt(java.time.LocalDateTime t) {
        return t == null ? "-" : t.format(FMT);
    }

    public String toText(Payment p) {
        String line = "=".repeat(52);
        StringBuilder sb = new StringBuilder();
        sb.append(line).append('\n');
        sb.append(center(config.get(ConfigService.COMPANY_NAME, "ABC Green Energy Solutions Pvt. Ltd."))).append('\n');
        sb.append(center("GSTIN: " + config.get(ConfigService.COMPANY_GSTIN, "29ABCGE1234F1Z5"))).append('\n');
        sb.append(center("EV CHARGING RECEIPT")).append('\n');
        sb.append(line).append('\n');
        sb.append(String.format("Invoice No : %s%n", p.getInvoiceNo()));
        sb.append(String.format("Customer   : %s%n", p.getCustomerName()));
        sb.append(String.format("Vehicle    : %s%n", p.getVehicleRegNo()));
        sb.append(String.format("Station    : %s%n", p.getStationName()));
        sb.append(String.format("Charger    : %s%n", p.getChargerCode()));
        sb.append(String.format("Start      : %s%n", fmt(p.getSessionStart())));
        sb.append(String.format("End        : %s%n", fmt(p.getSessionEnd())));
        sb.append(String.format("Duration   : %d min%n", p.getDurationMinutes()));
        sb.append("-".repeat(52)).append('\n');
        sb.append(String.format("Energy %.2f kWh x Rs %.2f %15s%n", p.getEnergyKwh(), p.getPricePerKwh(), String.format("%.2f", p.getEnergyCharge())));
        sb.append(String.format("Service fee %38.2f%n", p.getServiceFee()));
        sb.append(String.format("GST %46.2f%n", p.getTaxAmount()));
        sb.append("-".repeat(52)).append('\n');
        sb.append(String.format("TOTAL (Rs) %39.2f%n", p.getTotalAmount()));
        sb.append(String.format("Status     : %s%s%n", p.getStatus(), p.isPaid() ? " via " + p.getMethod() + " (" + p.getTransactionRef() + ")" : ""));
        sb.append(line).append('\n');
        sb.append(center("Thank you for charging green!")).append('\n');
        return sb.toString();
    }

    private static String center(String s) {
        int pad = Math.max(0, (52 - s.length()) / 2);
        return " ".repeat(pad) + s;
    }

    public String toHtml(Payment p) {
        String company = Validator.html(config.get(ConfigService.COMPANY_NAME, "ABC Green Energy Solutions Pvt. Ltd."));
        String gstin = Validator.html(config.get(ConfigService.COMPANY_GSTIN, "29ABCGE1234F1Z5"));
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Invoice " + Validator.html(p.getInvoiceNo()) + "</title>"
                + "<style>body{font-family:Segoe UI,Arial,sans-serif;max-width:720px;margin:30px auto;color:#1f2937}"
                + "h1{color:#047857;margin:0}table{width:100%;border-collapse:collapse;margin-top:16px}"
                + "td,th{padding:8px;border-bottom:1px solid #e5e7eb;text-align:left}th{background:#ecfdf5}"
                + ".r{text-align:right}.total{font-size:1.2em;font-weight:bold}.badge{padding:3px 10px;border-radius:10px;"
                + "background:" + (p.isPaid() ? "#d1fae5;color:#065f46" : "#fef3c7;color:#92400e") + "}"
                + "@media print{button{display:none}}</style></head><body>"
                + "<h1>&#9889; " + company + "</h1><div>GSTIN: " + gstin + "</div><hr>"
                + "<h2>Tax Invoice " + Validator.html(p.getInvoiceNo()) + " <span class='badge'>" + Validator.html(p.getStatus()) + "</span></h2>"
                + "<table><tr><th>Customer</th><td>" + Validator.html(p.getCustomerName()) + "<br>" + Validator.html(p.getCustomerEmail()) + "</td>"
                + "<th>Vehicle</th><td>" + Validator.html(p.getVehicleRegNo()) + "</td></tr>"
                + "<tr><th>Station</th><td>" + Validator.html(p.getStationName()) + "</td><th>Charger</th><td>" + Validator.html(p.getChargerCode()) + "</td></tr>"
                + "<tr><th>Start</th><td>" + fmt(p.getSessionStart()) + "</td><th>End</th><td>" + fmt(p.getSessionEnd()) + "</td></tr></table>"
                + "<table><tr><th>Description</th><th class='r'>Qty</th><th class='r'>Rate (Rs)</th><th class='r'>Amount (Rs)</th></tr>"
                + String.format("<tr><td>Energy delivered</td><td class='r'>%.2f kWh</td><td class='r'>%.2f</td><td class='r'>%.2f</td></tr>",
                        p.getEnergyKwh(), p.getPricePerKwh(), p.getEnergyCharge())
                + String.format("<tr><td>Service fee</td><td class='r'>1</td><td class='r'>%.2f</td><td class='r'>%.2f</td></tr>", p.getServiceFee(), p.getServiceFee())
                + String.format("<tr><td colspan='3'>GST</td><td class='r'>%.2f</td></tr>", p.getTaxAmount())
                + String.format("<tr class='total'><td colspan='3'>Total</td><td class='r'>%.2f</td></tr></table>", p.getTotalAmount())
                + "<p>Charging duration: " + p.getDurationMinutes() + " minutes"
                + (p.isPaid() ? "<br>Paid via " + Validator.html(p.getMethod()) + " on " + fmt(p.getPaidAt()) + " - Ref " + Validator.html(p.getTransactionRef()) : "")
                + "</p><p style='color:#6b7280'>This is a computer generated invoice. Payments are simulated for demonstration.</p>"
                + "<button onclick='window.print()'>Print / Save as PDF</button></body></html>";
    }
}
