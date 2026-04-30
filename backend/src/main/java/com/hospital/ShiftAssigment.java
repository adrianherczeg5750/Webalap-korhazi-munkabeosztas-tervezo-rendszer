package com.hospital;

import java.time.LocalDate;
import com.hospital.Shift.ShiftType;

public class ShiftAssigment {
    private final Long userId;
    private final LocalDate date;
    private final ShiftType shiftType;

    public ShiftAssigment(Long userId, LocalDate date, ShiftType shiftType) {
        this.userId = userId;
        this.date = date;
        this.shiftType = shiftType;
    }

    public Long getUserId() { return userId; }
    public LocalDate getDate() { return date; }
    public ShiftType getShiftType() { return shiftType; }
}
