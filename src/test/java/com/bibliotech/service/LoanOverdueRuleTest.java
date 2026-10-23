package com.bibliotech.service;

import com.bibliotech.model.Book;
import com.bibliotech.model.Loan;
import com.bibliotech.model.Student;
import com.bibliotech.repository.BookRepository;
import com.bibliotech.repository.LoanRepository;
import com.bibliotech.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Funcionalidad 4: un estudiante con préstamos vencidos no puede solicitar otro préstamo. */
class LoanOverdueRuleTest {

    private static final Long STUDENT_ID = 1L;
    private static final Long BOOK_ID = 10L;

    private BookRepository books;
    private LoanRepository loans;
    private LoanService service;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        StudentRepository students = new StudentRepository();
        books = new BookRepository();
        loans = new LoanRepository();
        service = new LoanService(students, books, loans);
        today = LocalDate.now();

        students.save(new Student(STUDENT_ID, "Ana Torres"));
        books.save(new Book(BOOK_ID, "Bases de Datos", 5));
    }

    /** Registra un préstamo del estudiante que vence en la fecha indicada. */
    private Loan prestamoConVencimiento(LocalDate dueDate) {
        return loans.save(new Loan(STUDENT_ID, BOOK_ID, dueDate.minusDays(14), dueDate));
    }

    @Test
    void sinPrestamosVencidos_puedeSolicitarOtroPrestamo() {
        prestamoConVencimiento(today.plusDays(3));

        Loan nuevo = service.requestLoan(STUDENT_ID, BOOK_ID);

        assertNotNull(nuevo.getId());
        assertEquals(2, loans.countActiveByStudent(STUDENT_ID));
    }

    @Test
    void conPrestamoVencido_esRechazadoYNoSeAlteraNada() {
        prestamoConVencimiento(today.minusDays(2));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.requestLoan(STUDENT_ID, BOOK_ID));

        assertTrue(error.getMessage().contains("vencidos"));
        assertEquals(1, loans.countActiveByStudent(STUDENT_ID));
        assertEquals(5, books.findById(BOOK_ID).orElseThrow().getAvailableCopies());
    }

    @Test
    void prestamoQueVenceHoy_noSeConsideraVencido() {
        prestamoConVencimiento(today);

        Loan nuevo = service.requestLoan(STUDENT_ID, BOOK_ID);

        assertNotNull(nuevo.getId());
    }

    @Test
    void prestamoVencidoYaDevuelto_noBloqueaNuevosPrestamos() {
        Loan vencido = prestamoConVencimiento(today.minusDays(10));
        vencido.markReturned();

        Loan nuevo = service.requestLoan(STUDENT_ID, BOOK_ID);

        assertNotNull(nuevo.getId());
    }
}
