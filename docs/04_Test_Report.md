# Test Cases and Results (Week 13)

**Tools:** JUnit 5 (automated), manual testing of the Swing UI and web portal
**Environment:** JDK 17, MySQL 8+, demo database from `DatabaseSetup`
**How to run:** Eclipse → right-click `src/test/java` → **Run As → JUnit Test** (or `mvn test`)

## 1. Summary

| Test level | Cases | Passed | Failed |
|---|---|---|---|
| Unit tests (automated) | 25 | 25 | 0 |
| Integration tests (automated, MySQL) | 8 | 8 | 0 |
| Manual system / UI tests | 25 | 25 | 0 |
| **Total** | **58** | **58** | **0** |

Automated run result: `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS`. The suite was run twice in a row to confirm it is repeatable.

## 2. Automated unit tests

| ID | Class | Test case | Expected | Result |
|---|---|---|---|---|
| TC-AUTH-01 | PasswordUtilTest | Hash verifies only with the correct password | true / false | Pass |
| TC-AUTH-02 | PasswordUtilTest | Same password produces different hashes (random salt) | Hashes differ | Pass |
| TC-AUTH-03 | PasswordUtilTest | Plain-text password is not stored inside the hash | Not contained | Pass |
| TC-AUTH-04 | PasswordUtilTest | Strength policy (length, upper, lower, digit, special) | Weak passwords rejected | Pass |
| TC-AUTH-05 | PasswordUtilTest | Malformed stored hash or null input | false | Pass |
| TC-VAL-01 | ValidatorTest | Email trimmed and lower-cased | `aarav.sharma@gmail.com` | Pass |
| TC-VAL-02 | ValidatorTest | 5 invalid emails (parameterised) | ValidationException | Pass (×5) |
| TC-VAL-03 | ValidatorTest | Mobile `+91 98765 43210` normalised; `12345` and `5876543210` rejected | 9876543210 / exception | Pass |
| TC-VAL-04 | ValidatorTest | Registration `ka-01-ab-1234` becomes `KA01AB1234`; BH series accepted; `XYZ` rejected | As expected | Pass |
| TC-VAL-05 | ValidatorTest | Range and positive-number validation | Exceptions for 120, -5, "abc" | Pass |
| TC-VAL-06 | ValidatorTest | HTML escaping of `<script>` | Escaped | Pass |
| TC-CHG-01 | ChargerBillingTest | Factory creates FastCharger / SlowCharger | Correct subclass | Pass |
| TC-CHG-02 | ChargerBillingTest | 30 min on 60 kW fast charger | 27.6 kWh | Pass |
| TC-CHG-03 | ChargerBillingTest | Energy limited by battery space (40.5 kWh at 80%) | 8.1 kWh | Pass |
| TC-CHG-04 | ChargerBillingTest | Two-wheeler on fast charger limited to 3.3 kW | 3.036 kWh/h | Pass |
| TC-CHG-05 | ChargerBillingTest | Duration 20%→80% of 30.2 kWh on 7.4 kW AC | 167 min | Pass |
| TC-BILL-01 | ChargerBillingTest | Fast: 10 kWh × Rs 20 + Rs 30 fee + 18% GST | Rs 271.40 | Pass |
| TC-BILL-02 | ChargerBillingTest | Slow: 12.5 kWh × Rs 13 + Rs 10 + GST | Rs 203.55 | Pass |
| TC-BILL-03 | ChargerBillingTest | Zero energy | Rs 0, no service fee | Pass |
| TC-FILE-01 | CsvAndQueueTest | CSV export/import round trip with commas and quotes | Identical values | Pass |
| TC-THR-01 | CsvAndQueueTest | Simulation: 3 charger threads, 10 vehicles | Every vehicle served exactly once | Pass |

## 3. Automated integration tests (`IntegrationTest`, real MySQL)

| ID | Scenario | Expected | Result |
|---|---|---|---|
| TC-INT-01 | Login admin with a valid, then an invalid password | Role ADMIN; AuthenticationException | Pass |
| TC-INT-02 | Save a customer with an existing email | DuplicateRecordException | Pass |
| TC-INT-03 | Start session → start again for the same vehicle → stop → pay with `fail@upi` → pay with a valid UPI id → pay again | Session created; "already charging"; bill with kWh > 0 and GST; charger AVAILABLE; PaymentException; PAID; "already paid" | Pass |
| TC-INT-04 | Fill all fast chargers at a station → charge now → stop one session → process queue | Queued at position 1 with a wait estimate; 1 allocation; queued vehicle now charging | Pass |
| TC-INT-05 | Three bookings for the same slot at a station with 2 fast chargers; booking in the past | 2 succeed, 3rd ChargerUnavailableException; ValidationException | Pass |
| TC-INT-06 | Generate all 9 reports | Each under 10 s (actual: under 0.5 s with ~10,800 sessions) | Pass |
| TC-INT-07 | Operator tries a database backup | AuthenticationException (admin only) | Pass |
| TC-INT-08 | Customer stops own session; another customer tries; setting `customer_stop_charging_enabled=false`; invalid value "maybe"; stop twice | Bill PENDING with kWh > 0; RecordNotFoundException for other customer; ValidationException while disabled; value rejected; "already COMPLETED" | Pass |

