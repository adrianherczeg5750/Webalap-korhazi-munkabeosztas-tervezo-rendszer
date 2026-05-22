package com.hospital;

import com.hospital.Shift.ShiftType;

import java.time.LocalDate;
import java.util.*;

public class BacktrackingGenerator implements ShiftGenerator {
    @Override
    public List<ShiftAssigment> generate(GeneratorInput input) {
        List<ShiftAssigment> result = new ArrayList<>();

        LocalDate start = input.getStartDate();
        LocalDate end = input.getEndDate();

        List<User> employees = input.getEmployees();
        Map<Long, Set<LocalDate>> leaveMap = input.getLeaveDays();
        Map<LocalDate, Map<Long, ShiftType>> workRequestMap = input.getWorkRequests();

        Map<Long, Integer> hoursWorked = new HashMap<>();
        for (User user : employees) {
            hoursWorked.put(user.getId(), input.getHoursWorked().getOrDefault(user.getId(), 0));
        }
        Map<LocalDate, Set<Long>> assignedPerDay = new HashMap<>();
        
        Map<String, Integer> fixedCounts = new HashMap<>();
        for (ShiftAssigment fixed : input.getFixedAssignments()) {
            String key = fixed.getDate() + "_" + fixed.getShiftType();
            fixedCounts.merge(key, 1, Integer::sum);
            assignedPerDay.computeIfAbsent(fixed.getDate(), k -> new HashSet<>()).add(fixed.getUserId());
            hoursWorked.merge(fixed.getUserId(), 8, Integer::sum);
        }

        Shift.ShiftType[] shiftTypes = { Shift.ShiftType.MORNING, Shift.ShiftType.AFTERNOON, Shift.ShiftType.NIGHT};
        List<Slot> slots = new ArrayList<>();
        LocalDate currentDate = start;
        while (!currentDate.isAfter(end)) {
            for (Shift.ShiftType shiftType : shiftTypes) {
                String slotKey = currentDate + "_" + shiftType;
                int alreadyFilled = fixedCounts.getOrDefault(slotKey, 0);
                int remaining = input.getStaffPerShift() - alreadyFilled;
                for (int i = 0; i < remaining; i++){
                    slots.add(new Slot(currentDate, shiftType));
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        Map<LocalDate, Set<Long>> nightShiftDays = new HashMap<>();
        for (ShiftAssigment fixed : input.getFixedAssignments()) {
            if (fixed.getShiftType() == ShiftType.NIGHT) {
                nightShiftDays.computeIfAbsent(fixed.getDate(), k -> new HashSet<>()).add(fixed.getUserId());
            }
        }

        boolean success = solve(0, slots, result, assignedPerDay, hoursWorked, employees, workRequestMap, leaveMap, nightShiftDays);
        if (!success) {
            throw new RuntimeException("Nem generálható beosztás erre a hónapra!");
        }
        return result;
    }

    private boolean isValid(User emp, LocalDate date, ShiftType shiftType,
                            Set<Long> assignedToday, Map<Long, Set<LocalDate>> leaveDays,
                            Map<LocalDate, Set<Long>> nightShiftDays) {
        Long empId = emp.getId();
        if (assignedToday.contains(empId)) return false;
        Set<LocalDate> leaves = leaveDays.get(empId);
        if (leaves != null && leaves.contains(date)) return false;
        if (shiftType == ShiftType.MORNING) {
            LocalDate prevDay = date.minusDays(1);
            Set<Long> prevNight = nightShiftDays.getOrDefault(prevDay, Set.of());
            if (prevNight.contains(empId)) return false;
        }
        return true;
    }

    private List<User> getCandidates(LocalDate date,
                                     Shift.ShiftType shiftType,
                                     Set<Long> assignedToday,
                                     Map<Long, Set<LocalDate>> leaveDays,
                                     List<User> employees,
                                     Map<Long, Integer> hoursWorked,
                                     Map<LocalDate, Map<Long, Shift.ShiftType>> workRequests,
                                     Map<LocalDate, Set<Long>> nightShiftDays) {

        List<User> valid = new ArrayList<>();
        for (User user : employees) {
            if (isValid(user, date, shiftType, assignedToday, leaveDays, nightShiftDays)) {
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
                          Map<Long, Set<LocalDate>> leaveDays,
                          Map<LocalDate, Set<Long>> nightShiftDays) {
        if (slotIndex == slots.size()) return true;
        if (slotIndex < 0 || slotIndex >= slots.size()) return false;

        Slot slot = slots.get(slotIndex);
        Set<Long> assignedToday = assignedPerDay.computeIfAbsent(slot.date, k -> new HashSet<>());

        List<User> candidates = getCandidates(slot.date, slot.shiftType, assignedToday, leaveDays, employees, hoursWorked, workRequests, nightShiftDays);

        for (User emp : candidates) {
            Long empId = emp.getId();

            result.add(new ShiftAssigment(empId, slot.date, slot.shiftType));
            assignedToday.add(empId);
            assignedPerDay.put(slot.date, assignedToday);
            hoursWorked.merge(empId, 8, Integer::sum);
            if (slot.shiftType == ShiftType.NIGHT) {
                nightShiftDays.computeIfAbsent(slot.date, k -> new HashSet<>()).add(empId);
            }

            if (solve(slotIndex + 1, slots, result, assignedPerDay, hoursWorked, employees, workRequests, leaveDays, nightShiftDays)) {
                return true;
            }

            result.remove(result.size() - 1);
            assignedToday.remove(empId);
            hoursWorked.merge(empId, -8, Integer::sum);
            if (slot.shiftType == ShiftType.NIGHT) {
                nightShiftDays.getOrDefault(slot.date, new HashSet<>()).remove(empId);
            }
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
