package com.hospital;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_user_id", nullable = false)
    private User employee;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;


    @ElementCollection
    @CollectionTable(
            name = "leave_request_dates",
            joinColumns = @JoinColumn(name = "leave_request_id")
    )
    @Column(name = "leave_date", nullable = false)
    private Set<LocalDate> dates = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private LeaveType type = LeaveType.PAID;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    /** Manager (or higher role) who decided this request */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id")
    private User decidedBy;

    @Column(name = "manager_note", length = 1000)
    private String managerNote;

    public enum LeaveType {
        PAID,
        UNPAID
    }

    public enum LeaveStatus {
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
            status = LeaveStatus.PENDING;
        }
        if (type == null) {
            type = LeaveType.PAID;
        }
    }

    public boolean isPending() {
        return status == LeaveStatus.PENDING;
    }

    public void approve(User manager, String note) {
        this.status = LeaveStatus.APPROVED;
        this.decidedBy = manager;
        this.decidedAt = Instant.now();
        this.managerNote = note;
    }

    public void reject(User manager, String note) {
        this.status = LeaveStatus.REJECTED;
        this.decidedBy = manager;
        this.decidedAt = Instant.now();
        this.managerNote = note;
    }

    public Long getId() {
        return id;
    }

    public User getEmployee() {
        return employee;
    }

    public void setEmployee(User employee) {
        this.employee = employee;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Set<LocalDate> getDates() {
        return dates;
    }

    public void setDates(Set<LocalDate> dates) {
        this.dates = (dates == null) ? new HashSet<>() : dates;
    }

    public LeaveType getType() {
        return type;
    }

    public void setType(LeaveType type) {
        this.type = type;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(User decidedBy) {
        this.decidedBy = decidedBy;
    }

    public String getManagerNote() {
        return managerNote;
    }

    public void setManagerNote(String managerNote) {
        this.managerNote = managerNote;
    }
}
