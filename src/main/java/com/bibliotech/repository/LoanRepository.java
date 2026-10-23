package com.bibliotech.repository;

import com.bibliotech.model.Loan;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class LoanRepository {

    private final Map<Long, Loan> loans = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public Loan save(Loan loan) {
        if (loan.getId() == null) {
            loan.setId(sequence.incrementAndGet());
        }
        loans.put(loan.getId(), loan);
        return loan;
    }

    public List<Loan> findActiveByStudent(Long studentId) {
        return loans.values().stream()
                .filter(l -> l.getStudentId().equals(studentId) && !l.isReturned())
                .toList();
    }

    public long countActiveByStudent(Long studentId) {
        return findActiveByStudent(studentId).size();
    }

    /** Préstamos sin devolver cuya fecha de vencimiento ya pasó (vencen antes de {@code today}). */
    public List<Loan> findOverdueByStudent(Long studentId, LocalDate today) {
        return findActiveByStudent(studentId).stream()
                .filter(l -> l.getDueDate().isAfter(today))
                .toList();
    }
}
