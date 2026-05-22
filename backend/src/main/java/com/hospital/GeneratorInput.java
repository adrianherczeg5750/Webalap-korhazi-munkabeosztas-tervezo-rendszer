package com.hospital;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.hospital.Shift.ShiftType;

public class GeneratorInput {
    private final String month;
    private final int staffPerShift;
    private final List<User> employees;
    private final Map<Long, Set<LocalDate>> leaveDays;
    private final Map<LocalDate, Map<Long, ShiftType>> workRequests;
    private final List<ShiftAssigment> fixedAssignments;
    private final Map<Long, Integer> hoursWorked;
    private final LocalDate startDate;
    private final LocalDate endDate;

    public GeneratorInput(String month, int staffPerShift, List<User> employees,
                          Map<Long, Set<LocalDate>> leaveDays,
                          Map<LocalDate, Map<Long, ShiftType>> workRequests,
                          List<ShiftAssigment> fixedAssignments, Map<Long, Integer> hoursWorked,
                          LocalDate startDate, LocalDate endDate) {
        this.month = month;
        this.staffPerShift = staffPerShift;
        this.employees = employees;
        this.leaveDays = leaveDays;
        this.workRequests = workRequests;
        this.fixedAssignments = fixedAssignments;
        this.hoursWorked = hoursWorked;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getMonth() { return month; }
    public int getStaffPerShift() { return staffPerShift; }
    public List<User> getEmployees() { return employees; }
    public Map<Long, Set<LocalDate>> getLeaveDays() { return leaveDays; }
    public Map<LocalDate, Map<Long, ShiftType>> getWorkRequests() { return workRequests; }
    public List<ShiftAssigment> getFixedAssignments() { return fixedAssignments; }
    public Map<Long, Integer> getHoursWorked() { return hoursWorked; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
