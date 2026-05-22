package com.hospital;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class ShiftRepository implements PanacheRepository<Shift> {

    public void deleteByDateBetween(LocalDate start, LocalDate end) {
        delete("date >= ?1 and date <= ?2", start, end);
    }

    public void deleteByDateBetweenAndAssigment(LocalDate start, LocalDate end, User.Assigment assigment) {
        delete("date >= ?1 and date <= ?2 and user.assigment = ?3", start, end, assigment);
    }

    public List<Shift> findByEmployeeAssigment(User.Assigment assigment) {
        return list("user.assigment", assigment);
    }

    @SuppressWarnings("unchecked")
    public List<String> findDistinctMonths() {
        return getEntityManager()
                .createNativeQuery("SELECT DISTINCT TO_CHAR(shift_date, 'YYYY-MM') FROM shifts ORDER BY 1")
                .getResultList();
    }

    public void save(Shift shift) {
        persist(shift);
    }

    public List<Shift> findByDateBetween(LocalDate start, LocalDate end) {
        return list("date >= ?1 and date <= ?2", start, end);
    }
}