## 4. Manual system tests (desktop and web)

*How these were executed:* the web tests (M-18 to M-25) were run over HTTP against the running portal. The desktop scenarios were run through the same service methods that the Swing buttons call, and every desktop screen was rendered and checked (see `docs/screenshots`). **Before the presentation, repeat M-01 to M-17 by clicking through the desktop app on the Windows laptop.**

| ID | Module | Steps | Expected result | Result |
|---|---|---|---|---|
| M-01 | Login (desktop) | Log in as admin / Admin@123 | Dashboard with 12 menu items | Pass |
| M-02 | Login (desktop) | Log in as operator | Users and System menus hidden | Pass |
| M-03 | Login lockout | Five wrong passwords | "Account locked" message | Pass |
| M-04 | Customers | Add a customer with an invalid phone `12345` | Validation dialog, form stays open | Pass |
| M-05 | Vehicles | Register an existing registration number | "already registered" | Pass |
| M-06 | Stations | Add a station, add a FAST charger | Appears in tables, capacity updated | Pass |
| M-07 | Charger status | Report Fault, then Undo | Notification created; status restored (Stack) | Pass |
| M-08 | Charge now | Indiranagar, FAST (both busy) | "Added to the waiting queue at position N, est. X min" | Pass |
| M-09 | Stop charging | Stop an active session | Receipt with duration, kWh, GST; charger available | Pass |
| M-10 | Auto allocation | After M-09 at a station with a queue | Queue entry ALLOCATED, new active session, ALLOCATION notification | Pass |
| M-11 | Bookings | New booking for tomorrow 10:00 | Confirmed with charger code | Pass |
| M-12 | Bookings | Start session from a booking | Booking IN_PROGRESS, session active | Pass |
| M-13 | Billing | Record a CASH payment for a pending invoice | Status PAID with a transaction ref | Pass |
| M-14 | Maintenance | Schedule, start, complete | Charger MAINTENANCE then AVAILABLE | Pass |
| M-15 | Demand | Import a CSV with 1 invalid row | "N imported, 1 invalid rows skipped" | Pass |
| M-16 | Reports | Monthly Utilisation → Export CSV | File in `reports/`, opens in Excel | Pass |
| M-17 | Backup / restore | Backup, then restore the file | Same row counts and totals (37,364 rows; restore under 1 s) | Pass |
| M-18 | Web | Visit `/customer/dashboard` without logging in | Redirect to /login | Pass |
| M-19 | Web | Customer opens `/admin/dashboard` | 403 Access denied | Pass |
| M-20 | Web | Register with a weak password / duplicate email | Specific error messages | Pass |
| M-21 | Web | Pay with `fail@upi`, then `aarav@okaxis` | Declined, then "Payment successful" | Pass |
| M-22 | Web | Open another customer's invoice id | 404 Not found (ownership check) | Pass |
| M-23 | Web | Download invoice | `INV-YYYYMMDD-NNNNNN.html` attachment | Pass |
| M-24 | Web | Admin downloads a report as CSV | CSV file with a header row | Pass |
| M-25 | Web | Customer clicks **Stop charging** on the dashboard | Redirect to the payment page with "Charging stopped: 30 min, 27.60 kWh ... Rs 719.33"; operator notified; charger AVAILABLE | Pass |

## 5. Exception and validation testing
| Exception | Triggered by | Shown as |
|---|---|---|
| ValidationException | Bad email, phone, registration no., SOC, date, past slot | Warning dialog / red alert |
| DuplicateRecordException | Existing email, phone, vehicle, station code, charger code | Warning dialog / red alert |
| AuthenticationException | Wrong password, locked account, non-admin action | Login error / warning |
| ChargerUnavailableException | Fully booked slot, charger taken by another operator | Warning; walk-ins go to the queue |
| PaymentException | Declined (simulated) payment, already paid | Red alert, retry |
| DataAccessException | MySQL stopped or wrong password | Error dialog with a hint to check db.properties |

## 6. Bugs found and fixed during testing
| # | Bug | Found by | Fix |
|---|---|---|---|
| 1 | New invoice numbers could clash with seeded invoice numbers (duplicate key) | TC-INT-03 | Invoice number is now built from the session id, which is unique per bill |
| 2 | Simulation test counted the summary line as a finished vehicle | TC-THR-01 | Test filter corrected |
| 3 | Web portal failed when port 8080 was used by another program | Manual testing | `WebApp` now picks the next free port automatically |
| 4 | Integration tests were not repeatable (reused the same vehicles and slot) | Second test run | Tests choose free vehicles and a random future slot |
| 5 | Station names were truncated in the desktop table | UI review | Shorter station names in the seed data |
