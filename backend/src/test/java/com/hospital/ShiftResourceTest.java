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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@QuarkusTest
@TestSecurity(user = "manager", roles = {"MANAGER"})
public class ShiftResourceTest {

    @Inject
    ShiftResource shiftResource;

    @InjectMock
    ShiftRepository shiftRepository;

    @InjectMock
    UserRepository userRepository;

    @InjectMock
    ShiftGenerationService shiftService;

    @InjectMock
    JsonWebToken jwt;

    private User manager;
    private User employee;
    private ShiftResource.CreateShiftDTO createDto;

    @BeforeEach
    public void setup() {
        manager = new User();
        manager.id = 1L;
        manager.username = "manager";
        manager.role = User.Role.MANAGER;
        manager.assigment = User.Assigment.EMERGENCY;

        employee = new User();
        employee.id = 2L;
        employee.username = "employee";
        employee.role = User.Role.EMPLOYEE;
        employee.assigment = User.Assigment.EMERGENCY;

        createDto = new ShiftResource.CreateShiftDTO();
        createDto.userId = 2L;
        createDto.date = LocalDate.of(2026, 6, 10);
        createDto.shiftType = Shift.ShiftType.MORNING;
    }

    @Test
    public void testCreateShift() {
        when(userRepository.findById(2L)).thenReturn(employee);
        when(shiftRepository.count("user = ?1 and date = ?2", employee, createDto.date)).thenReturn(0L);

        Response response = shiftResource.create(createDto);
        assertEquals(201, response.getStatus());
        verify(shiftRepository).persist(any(Shift.class));
    }

    @Test
    public void testCreateShiftUserNotFound() {
        when(userRepository.findById(2L)).thenReturn(null);

        Response response = shiftResource.create(createDto);
        assertEquals(404, response.getStatus());
        verify(shiftRepository, never()).persist(any(Shift.class));
    }

    @Test
    public void testCreateShiftDuplicate() {
        when(userRepository.findById(2L)).thenReturn(employee);
        when(shiftRepository.count("user = ?1 and date = ?2", employee, createDto.date)).thenReturn(1L);

        Response response = shiftResource.create(createDto);
        assertEquals(409, response.getStatus());
        verify(shiftRepository, never()).persist(any(Shift.class));
    }

    @Test
    public void testGenerateShifts() {
        ShiftResource.GenerateRequest req = new ShiftResource.GenerateRequest();
        req.setMonth("2026-06");
        req.setStaffPerShift(3);
        req.setGeneratorName("GreedyGenerator");

        when(jwt.getName()).thenReturn("manager");
        when(userRepository.findByUsername("manager")).thenReturn(manager);

        Response response = shiftResource.generate(req);
        assertEquals(200, response.getStatus());
        verify(shiftService).generateForMonth("2026-06", User.Assigment.EMERGENCY, 3, "GreedyGenerator");
    }

    @Test
    public void testGenerateShiftsNotAssignedManager() {
        manager.assigment = User.Assigment.NOT_ASSIGNED;

        ShiftResource.GenerateRequest req = new ShiftResource.GenerateRequest();
        req.setMonth("2026-06");
        req.setStaffPerShift(3);
        req.setGeneratorName("GreedyGenerator");

        when(jwt.getName()).thenReturn("manager");
        when(userRepository.findByUsername("manager")).thenReturn(manager);

        Response response = shiftResource.generate(req);
        assertEquals(403, response.getStatus());
    }

    @Test
    public void testGenerateShiftsInvalidStaffPerShift() {
        ShiftResource.GenerateRequest req = new ShiftResource.GenerateRequest();
        req.setMonth("2026-06");
        req.setStaffPerShift(5);
        req.setGeneratorName("GreedyGenerator");

        when(jwt.getName()).thenReturn("manager");
        when(userRepository.findByUsername("manager")).thenReturn(manager);

        Response response = shiftResource.generate(req);
        assertEquals(400, response.getStatus());
    }
}