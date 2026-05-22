package com.hospital;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestSecurity(user = "employee", roles = {"EMPLOYEE", "MANAGER"})
public class WorkRequestResourceTest {

    @Inject
    WorkRequestResource workRequestResource;

    @InjectMock
    WorkRequestRepository workRequestRepository;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    LeaveRequestRepository leaveRequestRepository;

    @InjectMock
    JsonWebToken jwt;

    private User employee;
    private User manager;
    private WorkRequest workRequest;
    private WorkRequestResource.CreateWorkRequestDTO createDto;
    private WorkRequestResource.DecisionDTO decisionDto;

    @BeforeEach
    public void setup() {
        employee = new User();
        employee.id = 1L;
        employee.username = "employee";
        employee.role = User.Role.EMPLOYEE;
        employee.assigment = User.Assigment.EMERGENCY;

        manager = new User();
        manager.id = 2L;
        manager.username = "manager";
        manager.role = User.Role.MANAGER;
        manager.assigment = User.Assigment.EMERGENCY;

        createDto = new WorkRequestResource.CreateWorkRequestDTO();
        createDto.employeeId = 1L;
        createDto.startDate = LocalDate.of(2026, 6, 1);
        createDto.endDate = LocalDate.of(2026, 6, 5);
        createDto.type = WorkRequest.WorkType.SINGLE;
        createDto.role = "MORNING";

        workRequest = new WorkRequest();
        workRequest.setEmployee(employee);
        workRequest.setStartDate(LocalDate.of(2026, 6, 1));
        workRequest.setEndDate(LocalDate.of(2026, 6, 5));
        workRequest.setWorkType(WorkRequest.WorkType.SINGLE);
        workRequest.setShiftType(Shift.ShiftType.MORNING);

        decisionDto = new WorkRequestResource.DecisionDTO();
        decisionDto.managerId = 2L;

    }

    @Test
    public void testCreateWorkRequest() {
        when(userRepository.findById(1L)).thenReturn(employee);
        Response response = workRequestResource.create(createDto);
        assertEquals(201, response.getStatus());
        verify(workRequestRepository).persist(any(WorkRequest.class));
    }

    @Test
    public void testCreateWorkRequestEmployeeNotFound() {
        when(userRepository.findById(1L)).thenReturn(null);
        Response response = workRequestResource.create(createDto);
        assertEquals(404, response.getStatus());
        verify(workRequestRepository, never()).persist(any(WorkRequest.class));
    }

    @Test
    public void testCreateWorkRequestInvalidShiftType() {
        createDto.role = "INVALID";
        when(userRepository.findById(1L)).thenReturn(employee);
        Response response = workRequestResource.create(createDto);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testApproveWorkRequest() {
        when(workRequestRepository.findById(1L)).thenReturn(workRequest);
        when(userRepository.findById(2L)).thenReturn(manager);
        when(leaveRequestRepository.findApprovedByEmployeeOverlapping(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());

        Response response = workRequestResource.approve(1L, decisionDto);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testApproveWorkRequestConflictWithLeave() {
        LeaveRequest conflictingLeave = new LeaveRequest();
        conflictingLeave.setEmployee(employee);

        when(workRequestRepository.findById(1L)).thenReturn(workRequest);
        when(userRepository.findById(2L)).thenReturn(manager);
        when(leaveRequestRepository.findApprovedByEmployeeOverlapping(eq(1L), any(), any()))
                .thenReturn(List.of(conflictingLeave));

        Response response = workRequestResource.approve(1L, decisionDto);
        assertEquals(409, response.getStatus());
    }

    @Test
    public void testRejectWorkRequest() {
        when(workRequestRepository.findById(1L)).thenReturn(workRequest);
        when(userRepository.findById(2L)).thenReturn(manager);

        Response response = workRequestResource.reject(1L, decisionDto);
        assertEquals(200, response.getStatus());
    }
}