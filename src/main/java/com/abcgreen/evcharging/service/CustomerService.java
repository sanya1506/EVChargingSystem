package com.abcgreen.evcharging.service;

import java.util.List;

import com.abcgreen.evcharging.dao.CustomerDao;
import com.abcgreen.evcharging.dao.VehicleDao;
import com.abcgreen.evcharging.exception.DuplicateRecordException;
import com.abcgreen.evcharging.exception.EVChargingException;
import com.abcgreen.evcharging.exception.RecordNotFoundException;
import com.abcgreen.evcharging.exception.ValidationException;
import com.abcgreen.evcharging.model.Customer;
import com.abcgreen.evcharging.model.Vehicle;
import com.abcgreen.evcharging.util.Validator;

/** FR-02 Customer management and FR-03 Vehicle registration. */
public class CustomerService {

    private final CustomerDao customerDao = new CustomerDao();
    private final VehicleDao vehicleDao = new VehicleDao();
    private final AuditService audit = new AuditService();

    public List<Customer> search(String keyword) {
        return customerDao.search(keyword, 1000);
    }

    public Customer findById(int id) throws RecordNotFoundException {
        Customer c = customerDao.findById(id);
        if (c == null) {
            throw new RecordNotFoundException("Customer #" + id + " not found.");
        }
        return c;
    }

    public Customer findByUserId(int userId) {
        return customerDao.findByUserId(userId);
    }

    /** Validates, normalises and checks duplicates before insert / update. */
    public void save(Customer c) throws EVChargingException {
        c.setFullName(Validator.name(c.getFullName(), "Full name"));
        c.setEmail(Validator.email(c.getEmail()));
        c.setPhone(Validator.phone(c.getPhone()));
        c.setCity(Validator.required(c.getCity(), "City"));
        if (customerDao.emailExists(c.getEmail(), c.getCustomerId())) {
            throw new DuplicateRecordException("Another customer already uses email " + c.getEmail());
        }
        if (customerDao.phoneExists(c.getPhone(), c.getCustomerId())) {
            throw new DuplicateRecordException("Another customer already uses phone " + c.getPhone());
        }
        if (c.getCustomerId() == 0) {
            customerDao.insert(c);
            audit.log("CREATE", "CUSTOMER", c.getCustomerId(), c.getFullName());
        } else {
            customerDao.update(c);
            audit.log("UPDATE", "CUSTOMER", c.getCustomerId(), c.getFullName());
        }
    }

    public void delete(int customerId) {
        customerDao.delete(customerId);
        audit.log("DELETE", "CUSTOMER", customerId, null);
    }

    public List<Vehicle> searchVehicles(String keyword) {
        return vehicleDao.search(keyword, 1000);
    }

    public List<Vehicle> vehiclesOf(int customerId) {
        return vehicleDao.findByCustomer(customerId);
    }

    public Vehicle findVehicle(int vehicleId) throws RecordNotFoundException {
        Vehicle v = vehicleDao.findById(vehicleId);
        if (v == null) {
            throw new RecordNotFoundException("Vehicle #" + vehicleId + " not found.");
        }
        return v;
    }

    public void saveVehicle(Vehicle v) throws EVChargingException {
        if (v.getCustomerId() <= 0) {
            throw new ValidationException("Please select the vehicle owner.");
        }
        v.setRegistrationNo(Validator.vehicleRegistration(v.getRegistrationNo()));
        v.setMake(Validator.required(v.getMake(), "Make"));
        v.setModel(Validator.required(v.getModel(), "Model"));
        if (v.getBatteryCapacityKwh() <= 0 || v.getBatteryCapacityKwh() > 500) {
            throw new ValidationException("Battery capacity must be between 0 and 500 kWh.");
        }
        if (vehicleDao.registrationExists(v.getRegistrationNo(), v.getVehicleId())) {
            throw new DuplicateRecordException("Vehicle " + v.getRegistrationNo() + " is already registered.");
        }
        if (v.getVehicleId() == 0) {
            vehicleDao.insert(v);
            audit.log("CREATE", "VEHICLE", v.getVehicleId(), v.getRegistrationNo());
        } else {
            vehicleDao.update(v);
            audit.log("UPDATE", "VEHICLE", v.getVehicleId(), v.getRegistrationNo());
        }
    }

    public void deleteVehicle(int vehicleId) {
        vehicleDao.delete(vehicleId);
        audit.log("DELETE", "VEHICLE", vehicleId, null);
    }
}
