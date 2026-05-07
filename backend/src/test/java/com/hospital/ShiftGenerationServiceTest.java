package com.hospital;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
public class ShiftGenerationServiceTest {

    @Inject
    ShiftGenerationService shiftGenerationService;

    @InjectMock
    ShiftRepository shiftRepository;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    LeaveRequestRepository leaveRequestRepository;

    @InjectMock
    WorkRequestRepository workRequestRepository;

    private List<User> employees;

    @BeforeEach
    public void setup() {
        employees = new ArrayList<>();
        for (long i = 1; i <= 8; i++) {
            User emp = new User();
            emp.id = i;
            emp.username = "emp" + i;
            emp.role = User.Role.EMPLOYEE;
            emp.assigment = User.Assigment.EMERGENCY;
            employees.add(emp);
            when(userRepository.findById(i)).thenReturn(emp);
        }
    }

    @Test
    public void testGenerateForMonth() {
        when(userRepository.findByAssigment(User.Assigment.EMERGENCY))
                .thenReturn(employees);
        when(leaveRequestRepository.findApprovedBetween(any(), any()))
                .thenReturn(Collections.emptyList());
        when(workRequestRepository.findApprovedBetween(any(), any()))
                .thenReturn(Collections.emptyList());

        shiftGenerationService.generateForMonth("2026-06", User.Assigment.EMERGENCY, 2, "GreedyGenerator");

        verify(shiftRepository).deleteByDateBetweenAndAssigment(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), User.Assigment.EMERGENCY);
        verify(shiftRepository, atLeastOnce()).save(any(Shift.class));
    }

    @Test
    public void testGenerateForMonthWithLeaves() {
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employees.get(0));
        leave.setStartDate(LocalDate.of(2026, 6, 10));
        leave.setEndDate(LocalDate.of(2026, 6, 15));

        when(userRepository.findByAssigment(User.Assigment.EMERGENCY))
                .thenReturn(employees);
        when(leaveRequestRepository.findApprovedBetween(any(), any()))
                .thenReturn(List.of(leave));
        when(workRequestRepository.findApprovedBetween(any(), any()))
                .thenReturn(Collections.emptyList());

        shiftGenerationService.generateForMonth("2026-06", User.Assigment.EMERGENCY, 2, "GreedyGenerator");

        verify(shiftRepository, atLeastOnce()).save(any(Shift.class));
    }
}