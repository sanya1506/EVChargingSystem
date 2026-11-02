# Smart EV Charging Network Planning and Management System
## Domain Study (Week 1) and Software Requirement Specification (Week 2)

**Client:** ABC Green Energy Solutions Pvt. Ltd.
**Case study:** Shell.ai EV Charging Network Challenge (2022)
**Version:** 1.0

---

## Part A: Domain Study

### A.1 Industry background
Electric vehicle adoption in India is growing quickly, driven by FAME-II subsidies, state EV policies and affordable models such as the Tata Nexon EV, Tiago EV, MG Comet, Ather 450X and Ola S1. Charging infrastructure is the main constraint on growth. Customers need to:

- find a working charger nearby,
- know whether it is free,
- avoid long queues, and
- pay easily.

Operators, on the other hand, need high utilisation of expensive DC fast chargers, quick fault resolution, and accurate data for planning new sites.

The **Shell.ai EV Charging Network Challenge (2022)** asked participants to forecast EV charging demand across a region for future years, and to choose where to place slow and fast chargers so that demand is met at minimum cost. The prerequisite for such optimisation is a reliable operational platform that records **who charged where, when, for how long and how much**. This project builds that platform. AI forecasting and optimisation are left as *future enhancements*. The layered design, the hourly demand data set and the station API make them straightforward to add later.

### A.2 Business problem at ABC Green Energy
ABC Green currently manages its stations with spreadsheets. This causes the following problems:

| Problem | Impact |
|---|---|
| Duplicate customer registrations | Wrong billing, unreliable customer counts |
| Inaccurate station / charger records | Customers are sent to faulty chargers |
| No live charger availability | Long walk-in queues, poor experience |
| Manual slot allocation | Double bookings, idle chargers |
| Delayed reports | Management decides without data |
| No historical demand data | No basis for planning new chargers (the Shell.ai problem) |
| No central system | Every city works differently |

### A.3 Stakeholders

| Stakeholder | Interest |
|---|---|
| Customers (EV owners) | Find chargers, book slots, short waiting time, easy payment, invoices |
| Station operators | Start and stop sessions, handle queues and faults, collect payments |
| Administrator / management | Users, configuration, reports, utilisation, backups |
| Maintenance technicians | Maintenance schedule |
| Planning / data science team | Historical demand data for forecasting (future) |

### A.4 Charging domain concepts

| Term | Meaning |
|---|---|
| **Fast charger (DC)** | 30–150 kW, CCS2 / CHAdeMO connector, about 30–60 minutes for a car. Higher price per kWh. |
| **Slow charger (AC)** | 3.3–22 kW, Type 2 / Bharat AC-001 connector, several hours. Suitable for two-wheelers and overnight charging. |
| **SOC** | State of Charge: the battery level in %. |
| **kWh** | Unit of energy delivered. The bill is based on kWh × tariff. |
| **Utilisation** | Charging time ÷ available charger time. The key infrastructure KPI. |
| **Peak hours** | 8–10 am and 5–9 pm, when most sessions start. |
| **GST** | 18% tax on EV charging services in India. |

### A.5 Project scope summary
**In scope:** customer, vehicle, station and charger management; booking; queue; sessions; billing with simulated payment; demand data; infrastructure monitoring; maintenance; reports and CSV export; audit log; backup and restore; a Swing desktop app and a web portal.

**Out of scope (future):** AI demand forecasting, ML prediction, charger placement optimisation, route optimisation, GIS maps, a real payment gateway and push notifications.

---

## Part B: Software Requirement Specification (SRS)

### B.1 Purpose
This section specifies the functional and non-functional requirements of the Smart EV Charging Network Management System for ABC Green Energy Solutions.

### B.2 Product perspective
This is a stand-alone, three-tier system:

1. **Presentation:** a Java Swing desktop app (staff) and Servlet/JSP web pages (customers, admin).
2. **Business logic:** Java service classes.
3. **Data:** a MySQL database accessed through JDBC.

Both front ends share the same service layer and database.

### B.3 User roles

| Role | Access |
|---|---|
| **Administrator** | Everything, plus user management, backup/restore and system configuration |
| **Operator** | Customers, vehicles, stations, chargers, sessions, bookings, queue, billing, maintenance, demand, reports |
| **Customer** | Web portal only: own vehicles, bookings, charge now / queue, history, payments, invoices |
| **System (Queue Processor)** | Background thread that allocates free chargers to waiting customers and notifies operators |

### B.4 Functional requirements

