package com.busreservation.service;

import com.busreservation.model.Staff;
import java.util.List;

public interface StaffService {
    List<Staff> getAllStaff();

    Staff getStaffById(Long id);

    Staff saveStaff(Staff staff);

    void deleteStaff(Long id);

    List<Staff> getStaffByRole(String role);

    long countStaff();
}
