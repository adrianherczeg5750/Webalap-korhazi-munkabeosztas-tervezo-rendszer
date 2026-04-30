package com.hospital;

import com.hospital.Shift.ShiftType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class BacktrackingGenerator implements ShiftGenerator {
    @Override
    public List<ShiftAssigment> generate(GeneratorInput input) {
        List<ShiftAssigment> result = new ArrayList<>();

        YearMonth yearMonth = YearMonth.parse(input.getMonth());
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<User> employees = input.getEmployees();
        Map<Long, Set<LocalDate>> leaveMap = input.getLeaveDays();
        Map<LocalDate, Map<Long, ShiftType>> workRequestMap = input.getWorkRequests();

        Map<Long, Integer> hoursWorked = new HashMap<>();
        Map<LocalDate, Set<Long>> assignedPerDay = new HashMap<>();

        Shift.ShiftType[] shiftTypes = { Shift.ShiftType.MORNING, Shift.ShiftType.AFTERNOON, Shift.ShiftType.NIGHT};
        List<Slot> slots = new ArrayList<>();
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            for (Shift.ShiftType shiftType : shiftTypes) {
                for (int i = 0; i < input.getStaffPerShift(); i++){
                    slots.add(new Slot(currentDate, shiftType));
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        boolean success = solve(0, slots, result, assignedPerDay, hoursWorked, employees, workRequestMap, leaveMap);
        if (!success) {
            throw new RuntimeException("Nem generálható beosztás erre a hónapra!");
        }
        return result;
    }

    private boolean isValid(User emp, LocalDate date, Set<Long> assignedToday, Map<Long, Set<LocalDate>> leaveDays) {
        Long empId = emp.getId();
        if (assignedToday.contains(empId)) return false;
        Set<LocalDate> leaves = leaveDays.get(empId);
        if (leaves != null && leaves.contains(date)) return false;
        return true;
    }

    private List<User> getCandidates(LocalDate date,
                                     Shift.ShiftType shiftType,
                                     Set<Long> assignedToday,
                                     Map<Long, Set<LocalDate>> leaveDays,
                                     List<User> employees,
                                     Map<Long, Integer> hoursWorked,
                                     Map<LocalDate, Map<Long, Shift.ShiftType>> workRequests) {

        List<User> valid = new ArrayList<>();
        for (User user : employees) {
            if (isValid(user, date, assignedToday, leaveDays)) {
                valid.add(user);
            }
        }

        valid.sort(Comparator.comparingInt(e -> hoursWorked.getOrDefault(e.getId(), 0)));

        Map<Long, Shift.ShiftType> dayRequests = workRequests.getOrDefault(date, Map.of());
        List<User> prioritized = new ArrayList<>();
        List<User> others = new ArrayList<>();
        for (User emp : valid) {
            if (dayRequests.get(emp.getId()) == shiftType) {
                prioritized.add(emp);
            } else others.add(emp);
        }
        prioritized.addAll(others);
        return prioritized;
    }

    private boolean solve(int slotIndex,
                          List<Slot> slots,
                          List<ShiftAssigment> result,
                          Map<LocalDate, Set<Long>> assignedPerDay,
                          Map<Long, Integer> hoursWorked,
                          List<User> employees,
                          Map<LocalDate, Map<Long, Shift.ShiftType>> workRequests,
                          Map<Long, Set<LocalDate>> leaveDays) {
        if (slotIndex == slots.size()) return true;
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;

        Slot slot = slots.get(slotIndex);
        Set<Long> assignedToday = assignedPerDay.computeIfAbsent(slot.date, k -> new HashSet<>());

        List<User> candidates = getCandidates(slot.date, slot.shiftType, assignedToday, leaveDays, employees, hoursWorked, workRequests);

        for (User emp : candidates) {
            Long empId = emp.getId();

            result.add(new ShiftAssigment(empId, slot.date, slot.shiftType));
            assignedToday.add(empId);
            assignedPerDay.put(slot.date, assignedToday);
            hoursWorked.merge(empId, 8, Integer::sum);

            if (solve(slotIndex + 1, slots, result, assignedPerDay, hoursWorked, employees, workRequests, leaveDays)) {
                return true;
            }

            result.remove(result.size() - 1);
            assignedToday.remove(empId);
            hoursWorked.merge(empId, -8, Integer::sum);
        }
        return false;
    }

    private static class Slot {
        LocalDate date;
        Shift.ShiftType shiftType;

        Slot(LocalDate date, Shift.ShiftType shiftType) {
            this.date = date;
            this.shiftType = shiftType;
        }
    }
}
