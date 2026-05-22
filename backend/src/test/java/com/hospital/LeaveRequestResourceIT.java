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
public class LeaveRequestResourceIT {

    @Inject
    UserRepository userRepository;

    @Inject
    LeaveRequestRepository leaveRequestRepository;

    @Inject
    WorkRequestRepository workRequestRepository;

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
    public void testCreateLeaveRequest() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "employeeId": %d,
                    "startDate": "2026-03-04",
                    "endDate": "2026-03-09",
                    "type": "UNPAID"
                }
                """.formatted(employeeId))
        .when()
            .post("/api/leave-requests/create-request")
        .then()
            .statusCode(201)
            .body("employeeUsername", equalTo("testemployee"))
            .body("status", equalTo("PENDING"))
            .body("type", equalTo("UNPAID"));
    }

    @Test
    @TestSecurity(user = "testemployee", roles = "EMPLOYEE")
    public void testCreateLeaveRequestEmployeeNotFound() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "employeeId": 99999,
                    "startDate": "2026-03-04",
                    "endDate": "2026-03-09",
                    "type": "UNPAID"
                }
                """)
        .when()
            .post("/api/leave-requests/create-request")
        .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testmanager", roles = "MANAGER")
    public void testApproveLeaveRequest() {
        Long requestId = persistLeaveRequest();

        given()
            .contentType("application/json")
            .body("""
                { "managerId": %d }
                """.formatted(managerId))
        .when()
            .post("/api/leave-requests/" + requestId + "/approve")
        .then()
            .statusCode(200)
            .body("status", equalTo("APPROVED"));
    }

    @Test
    @TestSecurity(user = "testmanager", roles = "MANAGER")
    public void testRejectLeaveRequest() {
        Long requestId = persistLeaveRequest();

        given()
            .contentType("application/json")
            .body("""
                { "managerId": %d }
                """.formatted(managerId))
        .when()
            .post("/api/leave-requests/" + requestId + "/reject")
        .then()
            .statusCode(200)
            .body("status", equalTo("REJECTED"));
    }

    @Test
    @TestSecurity(user = "testmanager", roles = "MANAGER")
    public void testApproveLeaveRequestNotFound() {
        given()
            .contentType("application/json")
            .body("""
                { "managerId": %d }
                """.formatted(managerId))
        .when()
            .post("/api/leave-requests/99999/approve")
        .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "testemployee", roles = {"EMPLOYEE", "MANAGER"})
    public void testListByEmployeeFromManager() {
        persistLeaveRequest();

        given()
        .when()
            .get("/api/leave-requests/by-employee/" + employeeId)
        .then()
            .statusCode(200)
            .body("size()", greaterThanOrEqualTo(1))
            .body("[0].employeeUsername", equalTo("testemployee"));
    }

    @Test
    @TestSecurity(user = "testmanager", roles = "MANAGER")
    public void testApproveConflictsWithWorkRequest() {
        Long requestId = persistLeaveRequest();
        persistApprovedWorkRequest();

        given()
            .contentType("application/json")
            .body("""
                { "managerId": %d }
                """.formatted(managerId))
        .when()
            .post("/api/leave-requests/" + requestId + "/approve")
        .then()
            .statusCode(409);
    }

    @Transactional
    Long persistLeaveRequest() {
        User employee = userRepository.findById(employeeId);

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(employee);
        lr.setStartDate(java.time.LocalDate.of(2026, 3, 4));
        lr.setEndDate(java.time.LocalDate.of(2026, 3, 9));
        lr.setType(LeaveRequest.LeaveType.UNPAID);
        leaveRequestRepository.persist(lr);
        return lr.getId();
    }

    @Transactional
    void persistApprovedWorkRequest() {
        User employee = userRepository.findById(employeeId);
        User manager = userRepository.findById(managerId);

        WorkRequest wr = new WorkRequest();
        wr.setEmployee(employee);
        wr.setStartDate(java.time.LocalDate.of(2026, 3, 5));
        wr.setEndDate(java.time.LocalDate.of(2026, 3, 7));
        wr.setWorkType(WorkRequest.WorkType.SINGLE);
        wr.setShiftType(Shift.ShiftType.MORNING);
        wr.approve(manager);
        workRequestRepository.persist(wr);
    }
}
