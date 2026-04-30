package com.hospital;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

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

    public void generateForMonth(String monthString, User.Assigment assigment, int staffPerShift, String generatorName) {
        ShiftGenerator generator = loadGenerator(generatorName);

        YearMonth yearMonth = YearMonth.parse(monthString);
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();

        shiftRepository.deleteByDateBetweenAndAssigment(start, end, assigment);

        List<User> employees = userRepository.findByAssigment(assigment);
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findApprovedBetween(start, end);
        List<WorkRequest> approvedWorkRequests = workRequestRepository.findApprovedBetween(start, end);

        Map<Long, Set<LocalDate>> leaveMap = buildLeaveMap(approvedLeaves);
        Map<LocalDate, Map<Long, Shift.ShiftType>> workRequestMap = buildWorkRequestMap(approvedWorkRequests);

        GeneratorInput input = new GeneratorInput(monthString, staffPerShift, employees, leaveMap, workRequestMap);
        List<ShiftAssigment> assignments = generator.generate(input);

        for (ShiftAssigment a : assignments) {
            User user = userRepository.findById(a.getUserId());
            Shift shift = new Shift();
            shift.setDate(a.getDate());
            shift.setType(a.getShiftType());
            shift.setEmployee(user);
            shiftRepository.save(shift);
        }
    }

    private static final Path GENERATORS_DIR = Paths.get("src/main/resources/generators");

    private ShiftGenerator loadGenerator(String generatorName) {
        Path pyFile = GENERATORS_DIR.resolve(generatorName + ".py");

        if (Files.exists(pyFile)) {
            return loadPythonGenerator(generatorName, pyFile);
        }
        
        try {
            Class<?> clazz = Class.forName("com.hospital." + generatorName);

            if (!ShiftGenerator.class.isAssignableFrom(clazz)) {
                throw new RuntimeException(generatorName + " nem implementálja a ShiftGenerator interfészt.");
            }

            return (ShiftGenerator) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Generátor nem található: " + generatorName);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Generátor betöltési hiba: " + e.getMessage(), e);
        }
    }

    private ShiftGenerator loadPythonGenerator(String generatorName, Path pyFile) {
        return (GeneratorInput input) -> {
            try {
                ObjectMapper mapper = new ObjectMapper();

                List<Map<String, Object>> employeeList = new ArrayList<>();
                for (User emp : input.getEmployees()) {
                    Map<String, Object> e = new HashMap<>();
                    e.put("id", emp.getId());
                    employeeList.add(e);
                }

                Map<String, List<String>> leaveDaysStr = new HashMap<>();
                for (Map.Entry<Long, Set<LocalDate>> entry : input.getLeaveDays().entrySet()) {
                    List<String> dates = new ArrayList<>();
                    for (LocalDate d : entry.getValue()) {
                        dates.add(d.toString());
                    }
                    leaveDaysStr.put(entry.getKey().toString(), dates);
                }

                Map<String, Map<String, String>> workRequestsStr = new HashMap<>();
                for (Map.Entry<LocalDate, Map<Long, Shift.ShiftType>> entry : input.getWorkRequests().entrySet()) {
                    Map<String, String> inner = new HashMap<>();
                    for (Map.Entry<Long, Shift.ShiftType> wr : entry.getValue().entrySet()) {
                        inner.put(wr.getKey().toString(), wr.getValue().name());
                    }
                    workRequestsStr.put(entry.getKey().toString(), inner);
                }

                Map<String, Object> jsonInput = new HashMap<>();
                jsonInput.put("month", input.getMonth());
                jsonInput.put("staffPerShift", input.getStaffPerShift());
                jsonInput.put("employees", employeeList);
                jsonInput.put("leaveDays", leaveDaysStr);
                jsonInput.put("workRequests", workRequestsStr);

                String inputJson = mapper.writeValueAsString(jsonInput);

                Process process = new ProcessBuilder("python3", pyFile.toAbsolutePath().toString()).start();

                process.getOutputStream().write(inputJson.getBytes());
                process.getOutputStream().close();

                String outputJson = new String(process.getInputStream().readAllBytes());
                String errorOutput = new String(process.getErrorStream().readAllBytes());

                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Python generátor hiba (" + generatorName + "):\n" + errorOutput);
                }

                List<Map<String, Object>> rawList = mapper.readValue(outputJson,
                        mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

                List<ShiftAssigment> result = new ArrayList<>();
                for (Map<String, Object> item : rawList) {
                    Long userId = Long.valueOf(item.get("userId").toString());
                    LocalDate date = LocalDate.parse(item.get("date").toString());
                    Shift.ShiftType shiftType = Shift.ShiftType.valueOf(item.get("shiftType").toString());
                    result.add(new ShiftAssigment(userId, date, shiftType));
                }
                return result;

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Python generátor futtatási hiba: " + e.getMessage(), e);
            }
        };
    }

    private Map<Long, Set<LocalDate>> buildLeaveMap(List<LeaveRequest> approvedLeaves) {
        Map<Long, Set<LocalDate>> leaveMap = new HashMap<>();
        for (LeaveRequest leave : approvedLeaves) {
            Long userId = leave.getEmployee().getId();
            LocalDate current = leave.getStartDate();
            LocalDate end = leave.getEndDate();
            while (!current.isAfter(end)) {
                leaveMap.computeIfAbsent(userId, k -> new HashSet<>()).add(current);
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
}