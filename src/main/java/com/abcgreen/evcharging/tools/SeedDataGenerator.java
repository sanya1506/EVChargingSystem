package com.abcgreen.evcharging.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.abcgreen.evcharging.util.PasswordUtil;

/**
 * Generates database/02_seed_data.sql with realistic demo data:
 * ~600 customers, ~700 vehicles, 24 stations in 8 cities, ~150 chargers, ~10,000 charging sessions
 * over the last 90 days with realistic peak-hour patterns, payments, bookings, queue history,
 * maintenance, notifications and audit entries.
 *
 * All timestamps are written RELATIVE to the moment the script is executed (CURDATE()/NOW()),
 * so the dashboards always show "today" data whenever the database is created.
 *
 * Run once (output is already committed): Run As > Java Application.
 */
public class SeedDataGenerator {

    private static final int DAYS = 90;
    private static final int CUSTOMERS = 600;
    private final Random rnd = new Random(2022);
    private final List<String> sql = new ArrayList<>();

    // ---------------------------------------------------------------- reference data
    private static final String[] FIRST = { "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Ayaan", "Krishna", "Ishaan",
        "Rohan", "Karthik", "Rahul", "Amit", "Suresh", "Rajesh", "Pranav", "Nikhil", "Varun", "Siddharth", "Manoj", "Deepak", "Harsh",
        "Ananya", "Diya", "Aadhya", "Saanvi", "Pari", "Anika", "Navya", "Priya", "Sneha", "Kavya", "Pooja", "Neha", "Divya", "Meera",
        "Lakshmi", "Shreya", "Riya", "Isha", "Tanvi", "Nandini", "Swati", "Anjali", "Fatima", "Zoya", "Imran", "Farhan", "Joseph",
        "Mary", "Gurpreet", "Harpreet", "Venkat", "Srinivas", "Ramya", "Keerthi", "Arvind", "Gaurav", "Abhishek" };
    private static final String[] LAST = { "Sharma", "Verma", "Iyer", "Reddy", "Nair", "Patel", "Shah", "Gupta", "Singh", "Kumar", "Rao",
        "Menon", "Pillai", "Das", "Banerjee", "Chatterjee", "Mukherjee", "Joshi", "Kulkarni", "Deshpande", "Patil", "Naidu", "Hegde",
        "Shetty", "Agarwal", "Bansal", "Mehta", "Desai", "Khan", "Ahmed", "D'Souza", "Fernandes", "Gill", "Sandhu", "Bhat", "Kapoor",
        "Malhotra", "Saxena", "Chauhan", "Yadav" };
    private static final String[] DOMAINS = { "gmail.com", "gmail.com", "gmail.com", "yahoo.co.in", "outlook.com", "rediffmail.com", "hotmail.com" };

    /** city, state code, lat, lon, weight, areas... */
    private static final String[][] CITIES = {
        { "Bengaluru", "KA", "12.9716", "77.5946", "22", "Koramangala", "Indiranagar", "Whitefield", "HSR Layout", "Jayanagar", "Electronic City", "Hebbal", "Marathahalli" },
        { "Mumbai", "MH", "19.0760", "72.8777", "16", "Andheri West", "Bandra", "Powai", "Lower Parel", "Thane", "Malad", "Chembur", "Vashi" },
        { "Delhi", "DL", "28.6139", "77.2090", "16", "Connaught Place", "Dwarka", "Saket", "Rohini", "Vasant Kunj", "Karol Bagh", "Lajpat Nagar", "Noida Sector 18" },
        { "Hyderabad", "TS", "17.3850", "78.4867", "13", "Gachibowli", "HITEC City", "Banjara Hills", "Kondapur", "Madhapur", "Secunderabad", "Kukatpally" },
        { "Chennai", "TN", "13.0827", "80.2707", "11", "T Nagar", "Adyar", "Velachery", "Anna Nagar", "OMR Sholinganallur", "Guindy", "Porur" },
        { "Pune", "MH", "18.5204", "73.8567", "10", "Hinjewadi", "Kharadi", "Baner", "Koregaon Park", "Viman Nagar", "Wakad", "Hadapsar" },
        { "Kolkata", "WB", "22.5726", "88.3639", "6", "Salt Lake", "New Town", "Park Street", "Ballygunge", "Howrah", "Behala" },
        { "Ahmedabad", "GJ", "23.0225", "72.5714", "6", "SG Highway", "Satellite", "Prahlad Nagar", "Navrangpura", "Bopal", "Maninagar" } };

