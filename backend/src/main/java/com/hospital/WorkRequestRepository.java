package com.hospital;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class WorkRequestRepository implements PanacheRepository<WorkRequest> {

    public List<WorkRequest> findApprovedBetween(LocalDate start, LocalDate end) {
        return list("status = ?1 AND startDate <= ?2 AND endDate >= ?3",
                WorkRequest.WorkStatus.APPROVED, end, start);
    }

    public List<WorkRequest> findAllByAssigment(User.Assigment assigment) {
        return list("employee.assigment", assigment);
    }

    public List<WorkRequest> findApprovedByEmployeeOverlapping(Long employeeId, LocalDate start, LocalDate end) {
        return list("status = ?1 AND employee.id = ?2 AND startDate <= ?3 AND endDate >= ?4",
                WorkRequest.WorkStatus.APPROVED, employeeId, end, start);
    }
}