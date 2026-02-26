package com.hospital;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;

@ApplicationScoped
public class ShiftRepository implements PanacheRepository<Shift> {

    public void deleteByDateBetween(LocalDate start, LocalDate end) {
        delete("date >= ?1 and date <= ?2", start, end);
    }

    public void save(Shift shift) {
        persist(shift);
    }
}