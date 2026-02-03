package com.hospital;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private String role;

    @Column(name = "start_at_date", nullable = false)
    private LocalDate startAtDate;

    @Column(name = "start_at_time", nullable = false)
    private LocalTime startAtTime;

    @Column(name = "end_at_date", nullable = false)
    private LocalDate endAtDate;

    @Column(name = "end_at_time", nullable = false)
    private LocalTime endAtTime;

    @Transient
    private String workDuration;

    public Long getId() {
        return id;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDate getStartAtDate() {
        return startAtDate;
    }

    public void setStartAtDate(LocalDate startAtDate) {
        this.startAtDate = startAtDate;
    }

    public LocalTime getStartAtTime() {
        return startAtTime;
    }

    public void setStartAtTime(LocalTime startAtTime) {
        this.startAtTime = startAtTime;
    }

    public LocalDate getEndAtDate() {
        return endAtDate;
    }

    public void setEndAtDate(LocalDate endAtDate) {
        this.endAtDate = endAtDate;
    }

    public LocalTime getEndAtTime() {
        return endAtTime;
    }

    public void setEndAtTime(LocalTime endAtTime) {
        this.endAtTime = endAtTime;
    }

    public String getWorkDuration() {
        if (startAtDate == null || startAtTime == null || endAtDate == null || endAtTime == null) {
            return "00:00";
        }

        LocalDateTime start = LocalDateTime.of(startAtDate, startAtTime);
        LocalDateTime end = LocalDateTime.of(endAtDate, endAtTime);

        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();

        return String.format("%02d:%02d", hours, minutes);
    }
    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}