package com.hospital;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@Transactional
public class ShiftGenerationService {

    @Inject
    ShiftRepository shiftRepository;

    @Inject
    UserRepository userRepository;

   @Inject
   LeaveRequestRepository leaveRequestRepository;

   @Inject
   WorkRequestRepository workRequestRepository;

    public void generateForMonth(String monthString, User.Assigment assigment, int staffPerShift) {

        YearMonth yearMonth = YearMonth.parse(monthString);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        shiftRepository.deleteByDateBetweenAndAssigment(start, end, assigment);
        List<User> employees = userRepository.findByAssigment(assigment);
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedBetween(start, end);
        List<WorkRequest> approvedWorkRequests = workRequestRepository.findApprovedBetween(start, end);

        Map<Long, Set<LocalDate>> leaveMap = buildLeaveMap(approvedLeaves);
        Map<LocalDate, Map<Long, Shift.ShiftType>> workRequestMap = buildWorkRequestMap(approvedWorkRequests);

        Map<Long, Integer> hoursWorked = new HashMap<>();
        for (User user : employees) {
            hoursWorked.put(user.getId(), 0);
        }

        LocalDate current = start;

        while (!current.isAfter(end)) {

            for (Shift.ShiftType type : Shift.ShiftType.values()) {

                for (int i = 0; i < staffPerShift; i++) {

                    User selected = selectEmployee(employees, leaveMap, workRequestMap, hoursWorked, current, type);

                    if (selected == null) {
                        throw new RuntimeException("Nem generálható beosztás erre a hónapra!");
                    }

                    Shift shift = new Shift();
                    shift.setDate(current);
                    shift.setType(type);
                    shift.setEmployee(selected);

                    shiftRepository.save(shift);

                    hoursWorked.put(selected.getId(), hoursWorked.get(selected.getId()) + 8);
                }
            }

            current = current.plusDays(1);
        }


        validateMinimumHours(employees, hoursWorked);
    }

    private Map<Long, Set<LocalDate>> buildLeaveMap(List<LeaveRequest> approvedLeaves) {
        Map<Long, Set<LocalDate>> leaveMap = new HashMap<>();

        for (LeaveRequest leave : approvedLeaves) {

            Long userId = leave.getEmployee().getId();
            LocalDate current = leave.getStartDate();
            LocalDate end = leave.getEndDate();

            while (!current.isAfter(end)) {
                leaveMap.computeIfAbsent(userId, k -> new java.util.HashSet<>()).add(current);
                current = current.plusDays(1);
            }
        }

        return leaveMap;
    }

    private Map<LocalDate, Map<Long, Shift.ShiftType>> buildWorkRequestMap(List<WorkRequest> approvedWorkRequests) {
        Map<LocalDate, Map<Long, Shift.ShiftType>> map = new HashMap<>();

        for (WorkRequest wr : approvedWorkRequests) {
            Long userId = wr.getEmployee().getId();
            LocalDate current = wr.getStartDate();
            LocalDate end = wr.getEndDate();

            while (!current.isAfter(end)) {
                map.computeIfAbsent(current, k -> new HashMap<>()).put(userId, wr.getShiftType());
                current = current.plusDays(1);
            }
        }

        return map;
    }

    private User selectEmployee(List<User> employees, Map<Long, Set<LocalDate>> leaveMap,
                                Map<LocalDate, Map<Long, Shift.ShiftType>> workRequestMap,
                                Map<Long, Integer> hoursWorked, LocalDate date, Shift.ShiftType shiftType) {
        List<User> candidates = employees.stream()
                .filter(user -> !leaveMap.getOrDefault(user.getId(), Set.of()).contains(date))
                .filter(user -> shiftRepository.count("user.id = ?1 and date = ?2", user.getId(), date) == 0)
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

    private void validateMinimumHours(List<User> employees,Map<Long, Integer> hoursWorked ) {

    }
}
