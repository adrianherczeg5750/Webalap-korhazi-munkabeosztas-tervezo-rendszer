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

    public List<LeaveRequest> findAllWithEmployee() {
        return list("select lr from LeaveRequest lr join fetch lr.employee order by lr.createdAt desc");
    }
}
