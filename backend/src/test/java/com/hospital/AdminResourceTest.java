package com.hospital;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestSecurity(user = "admin", roles = {"ADMIN"})
public class AdminResourceTest {

    @Inject
    AdminResource adminResource;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    ShiftRepository shiftRepository;

    @InjectMock
    LeaveRequestRepository leaveRequestRepository;

    @InjectMock
    WorkRequestRepository workRequestRepository;

    private User testUser;

    @BeforeEach
    public void setup() {
        testUser = new User();
        testUser.id = 1L;
        testUser.username = "employee1";
        testUser.role = User.Role.EMPLOYEE;
        testUser.assigment = User.Assigment.EMERGENCY;
    }

    @Test
    public void testListUsers() {
        when(userRepository.listAll()).thenReturn(List.of(testUser));

        List<AdminResource.UserDto> users = adminResource.listUsers();
        assertEquals(1, users.size());
        assertEquals("employee1", users.get(0).username);
    }

    @Test
    public void testChangeRole() {
        AdminResource.ChangeRoleDto dto = new AdminResource.ChangeRoleDto();
        dto.role = "MANAGER";

        when(userRepository.findById(1L)).thenReturn(testUser);

        Response response = adminResource.changeRole(1L, dto);
        assertEquals(200, response.getStatus());
        assertEquals(User.Role.MANAGER, testUser.role);
    }

    @Test
    public void testChangeRoleToAdminResetsAssignment() {
        AdminResource.ChangeRoleDto dto = new AdminResource.ChangeRoleDto();
        dto.role = "ADMIN";

        when(userRepository.findById(1L)).thenReturn(testUser);

        Response response = adminResource.changeRole(1L, dto);
        assertEquals(200, response.getStatus());
        assertEquals(User.Role.ADMIN, testUser.role);
        assertEquals(User.Assigment.NOT_ASSIGNED, testUser.assigment);
    }

    @Test
    public void testChangeRoleInvalid() {
        AdminResource.ChangeRoleDto dto = new AdminResource.ChangeRoleDto();
        dto.role = "INVALID";

        when(userRepository.findById(1L)).thenReturn(testUser);

        Response response = adminResource.changeRole(1L, dto);
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(testUser);
        when(shiftRepository.count("user.id = ?1", 1L)).thenReturn(0L);
        when(leaveRequestRepository.list("employee.id", 1L)).thenReturn(Collections.emptyList());

        Response response = adminResource.deleteUser(1L);
        assertEquals(204, response.getStatus());
        verify(userRepository).delete(testUser);
    }

    @Test
    public void testDeleteUserWithShifts() {
        when(userRepository.findById(1L)).thenReturn(testUser);
        when(shiftRepository.count("user.id = ?1", 1L)).thenReturn(3L);

        Response response = adminResource.deleteUser(1L);
        assertEquals(409, response.getStatus());
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    public void testDeleteUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(null);

        Response response = adminResource.deleteUser(1L);
        assertEquals(404, response.getStatus());
    }
}