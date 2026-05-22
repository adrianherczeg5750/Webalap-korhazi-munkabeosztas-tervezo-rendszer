package com.hospital;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
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
public class LeaveRequestResourceTest {

    @InjectMock
    UserRepository userRepository;

    @Inject
    LeaveRequestResource leaveRequestResource;

    @InjectMock
    LeaveRequestRepository leaveRequestRepository;

    @InjectMock
    WorkRequestRepository workRequestRepository;

    private LeaveRequest testLeaveRequest;
    private User employee;
    private User manager;
    private LeaveRequestResource.CreateLeaveRequestDTO createDto;
    private LeaveRequestResource.DecisionDTO decisionDto;

    @BeforeEach
    public void setup() {
        employee = new User();
        employee.id = 1L;
        employee.username = "employee";
        employee.password = "password";
        employee.role = User.Role.EMPLOYEE;
        employee.assigment = User.Assigment.EMERGENCY;

        manager = new User();
        manager.id = 2L;
        manager.username = "manager";
        manager.password = "password";
        manager.role = User.Role.MANAGER;
        manager.assigment = User.Assigment.EMERGENCY;

        createDto = new LeaveRequestResource.CreateLeaveRequestDTO();
        createDto.employeeId = 1L;
        createDto.startDate = LocalDate.of(2026,3,4);
        createDto.endDate = LocalDate.of(2026,3,9);
        createDto.type = LeaveRequest.LeaveType.UNPAID;

        testLeaveRequest = new LeaveRequest();
        testLeaveRequest.setEmployee(employee);
        testLeaveRequest.setStartDate(LocalDate.of(2026, 3,4));
        testLeaveRequest.setEndDate(LocalDate.of(2026, 3,9));
        testLeaveRequest.setType(LeaveRequest.LeaveType.UNPAID);

        decisionDto = new LeaveRequestResource.DecisionDTO();
        decisionDto.managerId = 2L;

    }

    @Test
    public void testLeaveRequestCreate() {
        when(userRepository.findById(createDto.employeeId)).thenReturn(employee);
        Response response = leaveRequestResource.create(createDto);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        verify(userRepository).findById(createDto.employeeId);
        verify(leaveRequestRepository).persist(any(LeaveRequest.class));
    }

    @Test
    public void testCreateEmployeeNotFound() {
        when(userRepository.findById(createDto.employeeId)).thenReturn(null);
        Response response = leaveRequestResource.create(createDto);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        verify(userRepository).findById(createDto.employeeId);
    }

    @Test
    public void testApprove() {
        when(leaveRequestRepository.findById(1L)).thenReturn(testLeaveRequest);
        when(userRepository.findById(2L)).thenReturn(manager);
        when(workRequestRepository.findApprovedByEmployeeOverlapping(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        Response response = leaveRequestResource.approve(1L, decisionDto);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testApproveNotFound() {
        when(leaveRequestRepository.findById(1L)).thenReturn(null);

        Response response = leaveRequestResource.approve(1L, decisionDto);
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testApproveConflictWithWorkRequest() {
        WorkRequest conflict = new WorkRequest();
        conflict.setEmployee(employee);

        when(leaveRequestRepository.findById(1L)).thenReturn(testLeaveRequest);
        when(userRepository.findById(2L)).thenReturn(manager);
        when(workRequestRepository.findApprovedByEmployeeOverlapping(any(), any(), any()))
                .thenReturn(List.of(conflict));

        Response response = leaveRequestResource.approve(1L, decisionDto);
        assertEquals(409, response.getStatus());
    }

    @Test
    public void testReject() {
        when(leaveRequestRepository.findById(1L)).thenReturn(testLeaveRequest);
        when(userRepository.findById(2L)).thenReturn(manager);

        Response response = leaveRequestResource.reject(1L, decisionDto);
        assertEquals(200, response.getStatus());
    }
}
