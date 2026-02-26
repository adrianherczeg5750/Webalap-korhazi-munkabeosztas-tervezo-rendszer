package com.hospital;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "shifts")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    private ShiftType shiftType;

    @Column(name = "shift_date", nullable = false)
    private LocalDate date;

    public enum ShiftType {
        MORNING,
        AFTERNOON,
        NIGHT
    }

    public static final class ShiftHours {
        private ShiftHours() {}

        public static final int MORNING_START_HOUR = 0;
        public static final int MORNING_END_HOUR = 8;

        public static final int AFTERNOON_START_HOUR = 8;
        public static final int AFTERNOON_END_HOUR = 16;

        public static final int NIGHT_START_HOUR = 16;
        public static final int NIGHT_END_HOUR = 24;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Transient
    public String getWorkDuration() {
        if (shiftType == null) {
            return "00:00";
        }
        return "08:00";
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    public void setType(ShiftType type) {
        this.shiftType = type;
    }

    public void setEmployee(User user) {
        this.user = user;
    }
}