| ID | Module | Requirements |
|---|---|---|
| **FR-01** | User Authentication | Login with username or email and password. Passwords hashed with PBKDF2 + salt. Account locked after 5 failed attempts. Role-based access. Customer self-registration. Logout. Session timeout of 30 minutes on the web. |
| **FR-02** | Customer Management | Add, update, delete, search customers. Validate name, email and Indian mobile number. Reject duplicate email and phone. Block or unblock customers. |
| **FR-03** | Vehicle Registration | Register vehicles with Indian registration number validation (e.g. KA01AB1234). Reject duplicate registration numbers. Record make, model, type, battery capacity and connector. |
| **FR-04** | Charging Station Management | Add, update, delete, search stations (code, city, address, latitude/longitude, hours, status). Manage fast and slow chargers (power, tariff, connector). Change charger status (Available / Faulty / Maintenance) with undo. |
| **FR-05** | Charging Slot Booking | Book a slot by station, charger type, date, time and duration. Automatic charger allocation. No double booking. Slot must be in the next 30 days. Cancel, mark no-show, start a session from a booking. |
| **FR-06** | Queue Management | If all chargers of the requested type are occupied: add the customer to a FIFO queue, show the estimated waiting time, notify the operator, and allocate the next free charger automatically. Customers can leave the queue. |
| **FR-07** | Charging Session Management | Start and stop charging. Calculate duration, energy delivered (power × efficiency × time, limited by battery capacity), end SOC and cost. Generate a receipt. Show active sessions and history. Operators stop sessions from the desktop app; customers can stop their own session from the web dashboard and go straight to payment, when the `customer_stop_charging_enabled` setting is true. |
| **FR-08** | Demand Data Management | Record hourly demand per station automatically. Daily, monthly and peak-hour demand. Station utilisation. Import and export demand data as CSV. |
| **FR-09** | Infrastructure Monitoring | Live counts of available, occupied, faulty and maintenance chargers. Capacity (kW) per station. Maintenance schedule (schedule, start, complete). Dashboard with charts and notifications. |
| **FR-10** | Billing Management | Automatic bill when a session stops (energy charge + service fee + 18% GST). Unique invoice numbers. Simulated online payment (UPI, card, net banking, wallet, cash). Failed-payment handling. Invoice download. |
| **FR-11** | Report Generation | Daily operations, monthly utilisation, peak-hour demand, daily and monthly demand, live station utilisation, revenue, top customers and audit log reports. CSV export from desktop and web. |
| **FR-12** | Database Management | Backup of all data to an SQL file. Restore in a single transaction. Audit (transaction) log. System configuration (GST, simulation speed, lockout, queue polling, customer self-stop on/off). |

### B.5 Non-functional requirements

| ID | Requirement | How it is met |
|---|---|---|
| NFR-01 | Support multiple users | Desktop and web at the same time. Per-request DB connections. Atomic charger allocation (`UPDATE ... WHERE status='AVAILABLE'`, `SELECT ... FOR UPDATE`). |
| NFR-02 | Response within 3 seconds | Indexed queries. The UI uses background threads (SwingWorker) so it never freezes. |
| NFR-03 | Data integrity | Primary and foreign keys, CHECK constraints, ENUMs, JDBC transactions with rollback. |
| NFR-04 | Prevent duplicates | UNIQUE constraints plus service-level checks with clear messages. |
| NFR-05 | Role-based authentication | `Role` enum, `AuthFilter` for the web, menu restrictions and `requireAdmin()` checks. |
| NFR-06 | Reports within 10 seconds | Generation time is measured and shown. All reports take under 1 second on 10k+ sessions (test TC-INT-06). |
| NFR-07 | Transaction logs | `audit_log` table plus `logs/application.log`. |
| NFR-08 | Backup and recovery | `BackupService` backup and transactional restore. |
| NFR-09 | Scalability / future expansion | Layered architecture (new modules plug into the service layer), DAO pattern, hourly `demand_records` data set for forecasting, JSON API `/api/stations`, stored latitude/longitude for GIS. |
| NFR-10 | User-friendly interface | Modern FlatLaf Swing UI, responsive web pages, validation messages, colour-coded statuses. |
| NFR-11 | Security | PBKDF2 hashing, PreparedStatements (no SQL injection), HTML escaping (no XSS), session-fixation protection, HttpOnly cookies, ownership checks on invoices. |

### B.6 Assumptions
1. One MySQL server is shared by the desktop and web applications, running on the same machine.
2. Payments are **simulated**. No real gateway is used. Test failures: UPI `fail@upi`, or a card number ending in `0000`.
3. Charging is **simulated**. Real elapsed time × `simulation_speed_factor` (default 60, so 1 second = 1 minute of charging) gives the charged duration.
4. "Notify operator" means an in-app notification in the dashboard, not SMS or push.
5. A vehicle in the queue starts charging at 20% SOC when it is allocated automatically.
6. Currency is Indian Rupees and GST is 18% (configurable).

### B.7 Constraints
- Java 17+, MySQL 8+, Eclipse IDE, Windows 10/11 for the presentation.
- No external web frameworks (Spring etc.). Only Servlets and JSP, as the course requires.
- AI/ML, optimisation, route planning and GIS are out of scope (extension points only).

### B.8 Inputs and outputs

| Inputs | Outputs |
|---|---|
| Customer and vehicle details, station and charger details, booking requests, charge-now requests, start SOC, stop commands, payment details, demand CSV files, configuration values | Bookings, sessions, bills/invoices (HTML/TXT), queue positions with wait estimates, notifications, dashboards and charts, CSV reports, SQL backups, audit log |
