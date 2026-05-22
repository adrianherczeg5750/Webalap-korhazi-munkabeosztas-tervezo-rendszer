package com.hospital;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class GreedyGenerator implements ShiftGenerator {

    @Override
    public List<ShiftAssigment> generate(GeneratorInput input) {
        List<ShiftAssigment> result = new ArrayList<>();

        LocalDate start = input.getStartDate();
        LocalDate end = input.getEndDate();

        List<User> employees = input.getEmployees();
        Map<Long, Set<LocalDate>> leaveMap = input.getLeaveDays();
        Map<LocalDate, Map<Long, Shift.ShiftType>> workRequestMap = input.getWorkRequests();

        Map<Long, Integer> hoursWorked = new HashMap<>();
        for (User user : employees) {
            hoursWorked.put(user.getId(), input.getHoursWorked().getOrDefault(user.getId(), 0));
        }
        
        Map<String, Integer> fixedCounts = new HashMap<>();
        Set<String> fixedSlots = new HashSet<>();
        for (ShiftAssigment fixed : input.getFixedAssignments()) {
            String key = fixed.getDate() + "_" + fixed.getShiftType();
            fixedCounts.merge(key, 1, Integer::sum);
            fixedSlots.add(fixed.getUserId() + "_" + fixed.getDate());
            hoursWorked.merge(fixed.getUserId(), 8, Integer::sum);
        }

        Set<String> assignedToday = new HashSet<>();
        Set<Long> nightShiftPrevDay = new HashSet<>();

        for (ShiftAssigment fixed : input.getFixedAssignments()) {
            if (fixed.getShiftType() == Shift.ShiftType.NIGHT) {
                nightShiftPrevDay.add(fixed.getUserId());
            }
        }

        LocalDate current = start;
        while (!current.isAfter(end)) {
            assignedToday.clear();
            Set<Long> nightShiftToday = new HashSet<>();

            for (ShiftAssigment fixed : input.getFixedAssignments()) {
                if (fixed.getDate().equals(current)) {
                    assignedToday.add(fixed.getUserId() + "_" + current);
                    if (fixed.getShiftType() == Shift.ShiftType.NIGHT) {
                        nightShiftToday.add(fixed.getUserId());
                    }
                }
            }

            for (Shift.ShiftType type : Shift.ShiftType.values()) {
                String slotKey = current + "_" + type;
                int alreadyFilled = fixedCounts.getOrDefault(slotKey, 0);
                int remaining = input.getStaffPerShift() - alreadyFilled;

                for (int i = 0; i < remaining; i++) {

                    User selected = selectEmployee(employees, leaveMap, workRequestMap,
                            hoursWorked, assignedToday, current, type, nightShiftPrevDay);

                    if (selected == null) {
                        throw new RuntimeException("Nem generálható beosztás erre a hónapra!");
                    }

                    result.add(new ShiftAssigment(selected.getId(), current, type));
                    assignedToday.add(selected.getId() + "_" + current);
                    hoursWorked.put(selected.getId(), hoursWorked.get(selected.getId()) + 8);
                    if (type == Shift.ShiftType.NIGHT) {
                        nightShiftToday.add(selected.getId());
                    }
                }
            }

            nightShiftPrevDay = nightShiftToday;
            current = current.plusDays(1);
        }

        return result;
    }

    private User selectEmployee(List<User> employees, Map<Long, Set<LocalDate>> leaveMap,
                                Map<LocalDate, Map<Long, Shift.ShiftType>> workRequestMap,
                                Map<Long, Integer> hoursWorked, Set<String> assignedToday,
                                LocalDate date, Shift.ShiftType shiftType, Set<Long> nightShiftPrevDay) {

        List<User> candidates = employees.stream()
                .filter(user -> !leaveMap.getOrDefault(user.getId(), Set.of()).contains(date))
                .filter(user -> !assignedToday.contains(user.getId() + "_" + date))
                .filter(user -> !(shiftType == Shift.ShiftType.MORNING && nightShiftPrevDay.contains(user.getId())))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return null;
        }

        Map<Long, Shift.ShiftType> requestsForDay = workRequestMap.getOrDefault(date, Map.of());
        List<User> prioritized = candidates.stream()
                .filter(user -> requestsForDay.getOrDefault(user.getId(), null) == shiftType)
                .collect(Collectors.toList());

        List<User> pool = prioritized.isEmpty() ? candidates : prioritized;

        int minHours = pool.stream()
                .mapToInt(user -> hoursWorked.get(user.getId()))
                .min()
                .orElse(Integer.MAX_VALUE);

        List<User> leastWorkedCandidates = pool.stream()
                .filter(user -> hoursWorked.get(user.getId()) == minHours)
                .collect(Collectors.toList());

        Collections.shuffle(leastWorkedCandidates);
        return leastWorkedCandidates.get(0);
    }
}