    /** make, model, type, battery kWh, connector, weight */
    private static final String[][] MODELS = {
        { "Tata", "Nexon EV", "CAR", "40.5", "CCS2", "18" }, { "Tata", "Nexon EV Prime", "CAR", "30.2", "CCS2", "8" },
        { "Tata", "Tiago EV", "CAR", "24.0", "CCS2", "12" }, { "Tata", "Punch EV", "CAR", "35.0", "CCS2", "9" },
        { "MG", "ZS EV", "CAR", "50.3", "CCS2", "6" }, { "MG", "Comet EV", "CAR", "17.3", "TYPE2", "5" },
        { "MG", "Windsor EV", "CAR", "38.0", "CCS2", "6" }, { "Mahindra", "XUV400", "CAR", "39.4", "CCS2", "7" },
        { "Mahindra", "BE 6", "CAR", "59.0", "CCS2", "3" }, { "Hyundai", "Kona Electric", "CAR", "39.2", "CCS2", "3" },
        { "Hyundai", "Creta Electric", "CAR", "51.4", "CCS2", "4" }, { "BYD", "Atto 3", "CAR", "60.5", "CCS2", "3" },
        { "Kia", "EV6", "CAR", "77.4", "CCS2", "1" }, { "Citroen", "eC3", "CAR", "29.2", "CCS2", "3" },
        { "BYD", "e6", "CAR", "71.7", "CCS2", "2" }, { "Ather", "450X", "SCOOTER", "3.7", "BHARAT_AC001", "6" },
        { "Ola", "S1 Pro", "SCOOTER", "4.0", "BHARAT_AC001", "6" }, { "TVS", "iQube", "SCOOTER", "3.4", "BHARAT_AC001", "5" },
        { "Bajaj", "Chetak", "SCOOTER", "3.2", "BHARAT_AC001", "4" }, { "Revolt", "RV400", "BIKE", "3.24", "BHARAT_AC001", "2" },
        { "Mahindra", "Treo", "AUTO", "7.37", "BHARAT_AC001", "3" }, { "Piaggio", "Ape E-City", "AUTO", "8.0", "BHARAT_AC001", "2" } };

    /** station name templates per city (index matches CITIES) */
    private static final String[][] STATIONS = {
        { "Koramangala Hub", "Whitefield Tech Park", "Indiranagar 100ft Road", "Electronic City Phase 1", "Hebbal Flyover Plaza", "HSR Sector 2" },
        { "Andheri Metro Plaza", "Powai Lake Point", "Bandra Kurla Complex", "Thane Highway" },
        { "Connaught Place Central", "Dwarka Sector 21", "Saket Select City", "Noida Expressway" },
        { "HITEC City Mindspace", "Gachibowli ORR", "Banjara Hills Road 12" },
        { "OMR Sholinganallur", "T Nagar Panagal Park", "Anna Nagar Tower" },
        { "Hinjewadi Phase 2", "Kharadi EON IT Park" },
        { "Salt Lake Sector V" },
        { "SG Highway Iscon" } };
    private static final String[] CITY_CODES = { "BLR", "MUM", "DEL", "HYD", "CHN", "PUN", "KOL", "AMD" };
    private static final double[] HOUR_WEIGHTS = { 1, 0.5, 0.3, 0.3, 0.3, 0.5, 1.5, 3, 5, 6, 5, 4, 3.5, 3.5, 3.5, 4, 5, 6.5, 7.5, 7, 5.5, 4, 2.5, 1.5 };
    private static final String[] TECHNICIANS = { "Ravi Kumar (Tech)", "Suresh Babu (Tech)", "Anil Mehta (Tech)", "Prakash Rao (Tech)", "Imran Sheikh (Tech)", "Joseph Thomas (Tech)" };

    // ---------------------------------------------------------------- generated entities
    private static class Cust { int id; String name; int city; }
    private static class Veh { int id; int customerId; int city; String type; double battery; }
    private static class Station { int id; int city; int popularity; }
    private static class Charger { int id; int stationId; boolean fast; double power; double price; String status = "AVAILABLE"; }

