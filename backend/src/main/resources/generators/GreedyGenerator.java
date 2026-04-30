package com.hospital;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class GreedyGenerator implements ShiftGenerator {

    @Override
    public List<ShiftAssigment> generate(GeneratorInput input) {
        List<ShiftAssigment> result = new ArrayList<>();

        YearMonth yearMonth = YearMonth.parse(input.getMonth());
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        List<User> employees = input.getEmployees();
        Map<Long, Set<LocalDate>> leaveMap = input.getLeaveDays();
        Map<LocalDate, Map<Long, Shift.ShiftType>> workRequestMap = input.getWorkRequests();

        Map<Long, Integer> hoursWorked = new HashMap<>();
        for (User user : employees) {
            hoursWorked.put(user.getId(), 0);
        }

        Set<String> assignedToday = new HashSet<>();

        LocalDate current = start;
        while (!current.isAfter(end)) {
            assignedToday.clear();

            for (Shift.ShiftType type : Shift.ShiftType.values()) {
                for (int i = 0; i < input.getStaffPerShift(); i++) {

                    User selected = selectEmployee(employees, leaveMap, workRequestMap,
                            hoursWorked, assignedToday, current, type);

                    if (selected == null) {
                        throw new RuntimeException("Nem generálható beosztás erre a hónapra!");
                    }

                    result.add(new ShiftAssigment(selected.getId(), current, type));
                    assignedToday.add(selected.getId() + "_" + current);
                    hoursWorked.put(selected.getId(), hoursWorked.get(selected.getId()) + 8);
                }
            }

            current = current.plusDays(1);
        }

        return result;
    }

    private User selectEmployee(List<User> employees, Map<Long, Set<LocalDate>> leaveMap,
                                Map<LocalDate, Map<Long, Shift.ShiftType>> workRequestMap,
                                Map<Long, Integer> hoursWorked, Set<String> assignedToday,
                                LocalDate date, Shift.ShiftType shiftType) {

        List<User> candidates = employees.stream()
                .filter(user -> !leaveMap.getOrDefault(user.getId(), Set.of()).contains(date))
                .filter(user -> !assignedToday.contains(user.getId() + "_" + date))
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