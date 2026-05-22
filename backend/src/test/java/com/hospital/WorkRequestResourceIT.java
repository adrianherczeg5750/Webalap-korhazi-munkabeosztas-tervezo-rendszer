package com.hospital;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class WorkRequestResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    WorkRequestRepository workRequestRepository;

    @Inject
    LeaveRequestRepository leaveRequestRepository;

    private Long employeeId;
    private Long managerId;

    @BeforeEach
    @Transactional
    public void setup() {
        workRequestRepository.deleteAll();
        leaveRequestRepository.deleteAll();
        userRepository.deleteAll();

        User employee = new User();
        employee.username = "testemployee";
        employee.password = "password";
        employee.role = User.Role.EMPLOYEE;
        employee.assigment = User.Assigment.EMERGENCY;
        userRepository.persist(employee);
        employeeId = employee.id;

        User manager = new User();
        manager.username = "testmanager";
        manager.password = "password";
        manager.role = User.Role.MANAGER;
        manager.assigment = User.Assigment.EMERGENCY;
        userRepository.persist(manager);
        managerId = manager.id;
    }

    @Test
    @TestSecurity(user = "testemployee", roles = "EMPLOYEE")
    public void testCreateWorkRequest() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "employeeId": %d,
                    "startDate": "2026-06-01",
                    "endDate": "2026-06-05",
                    "type": "SINGLE",
                    "role": "MORNING"
                }
                """.formatted(employeeId))
        .when()
            .post("/api/work-requests/create-request")
        .then()
            .statusCode(201)
            .body("employeeUsername", equalTo("testemployee"))
            .body("status", equalTo("PENDING"))
            .body("shiftType", equalTo("MORNING"));
    }

    @Test
    @TestSecurity(user = "testemployee", roles = "EMPLOYEE")
    public void testCreateWorkRequestEmployeeNotFound() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "employeeId": 99999,
                    "startDate": "2026-06-01",
                    "endDate": "2026-06-05",
                    "type": "SINGLE",
                    "role": "MORNING"
                }
                """)
        .when()
            .post("/api/work-requests/create-request")
        .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testemployee", roles = "EMPLOYEE")
    public void testCreateWorkRequestInvalidShiftType() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "employeeId": %d,
                    "startDate": "2026-06-01",
                    "endDate": "2026-06-05",
                    "type": "SINGLE",
                    "role": "INVALID"
                }
                """.formatted(employeeId))
        .when()
            .post("/api/work-requests/create-request")
        .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "testmanager", roles = "MANAGER")
    public void testApproveWorkRequest() {
        Long requestId = persistWorkRequest();

        given()
            .contentType("application/json")
            .body("""
                { "managerId": %d }
                """.formatted(managerId))
        .when()
            .post("/api/work-requests/" + requestId + "/approve")
        .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"));
    }

    @Test
    @TestSecurity(user = "testmanager", roles = "MANAGER")
    public void testRejectWorkRequest() {
        Long requestId = persistWorkRequest();

        given()
            .contentType("application/json")
            .body("""
                { "managerId": %d }
                """.formatted(managerId))
        .when()
            .post("/api/work-requests/" + requestId + "/reject")
        .then()
            .statusCode(200)
            .body("status", equalTo("REJECTED"));
    }

    @Test
    @TestSecurity(user = "testmanager", roles = "MANAGER")
    public void testApproveWorkRequestNotFound() {
        given()
            .contentType("application/json")
            .body("""
                { "managerId": %d }
                """.formatted(managerId))
        .when()
            .post("/api/work-requests/99999/approve")
        .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testemployee", roles = {"EMPLOYEE", "MANAGER"})
    public void testListByEmployeeFromManager() {
        persistWorkRequest();

        given()
        .when()
            .get("/api/work-requests/by-employee/" + employeeId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0].employeeUsername", equalTo("testemployee"));
    }

    @Test
    @TestSecurity(user = "testmanager", roles = "MANAGER")
    public void testApproveConflictsWithLeaveRequest() {
        Long requestId = persistWorkRequest();
        persistApprovedLeaveRequest();

        given()
            .contentType("application/json")
            .body("""
                { "managerId": %d }
                """.formatted(managerId))
        .when()
            .post("/api/work-requests/" + requestId + "/approve")
        .then()
            .statusCode(409);
    }

    @Transactional
    Long persistWorkRequest() {
        User employee = userRepository.findById(employeeId);

        WorkRequest wr = new WorkRequest();
        wr.setEmployee(employee);
        wr.setStartDate(java.time.LocalDate.of(2026, 6, 1));
        wr.setEndDate(java.time.LocalDate.of(2026, 6, 5));
        wr.setWorkType(WorkRequest.WorkType.SINGLE);
        wr.setShiftType(Shift.ShiftType.MORNING);
        workRequestRepository.persist(wr);
        return wr.getId();
    }

    @Transactional
    void persistApprovedLeaveRequest() {
        User employee = userRepository.findById(employeeId);
        User manager = userRepository.findById(managerId);

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(employee);
        lr.setStartDate(java.time.LocalDate.of(2026, 6, 2));
        lr.setEndDate(java.time.LocalDate.of(2026, 6, 4));
        lr.setType(LeaveRequest.LeaveType.PAID);
        lr.approve(manager);
        leaveRequestRepository.persist(lr);
    }
}
