package com.hospital;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class LeaveRequestRepository implements PanacheRepository<LeaveRequest> {

    public List<LeaveRequest> findApprovedBetween(LocalDate start, LocalDate end) {

        return list(
            "status = ?1 and startDate <= ?2 and endDate >= ?3",
            LeaveRequest.LeaveStatus.APPROVED,
            end,
            start
        );
    }

    public List<LeaveRequest> findPendingByAssigment(User.Assigment assigment) {
        return list("status = ?1 and employee.assigment = ?2", LeaveRequest.LeaveStatus.PENDING, assigment);
    }

    public List<LeaveRequest> findAllByAssigment(User.Assigment assigment) {
        return list("employee.assigment", assigment);
    }

    public List<LeaveRequest> findApprovedByEmployeeOverlapping(Long employeeId, LocalDate start, LocalDate end) {
        return list("status = ?1 AND employee.id = ?2 AND startDate <= ?3 AND endDate >= ?4",
                LeaveRequest.LeaveStatus.APPROVED, employeeId, end, start);
    }
}
