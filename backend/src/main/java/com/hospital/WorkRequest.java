package com.hospital;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "work_requests")
public class WorkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_user_id", nullable = false)
    private User employee;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_type", nullable = false)
    private WorkType workType;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private Shift.ShiftType shiftType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkStatus status = WorkStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;

    public enum WorkType {
        SINGLE,
        MULTIPLE
    }

    public enum WorkStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = WorkStatus.PENDING;
        }
    }

    public boolean isPending() {
        return status == WorkStatus.PENDING;
    }

    public void approve(User manager) {
        this.status = WorkStatus.APPROVED;
        this.decidedBy = manager;
        this.decidedAt = Instant.now();
    }

    public void reject(User manager) {
        this.status = WorkStatus.REJECTED;
        this.decidedBy = manager;
        this.decidedAt = Instant.now();
    }

    public Long getId() { return id; }

    public User getEmployee() { return employee; }
    public void setEmployee(User employee) { this.employee = employee; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public WorkType getWorkType() { return workType; }
    public void setWorkType(WorkType workType) { this.workType = workType; }

    public Shift.ShiftType getShiftType() { return shiftType; }
    public void setShiftType(Shift.ShiftType shiftType) { this.shiftType = shiftType; }

    public WorkStatus getStatus() { return status; }
    public void setStatus(WorkStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }

    public User getDecidedBy() { return decidedBy; }
    public void setDecidedBy(User decidedBy) { this.decidedBy = decidedBy; }

}