    private final List<Cust> customers = new ArrayList<>();
    private final List<Veh> vehicles = new ArrayList<>();
    private final List<Station> stations = new ArrayList<>();
    private final List<Charger> chargers = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(args.length > 0 ? args[0] : "database/02_seed_data.sql");
        new SeedDataGenerator().generate(out);
    }

    private static String q(String s) {
        return s == null ? "NULL" : "'" + s.replace("'", "''") + "'";
    }

    private static String fmt(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    /** Timestamp expression: midnight `daysAgo` days back plus `minute` minutes. */
    private static String day(int daysAgo, int minute) {
        return "(CURDATE() - INTERVAL " + daysAgo + " DAY + INTERVAL " + minute + " MINUTE)";
    }

    private static String ago(int minutes) {
        return "(NOW() - INTERVAL " + minutes + " MINUTE)";
    }

    private int pickCity() {
        int total = 0;
        for (String[] c : CITIES) {
            total += Integer.parseInt(c[4]);
        }
        int r = rnd.nextInt(total);
        for (int i = 0; i < CITIES.length; i++) {
            r -= Integer.parseInt(CITIES[i][4]);
            if (r < 0) {
                return i;
            }
        }
        return 0;
    }

    private String[] pickModel() {
        int total = 0;
        for (String[] m : MODELS) {
            total += Integer.parseInt(m[5]);
        }
        int r = rnd.nextInt(total);
        for (String[] m : MODELS) {
            r -= Integer.parseInt(m[5]);
            if (r < 0) {
                return m;
            }
        }
        return MODELS[0];
    }

    private int pickHour() {
        double total = 0;
        for (double w : HOUR_WEIGHTS) {
            total += w;
        }
        double r = rnd.nextDouble() * total;
        for (int h = 0; h < 24; h++) {
            r -= HOUR_WEIGHTS[h];
            if (r < 0) {
                return h;
            }
        }
        return 18;
    }

    /** Writes rows as multi-row INSERT statements of up to 400 rows. */
    private void insert(String table, String columns, List<String> rows) {
        for (int i = 0; i < rows.size(); i += 400) {
            StringBuilder sb = new StringBuilder("INSERT INTO " + table + " (" + columns + ") VALUES\n");
            List<String> chunk = rows.subList(i, Math.min(rows.size(), i + 400));
            for (int j = 0; j < chunk.size(); j++) {
                sb.append(chunk.get(j)).append(j < chunk.size() - 1 ? ",\n" : ";");
            }
            sql.add(sb.toString());
        }
    }

    public void generate(Path out) throws IOException {
        sql.add("USE ev_charging_db;");
        sql.add("SET FOREIGN_KEY_CHECKS=0;");
        config();
        users();
        stationsAndChargers();
        sessionsPaymentsBookings();
        maintenance();
        notificationsAndAudit();
        sql.add("UPDATE chargers SET status = 'OCCUPIED' WHERE charger_id IN (SELECT charger_id FROM (SELECT charger_id FROM charging_sessions WHERE status = 'ACTIVE') t);");
        sql.add("INSERT INTO demand_records (station_id, demand_date, hour_of_day, sessions_count, energy_kwh)\n"
                + "SELECT ch.station_id, DATE(cs.start_time), HOUR(cs.start_time), COUNT(*), COALESCE(SUM(cs.energy_kwh), 0)\n"
                + "FROM charging_sessions cs JOIN chargers ch ON ch.charger_id = cs.charger_id WHERE cs.status = 'COMPLETED'\n"
                + "GROUP BY ch.station_id, DATE(cs.start_time), HOUR(cs.start_time);");
        sql.add("SET FOREIGN_KEY_CHECKS=1;");

        Files.createDirectories(out.toAbsolutePath().getParent());
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
            w.println("-- =====================================================================");
            w.println("-- Script 02 : Demo data for ev_charging_db (generated by SeedDataGenerator)");
            w.println("-- Dates are relative to the time this script runs.");
            w.println("-- All customer passwords: Customer@123   admin: Admin@123   operators: Operator@123");
            w.println("-- =====================================================================");
            for (String s : sql) {
                w.println(s);
            }
        }
        System.out.println("Wrote " + out + " (" + customers.size() + " customers, " + vehicles.size() + " vehicles, "
                + stations.size() + " stations, " + chargers.size() + " chargers, " + sessionCount + " sessions)");
    }

    private void config() {
        List<String> rows = new ArrayList<>();
        rows.add("('gst_rate_percent','18','GST percentage applied to charging bills')");
        rows.add("('simulation_speed_factor','60','Charging simulation speed: 60 means 1 real second = 1 minute of charging')");
        rows.add("('max_failed_logins','5','Failed login attempts before an account is locked')");
        rows.add("('queue_poll_seconds','10','How often the queue processor thread checks for free chargers')");
        rows.add("('default_slot_minutes','60','Default booking slot length in minutes')");
        rows.add("('company_name','ABC Green Energy Solutions Pvt. Ltd.','Company name printed on invoices')");
        rows.add("('company_gstin','29ABCGE1234F1Z5','Company GSTIN printed on invoices')");
        rows.add("('customer_stop_charging_enabled','true','If true, customers can stop their own charging session from the web portal; if false, only operators can stop sessions')");
        insert("system_config", "config_key, config_value, description", rows);
    }

    private void users() {
        List<String> userRows = new ArrayList<>();
        userRows.add("(1,'admin'," + q(PasswordUtil.hash("Admin@123")) + ",'System Administrator','admin@abcgreen.in','9876500001','ADMIN'," + day(200, 600) + ")");
        userRows.add("(2,'operator'," + q(PasswordUtil.hash("Operator@123")) + ",'Kiran Rao','kiran.rao@abcgreen.in','9876500002','OPERATOR'," + day(200, 620) + ")");
        userRows.add("(3,'operator.mumbai'," + q(PasswordUtil.hash("Operator@123")) + ",'Sunil Patil','sunil.patil@abcgreen.in','9876500003','OPERATOR'," + day(150, 600) + ")");
        userRows.add("(4,'operator.delhi'," + q(PasswordUtil.hash("Operator@123")) + ",'Meenakshi Gupta','meenakshi.gupta@abcgreen.in','9876500004','OPERATOR'," + day(120, 600) + ")");

        List<String> custRows = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();
        Set<String> usedPhones = new HashSet<>();
        Set<String> usedUsernames = new HashSet<>();
        for (int i = 1; i <= CUSTOMERS; i++) {
            Cust c = new Cust();
            c.id = i;
            String first = i == 1 ? "Aarav" : FIRST[rnd.nextInt(FIRST.length)];
            String last = i == 1 ? "Sharma" : LAST[rnd.nextInt(LAST.length)];
            c.name = first + " " + last;
            c.city = i == 1 ? 0 : pickCity();
            String base = (first + "." + last).toLowerCase().replace("'", "");
            String username = base;
            int n = 1;
            while (usedUsernames.contains(username)) {
                username = base + (++n);
            }
            usedUsernames.add(username);
            String email = username + "@" + DOMAINS[rnd.nextInt(DOMAINS.length)];
            while (usedEmails.contains(email)) {
                email = username + rnd.nextInt(100) + "@gmail.com";
            }
            usedEmails.add(email);
            String phone;
            do {
                phone = (6 + rnd.nextInt(4)) + String.format("%09d", rnd.nextInt(1_000_000_000));
            } while (usedPhones.contains(phone));
            usedPhones.add(phone);
            String[] city = CITIES[c.city];
            String address = (1 + rnd.nextInt(450)) + ", " + (rnd.nextBoolean() ? "Cross " + (1 + rnd.nextInt(12)) + ", " : "Main Road, ")
                    + city[5 + rnd.nextInt(city.length - 5)] + ", " + city[0];
            int regDays = 5 + rnd.nextInt(360);
            int userId = 4 + i;
            // one hash per customer (unique salt) - takes a few seconds
            userRows.add("(" + userId + "," + q(username) + "," + q(PasswordUtil.hash("Customer@123")) + "," + q(c.name) + "," + q(email) + ","
                    + q(phone) + ",'CUSTOMER'," + day(regDays, 540 + rnd.nextInt(600)) + ")");
            custRows.add("(" + i + "," + userId + "," + q(c.name) + "," + q(email) + "," + q(phone) + "," + q(city[0]) + "," + q(address) + ","
                    + day(regDays, 540 + rnd.nextInt(600)) + ",'" + (rnd.nextInt(100) < 2 ? "BLOCKED" : "ACTIVE") + "')");
            customers.add(c);
        }
        insert("users", "user_id, username, password_hash, full_name, email, phone, role, created_at", userRows);
        insert("customers", "customer_id, user_id, full_name, email, phone, city, address, registered_on, status", custRows);
        sql.add("UPDATE users SET username = 'aarav.sharma' WHERE user_id = 5;");

        // vehicles: everyone has one, ~18% have a second one (customer 1 has a car and a scooter)
        List<String> vehRows = new ArrayList<>();
        Set<String> regs = new HashSet<>();
        int vid = 0;
        for (Cust c : customers) {
            int count = c.id == 1 ? 2 : (rnd.nextInt(100) < 18 ? 2 : 1);
            for (int k = 0; k < count; k++) {
                String[] m = c.id == 1 ? (k == 0 ? MODELS[0] : MODELS[15]) : pickModel();
                if (k == 1 && c.id != 1 && "CAR".equals(m[2]) && rnd.nextBoolean()) {
                    m = MODELS[15 + rnd.nextInt(4)]; // second vehicle is often a scooter
                }
                String reg;
                do {
                    reg = CITIES[c.city][1] + String.format("%02d", 1 + rnd.nextInt(60)) + (char) ('A' + rnd.nextInt(26))
                            + (char) ('A' + rnd.nextInt(26)) + String.format("%04d", 1 + rnd.nextInt(9999));
                } while (regs.contains(reg));
                regs.add(reg);
                Veh v = new Veh();
                v.id = ++vid;
                v.customerId = c.id;
                v.city = c.city;
                v.type = m[2];
                v.battery = Double.parseDouble(m[3]);
                vehicles.add(v);
                vehRows.add("(" + v.id + "," + c.id + "," + q(reg) + "," + q(m[0]) + "," + q(m[1]) + ",'" + m[2] + "'," + m[3] + ",'" + m[4] + "',"
                        + day(1 + rnd.nextInt(300), 600) + ")");
            }
        }
        insert("vehicles", "vehicle_id, customer_id, registration_no, make, model, vehicle_type, battery_capacity_kwh, connector_type, created_at", vehRows);
    }

    private void stationsAndChargers() {
        List<String> stRows = new ArrayList<>();
        List<String> chRows = new ArrayList<>();
        int sid = 0;
        int cid = 0;
        for (int ci = 0; ci < STATIONS.length; ci++) {
            String[] city = CITIES[ci];
            for (int k = 0; k < STATIONS[ci].length; k++) {
                Station s = new Station();
                s.id = ++sid;
                s.city = ci;
                s.popularity = 60 + rnd.nextInt(80);
                stations.add(s);
                String code = CITY_CODES[ci] + "-" + String.format("%03d", k + 1);
                double lat = Double.parseDouble(city[2]) + (rnd.nextDouble() - 0.5) * 0.15;
                double lon = Double.parseDouble(city[3]) + (rnd.nextDouble() - 0.5) * 0.15;
                String hours = rnd.nextInt(10) < 7 ? "00:00-23:59" : "06:00-23:00";
                String status = s.id == 24 ? "MAINTENANCE" : "ACTIVE";
                stRows.add("(" + s.id + "," + q(code) + "," + q(STATIONS[ci][k]) + "," + q(city[0]) + ","
                        + q(STATIONS[ci][k] + ", " + city[5 + rnd.nextInt(city.length - 5)] + ", " + city[0]) + "," + String.format(Locale.ROOT, "%.6f,%.6f", lat, lon)
                        + "," + q(hours) + "," + q("80" + (40000000 + rnd.nextInt(9999999))) + ",'" + status + "'," + "(CURDATE() - INTERVAL " + (200 + rnd.nextInt(700)) + " DAY))");

                // small "demo queue" station (Indiranagar) has only 2 fast chargers
                int fast = s.id == 3 ? 2 : 2 + rnd.nextInt(3);
                int slow = s.id == 3 ? 2 : 2 + rnd.nextInt(3);
                for (int f = 1; f <= fast + slow; f++) {
                    Charger ch = new Charger();
                    ch.id = ++cid;
                    ch.stationId = s.id;
                    ch.fast = f <= fast;
                    String connector;
                    if (ch.fast) {
                        double[] powers = { 30, 50, 60, 60, 120 };
                        ch.power = powers[rnd.nextInt(powers.length)];
                        ch.price = ch.power >= 100 ? 24 : ch.power >= 50 ? 21 : 19;
                        connector = rnd.nextInt(10) == 0 ? "CHADEMO" : "CCS2";
                    } else {
                        double[] powers = { 3.3, 7.4, 7.4, 11, 22 };
                        ch.power = powers[rnd.nextInt(powers.length)];
                        ch.price = ch.power >= 11 ? 15 : 13;
                        connector = ch.power == 3.3 ? "BHARAT_AC001" : "TYPE2";
                    }
                    int r = rnd.nextInt(100);
                    if (s.id == 24) {
                        ch.status = "MAINTENANCE";
                    } else if (s.id != 3 && r < 4) {
                        ch.status = "FAULTY";
                    } else if (s.id != 3 && r < 7) {
                        ch.status = "MAINTENANCE";
                    }
                    chargers.add(ch);
                    String chCode = code + "-" + (ch.fast ? "F" + f : "S" + (f - fast));
                    chRows.add("(" + ch.id + "," + s.id + "," + q(chCode) + ",'" + (ch.fast ? "FAST" : "SLOW") + "','" + connector + "'," + fmt(ch.power)
                            + "," + fmt(ch.price) + ",'" + ch.status + "',(CURDATE() - INTERVAL " + (100 + rnd.nextInt(500)) + " DAY))");
                }
            }
        }
        insert("stations", "station_id, station_code, name, city, address, latitude, longitude, operating_hours, contact_phone, status, opened_on", stRows);
        insert("chargers", "charger_id, station_id, charger_code, charger_type, connector_type, power_kw, price_per_kwh, status, installed_on", chRows);
    }

    private int sessionCount;

    private Veh pickVehicle(Station s, boolean fast) {
        for (int attempt = 0; attempt < 50; attempt++) {
            Veh v = vehicles.get(rnd.nextInt(vehicles.size()));
            boolean cityOk = v.city == s.city || rnd.nextInt(100) < 5;
            boolean typeOk = fast ? "CAR".equals(v.type) : true;
            if (cityOk && typeOk) {
                return v;
            }
        }
        return vehicles.get(0);
    }

    private void sessionsPaymentsBookings() {
        List<String> sessRows = new ArrayList<>();
        List<String> payRows = new ArrayList<>();
        List<String> bookRows = new ArrayList<>();
        List<String> queueRows = new ArrayList<>();
        int sid = 0;
        int bid = 0;
        int qid = 0;
        String[] methods = { "UPI", "UPI", "UPI", "UPI", "UPI", "UPI", "CARD", "CARD", "CARD", "WALLET", "WALLET", "NETBANKING", "CASH", "CASH" };

        for (Charger ch : chargers) {
            Station st = stations.get(ch.stationId - 1);
            if (st.id == 24) {
                continue; // station under maintenance has no recent sessions
            }
            for (int d = DAYS; d >= 1; d--) {
                double growth = 0.7 + 0.6 * (DAYS - d) / (double) DAYS; // demand grows over time
                int dow = (int) ((d + 3) % 7);
                double weekend = dow < 2 ? 1.25 : 1.0;
                double mean = (ch.fast ? 1.0 : 0.5) * growth * weekend * st.popularity / 100.0;
                int count = poisson(mean);
                List<Integer> starts = new ArrayList<>();
                for (int k = 0; k < count; k++) {
                    starts.add(pickHour() * 60 + rnd.nextInt(60));
                }
                starts.sort(null);
                int cursor = 0;
                for (int start : starts) {
                    start = Math.max(start, cursor + 5);
                    if (start > 23 * 60 + 50) {
                        break;
                    }
                    Veh v = (ch.stationId <= 6 && rnd.nextInt(100) < 2) ? vehicles.get(rnd.nextInt(2)) : pickVehicle(st, ch.fast);
                    int startSoc = 8 + rnd.nextInt(50);
                    int targetSoc = 75 + rnd.nextInt(26);
                    double eff = ch.fast ? 0.92 : 0.88;
                    double power = ch.fast && v.battery < 10 ? 3.3 : ch.power;
                    double energy = v.battery * (targetSoc - startSoc) / 100.0;
                    int duration = (int) Math.ceil(energy / (power * eff) * 60);
                    duration = Math.max(10, Math.min(duration, ch.fast ? 120 : 600));
                    energy = Math.min(energy, power * eff * duration / 60.0);
                    int endSoc = (int) Math.min(100, Math.round(startSoc + energy * 100 / v.battery));
                    cursor = start + duration;
                    double cost = energy * ch.price;
                    sid++;
                    String startExpr = day(d, start);
                    String endExpr = day(d, start + duration);
                    Integer bookingId = null;
                    if (rnd.nextInt(100) < 25) {
                        bookingId = ++bid;
                        int slotStart = (start / 30) * 30;
                        bookRows.add("(" + bookingId + "," + v.customerId + "," + v.id + "," + st.id + "," + ch.id + "," + day(d, slotStart) + ","
                                + day(d, slotStart + Math.max(60, ((duration + 29) / 30) * 30)) + ",'COMPLETED'," + day(d + 1 + rnd.nextInt(3), 600) + ")");
                    }
                    sessRows.add("(" + sid + "," + (bookingId == null ? "NULL" : bookingId) + "," + v.customerId + "," + v.id + "," + ch.id + "," + startExpr + ","
                            + endExpr + "," + startSoc + "," + endSoc + "," + fmt(energy) + "," + duration + "," + fmt(cost) + ",'COMPLETED')");
                    payRows.add(payment(sid, ch, energy, d, start + duration, methods, d <= 2 ? 22 : (v.customerId == 1 && d <= 10 ? 50 : 2)));
                    if (rnd.nextInt(100) < 4) { // the customer had to wait in the queue for this session
                        int wait = 5 + rnd.nextInt(40);
                        qid++;
                        queueRows.add("(" + qid + "," + st.id + "," + v.customerId + "," + v.id + ",'" + (ch.fast ? "FAST" : "SLOW") + "'," + day(d, Math.max(0, start - wait))
                                + "," + wait + ",'ALLOCATED'," + ch.id + "," + startExpr + ")");
                    }
                }
            }
        }

        // today: some completed sessions in the last few hours and some ACTIVE sessions now
        int activeCount = 0;
        Set<Integer> busyVehicles = new HashSet<>();
        busyVehicles.add(1); // demo customer's car is free for the live demo
        for (Charger ch : chargers) {
            if (!"AVAILABLE".equals(ch.status)) {
                continue;
            }
            Station st = stations.get(ch.stationId - 1);
            boolean demoQueueStation = st.id == 3 && ch.fast;
            int r = rnd.nextInt(100);
            if (demoQueueStation || r < 9) {
                Veh v = demoQueueStation ? pickVehicle(st, true) : pickVehicle(st, ch.fast);
                while (!busyVehicles.add(v.id)) {
                    v = pickVehicle(st, ch.fast);
                }
                sid++;
                activeCount++;
                sessRows.add("(" + sid + ",NULL," + v.customerId + "," + v.id + "," + ch.id + "," + ago(5 + rnd.nextInt(40)) + ",NULL," + (10 + rnd.nextInt(40))
                        + ",NULL,NULL,NULL,NULL,'ACTIVE')");
                ch.status = "OCCUPIED";
            } else if (r < 45) {
                Veh v = pickVehicle(st, ch.fast);
                int duration = ch.fast ? 25 + rnd.nextInt(50) : 60 + rnd.nextInt(150);
                int endAgo = 10 + rnd.nextInt(200);
                double energy = Math.min(v.battery * 0.6, (ch.fast && v.battery < 10 ? 3.3 : ch.power) * (ch.fast ? 0.92 : 0.88) * duration / 60.0);
                int startSoc = 10 + rnd.nextInt(30);
                int endSoc = (int) Math.min(100, Math.round(startSoc + energy * 100 / v.battery));
                sid++;
                sessRows.add("(" + sid + ",NULL," + v.customerId + "," + v.id + "," + ch.id + "," + ago(endAgo + duration) + "," + ago(endAgo) + "," + startSoc + ","
                        + endSoc + "," + fmt(energy) + "," + duration + "," + fmt(energy * ch.price) + ",'COMPLETED')");
                payRows.add(paymentToday(sid, ch, energy, endAgo, methods));
            }
        }
        // demo customer (Aarav Sharma) has one bill from today waiting for online payment
        Charger demoCh = chargers.get(0);
        sid++;
        sessRows.add("(" + sid + ",NULL,1,1," + demoCh.id + "," + ago(170) + "," + ago(120) + ",22,81," + fmt(23.9) + ",50," + fmt(23.9 * demoCh.price) + ",'COMPLETED')");
        double e = 23.9 * demoCh.price;
        double tax = (e + 30) * 0.18;
        payRows.add("(" + sid + ",CONCAT('INV-', DATE_FORMAT(CURDATE(), '%Y%m%d'), '-', LPAD(" + sid + ", 6, '0'))," + fmt(e) + ",30.00," + fmt(tax) + "," + fmt(e + 30 + tax)
                + ",NULL,'PENDING',NULL," + ago(120) + ",NULL)");
        sessionCount = sid;

        // waiting queue at the demo station (both fast chargers busy)
        for (int k = 0; k < 2; k++) {
            Veh v = pickVehicle(stations.get(2), true);
            while (!busyVehicles.add(v.id)) {
                v = pickVehicle(stations.get(2), true);
            }
            qid++;
            queueRows.add("(" + qid + ",3," + v.customerId + "," + v.id + ",'FAST'," + ago(12 - k * 5) + "," + (20 + k * 25) + ",'WAITING',NULL,NULL)");
        }
        // a few cancelled queue entries
        for (int k = 0; k < 15; k++) {
            Veh v = vehicles.get(rnd.nextInt(vehicles.size()));
            qid++;
            queueRows.add("(" + qid + "," + (1 + rnd.nextInt(23)) + "," + v.customerId + "," + v.id + ",'FAST'," + day(1 + rnd.nextInt(DAYS), 480 + rnd.nextInt(700))
                    + "," + (15 + rnd.nextInt(40)) + ",'CANCELLED',NULL,NULL)");
        }

        // historical cancelled / no-show bookings and upcoming bookings for the next 7 days
        for (int k = 0; k < 220; k++) {
            Veh v = vehicles.get(rnd.nextInt(vehicles.size()));
            Charger ch = chargers.get(rnd.nextInt(chargers.size()));
            int d = 1 + rnd.nextInt(DAYS);
            int slot = (8 + rnd.nextInt(13)) * 60;
            bookRows.add("(" + (++bid) + "," + v.customerId + "," + v.id + "," + ch.stationId + "," + ch.id + "," + day(d, slot) + "," + day(d, slot + 60) + ",'"
                    + (rnd.nextInt(100) < 65 ? "CANCELLED" : "NO_SHOW") + "'," + day(d + 2, 600) + ")");
        }
        Set<String> taken = new HashSet<>();
        for (int k = 0; k < 140; k++) {
            Charger ch = chargers.get(rnd.nextInt(chargers.size()));
            if (!"AVAILABLE".equals(ch.status) && !"OCCUPIED".equals(ch.status)) {
                continue;
            }
            Station st = stations.get(ch.stationId - 1);
            Veh v = k < 2 ? vehicles.get(0) : pickVehicle(st, ch.fast);
            int d = -(1 + rnd.nextInt(7)); // future day
            int slot = (7 + rnd.nextInt(15)) * 60;
            if (!taken.add(ch.id + ":" + d + ":" + slot)) {
                continue;
            }
            bookRows.add("(" + (++bid) + "," + v.customerId + "," + v.id + "," + ch.stationId + "," + ch.id + ",(CURDATE() + INTERVAL " + (-d) + " DAY + INTERVAL " + slot
                    + " MINUTE),(CURDATE() + INTERVAL " + (-d) + " DAY + INTERVAL " + (slot + 60) + " MINUTE),'BOOKED'," + ago(60 + rnd.nextInt(3000)) + ")");
        }

        insert("bookings", "booking_id, customer_id, vehicle_id, station_id, charger_id, slot_start, slot_end, status, created_at", bookRows);
        insert("charging_sessions", "session_id, booking_id, customer_id, vehicle_id, charger_id, start_time, end_time, start_soc, end_soc, energy_kwh, duration_minutes, cost, status", sessRows);
        insert("payments", "session_id, invoice_no, energy_charge, service_fee, tax_amount, total_amount, method, status, transaction_ref, created_at, paid_at", payRows);
        insert("queue_entries", "queue_id, station_id, customer_id, vehicle_id, charger_type, requested_at, estimated_wait_min, status, allocated_charger_id, allocated_at", queueRows);
        System.out.println("Active sessions now: " + activeCount);
    }

    private String payment(int sessionId, Charger ch, double energy, int daysAgo, int endMinute, String[] methods, int pendingPercent) {
        double e = energy * ch.price;
        double fee = ch.fast ? 30 : 10;
        double tax = (e + fee) * 0.18;
        boolean pending = rnd.nextInt(100) < pendingPercent;
        String method = methods[rnd.nextInt(methods.length)];
        String paidAt = day(daysAgo, endMinute + 1 + rnd.nextInt(30));
        return "(" + sessionId + ",CONCAT('INV-', DATE_FORMAT(" + day(daysAgo, endMinute) + ", '%Y%m%d'), '-', LPAD(" + sessionId + ", 6, '0')),"
                + fmt(e) + "," + fmt(fee) + "," + fmt(tax) + "," + fmt(e + fee + tax) + ","
                + (pending ? "NULL,'PENDING',NULL," : "'" + method + "','PAID','TXN" + Long.toHexString(Double.doubleToLongBits(rnd.nextDouble())).substring(0, 12).toUpperCase() + "',")
                + day(daysAgo, endMinute) + "," + (pending ? "NULL" : paidAt) + ")";
    }

    private String paymentToday(int sessionId, Charger ch, double energy, int endAgo, String[] methods) {
        double e = energy * ch.price;
        double fee = ch.fast ? 30 : 10;
        double tax = (e + fee) * 0.18;
        boolean pending = rnd.nextInt(100) < 25;
        return "(" + sessionId + ",CONCAT('INV-', DATE_FORMAT(CURDATE(), '%Y%m%d'), '-', LPAD(" + sessionId + ", 6, '0')),"
                + fmt(e) + "," + fmt(fee) + "," + fmt(tax) + "," + fmt(e + fee + tax) + ","
                + (pending ? "NULL,'PENDING',NULL," : "'" + methods[rnd.nextInt(methods.length)] + "','PAID','TXN"
                        + Long.toHexString(Double.doubleToLongBits(rnd.nextDouble())).substring(0, 12).toUpperCase() + "',")
                + ago(endAgo) + "," + (pending ? "NULL" : ago(Math.max(1, endAgo - 3))) + ")";
    }

    private int poisson(double mean) {
        double l = Math.exp(-mean);
        double p = 1.0;
        int k = 0;
        do {
            k++;
            p *= rnd.nextDouble();
        } while (p > l);
        return k - 1;
    }

    private void maintenance() {
        List<String> rows = new ArrayList<>();
        String[] descPrev = { "Quarterly preventive check - connectors and cables", "Cooling fan cleaning and filter replacement",
            "Firmware update and calibration", "Earthing and insulation resistance test" };
        String[] descCorr = { "Connector pin damaged - replace gun", "Communication error with OCPP backend", "RCD tripping frequently",
            "Display panel not working", "Power module overheating" };
        for (Charger ch : chargers) {
            // completed preventive maintenance in the past
            for (int k = 0; k < 1 + rnd.nextInt(2); k++) {
                int d = 5 + rnd.nextInt(170);
                rows.add("(" + ch.id + ",(CURDATE() - INTERVAL " + d + " DAY),'" + (rnd.nextInt(4) == 0 ? "INSPECTION" : "PREVENTIVE") + "',"
                        + q(descPrev[rnd.nextInt(descPrev.length)]) + "," + q(TECHNICIANS[rnd.nextInt(TECHNICIANS.length)]) + ",'COMPLETED',(CURDATE() - INTERVAL " + d + " DAY))");
            }
            if ("MAINTENANCE".equals(ch.status)) {
                rows.add("(" + ch.id + ",CURDATE(),'CORRECTIVE'," + q(descCorr[rnd.nextInt(descCorr.length)]) + "," + q(TECHNICIANS[rnd.nextInt(TECHNICIANS.length)])
                        + ",'IN_PROGRESS',NULL)");
            } else if ("FAULTY".equals(ch.status)) {
                rows.add("(" + ch.id + ",(CURDATE() + INTERVAL " + (1 + rnd.nextInt(3)) + " DAY),'CORRECTIVE'," + q(descCorr[rnd.nextInt(descCorr.length)]) + ","
                        + q(TECHNICIANS[rnd.nextInt(TECHNICIANS.length)]) + ",'SCHEDULED',NULL)");
            } else if (rnd.nextInt(100) < 15) {
                rows.add("(" + ch.id + ",(CURDATE() + INTERVAL " + (2 + rnd.nextInt(25)) + " DAY),'PREVENTIVE'," + q(descPrev[rnd.nextInt(descPrev.length)]) + ","
                        + q(TECHNICIANS[rnd.nextInt(TECHNICIANS.length)]) + ",'SCHEDULED',NULL)");
            }
        }
        insert("maintenance_schedule", "charger_id, scheduled_date, maintenance_type, description, technician, status, completed_on", rows);
    }

    private void notificationsAndAudit() {
        List<String> n = new ArrayList<>();
        n.add("('QUEUE','2 customers are waiting for a FAST charger at Indiranagar 100ft Road.',FALSE," + ago(7) + ")");
        n.add("('FAULT','Charger reported FAULTY at Whitefield Tech Park. Corrective maintenance scheduled.',FALSE," + ago(55) + ")");
        n.add("('PAYMENT','Payment of Rs 612.40 received (UPI).',FALSE," + ago(18) + ")");
        n.add("('BOOKING','New booking for tomorrow 09:00 at Koramangala Hub.',FALSE," + ago(35) + ")");
        n.add("('MAINTENANCE','Station SG Highway Iscon is under maintenance - all chargers offline.',FALSE," + ago(240) + ")");
        n.add("('SYSTEM','Nightly database backup completed successfully.',TRUE," + day(0, 120) + ")");
        String[] types = { "PAYMENT", "BOOKING", "ALLOCATION", "QUEUE", "SYSTEM" };
        for (int k = 0; k < 40; k++) {
            String t = types[rnd.nextInt(types.length)];
            String msg;
            switch (t) {
                case "PAYMENT": msg = "Payment of Rs " + (150 + rnd.nextInt(900)) + "." + (10 + rnd.nextInt(89)) + " received."; break;
                case "BOOKING": msg = "New booking received for station #" + (1 + rnd.nextInt(24)) + "."; break;
                case "ALLOCATION": msg = "Queue: customer allocated to a free charger at station #" + (1 + rnd.nextInt(24)) + "."; break;
                case "QUEUE": msg = "Customer joined the waiting queue at station #" + (1 + rnd.nextInt(24)) + "."; break;
                default: msg = "Daily report generated and exported."; break;
            }
            n.add("('" + t + "'," + q(msg) + ",TRUE," + day(1 + rnd.nextInt(30), rnd.nextInt(1440)) + ")");
        }
        insert("notifications", "type, message, is_read, created_at", n);

        List<String> a = new ArrayList<>();
        String[] users = { "admin", "operator", "operator.mumbai", "operator.delhi" };
        String[][] actions = { { "LOGIN", "USER" }, { "CREATE", "CUSTOMER" }, { "UPDATE", "STATION" }, { "STATUS", "CHARGER" },
            { "START", "SESSION" }, { "STOP", "SESSION" }, { "EXPORT", "REPORT" }, { "SCHEDULE", "MAINTENANCE" }, { "BACKUP", "DATABASE" } };
        for (int k = 0; k < 400; k++) {
            String[] act = actions[rnd.nextInt(actions.length)];
            String u = act[0].equals("BACKUP") ? "admin" : users[rnd.nextInt(users.length)];
            a.add("(" + q(u) + "," + q(act[0]) + "," + q(act[1]) + "," + q(String.valueOf(1 + rnd.nextInt(500))) + ","
                    + q(act[0].toLowerCase() + " " + act[1].toLowerCase() + " (seed data)") + "," + day(rnd.nextInt(DAYS), rnd.nextInt(1440)) + ")");
        }
        insert("audit_log", "username, action, entity, entity_id, details, created_at", a);
    }
}
