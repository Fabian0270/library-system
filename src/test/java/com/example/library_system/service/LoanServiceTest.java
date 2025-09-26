package com.example.library_system.service;

import com.example.library_system.entity.Book;
import com.example.library_system.entity.Loan;
import com.example.library_system.dto.UserDTO;
import com.example.library_system.repository.BookRepository;
import com.example.library_system.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookService bookService;

    @Mock
    private UserService userService;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private LoanService loanService;

    private Book testBook;
    private UserDTO testUser;
    private Long userId = 1L;
    private Long bookId = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Skapa testbok
        testBook = new Book();
        testBook.setBookId(bookId);
        testBook.setTitle("Test Book");
        testBook.setAvailableCopies(2);
        testBook.setTotalCopies(3);

        // Skapa testanvändare
        testUser = new UserDTO();
        testUser.setUserId(userId);
        testUser.setEmail("test@test.com");
    }

    @Test
    @DisplayName("KRAV TEST: Kontrollera att rätt datum sätts på dueDate när man lägger ett lån")
    void testCreateLoan_ShouldSetCorrectDueDate() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expectedDueDate = today.plusDays(14); // 14 dagar enligt kravspec

        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));
        when(bookService.isBookAvailable(bookId)).thenReturn(true);
        when(bookService.decreaseAvailableCopies(bookId)).thenReturn(true);
        when(loanRepository.findActiveLoadByUserIdAndBookId(userId, bookId)).thenReturn(Optional.empty());

        // Mocka save för att returnera lånet med ID
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setLoanId(1L);
            return savedLoan;
        });

        // Act
        Loan createdLoan = loanService.createLoan(userId, bookId);

        // Assert
        assertNotNull(createdLoan, "Lånet ska skapas");
        assertEquals(today, createdLoan.getBorrowedDate(),
                "Lånade datum ska vara dagens datum");
        assertEquals(expectedDueDate, createdLoan.getDueDate(),
                "Återlämningsdatum ska vara exakt 14 dagar från idag");

        // Verifiera att dueDate är EXAKT 14 dagar senare
        long daysBetween = createdLoan.getDueDate().toEpochDay() -
                createdLoan.getBorrowedDate().toEpochDay();
        assertEquals(14, daysBetween, "Det ska vara exakt 14 dagar mellan lån och återlämning");

        System.out.println("✅ KRAV UPPFYLLT: DueDate sätts korrekt till +14 dagar");
        System.out.println("   Lånat: " + createdLoan.getBorrowedDate());
        System.out.println("   Återlämning: " + createdLoan.getDueDate());
    }

    @Test
    @DisplayName("KRAV TEST: Man kan inte lägga ett lån om boken har 0 available copies")
    void testCreateLoan_ShouldFailWhenNoAvailableCopies() {
        // Arrange
        testBook.setAvailableCopies(0); // Sätt till 0 tillgängliga kopior

        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));
        when(bookService.isBookAvailable(bookId)).thenReturn(false); // Ingen kopia tillgänglig

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.createLoan(userId, bookId),
                "Ska kasta IllegalArgumentException när inga kopior är tillgängliga"
        );

        // Verifiera felmeddelandet
        assertEquals("Book is not available", exception.getMessage(),
                "Felmeddelandet ska indikera att boken inte är tillgänglig");

        // Verifiera att inget lån skapades
        verify(loanRepository, never()).save(any(Loan.class));

        // Verifiera att available copies INTE minskades
        verify(bookService, never()).decreaseAvailableCopies(anyLong());

        System.out.println("✅ KRAV UPPFYLLT: Lån blockeras när available_copies = 0");
        System.out.println("   Exception message: " + exception.getMessage());
    }

    @Test
    @DisplayName("Extra test: Kontrollera att available_copies minskar vid lån")
    void testCreateLoan_ShouldDecreaseAvailableCopies() {
        // Arrange
        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));
        when(bookService.isBookAvailable(bookId)).thenReturn(true);
        when(bookService.decreaseAvailableCopies(bookId)).thenReturn(true);
        when(loanRepository.findActiveLoadByUserIdAndBookId(userId, bookId)).thenReturn(Optional.empty());
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan savedLoan = invocation.getArgument(0);
            savedLoan.setLoanId(1L);
            return savedLoan;
        });

        // Act
        Loan createdLoan = loanService.createLoan(userId, bookId);

        // Assert
        assertNotNull(createdLoan);
        verify(bookService, times(1)).decreaseAvailableCopies(bookId);

        System.out.println("✅ Available copies minskas korrekt vid lån");
    }

    @Test
    @DisplayName("Extra test: Kontrollera att available_copies ökar vid retur")
    void testReturnBook_ShouldIncreaseAvailableCopies() {
        // Arrange
        Long loanId = 1L;
        Loan existingLoan = new Loan();
        existingLoan.setLoanId(loanId);
        existingLoan.setBookId(bookId);
        existingLoan.setUserId(userId);
        existingLoan.setBorrowedDate(LocalDate.now().minusDays(7));
        existingLoan.setDueDate(LocalDate.now().plusDays(7));
        existingLoan.setReturnedDate(null); // Inte återlämnad än

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(existingLoan));
        when(bookService.increaseAvailableCopies(bookId)).thenReturn(true);
        when(loanRepository.save(any(Loan.class))).thenReturn(existingLoan);

        // Act
        Loan returnedLoan = loanService.returnBook(loanId);

        // Assert
        assertNotNull(returnedLoan.getReturnedDate(), "Returned date ska sättas");
        assertEquals(LocalDate.now(), returnedLoan.getReturnedDate(),
                "Returned date ska vara dagens datum");
        verify(bookService, times(1)).increaseAvailableCopies(bookId);

        System.out.println("✅ Available copies ökas korrekt vid återlämning");
    }

    @Test
    @DisplayName("Extra test: Man kan inte låna samma bok två gånger")
    void testCreateLoan_ShouldFailWhenUserAlreadyHasActiveLoan() {
        // Arrange
        Loan existingLoan = new Loan();
        existingLoan.setLoanId(1L);
        existingLoan.setUserId(userId);
        existingLoan.setBookId(bookId);

        when(userService.getUserById(userId)).thenReturn(Optional.of(testUser));
        when(bookService.isBookAvailable(bookId)).thenReturn(true);
        when(loanRepository.findActiveLoadByUserIdAndBookId(userId, bookId))
                .thenReturn(Optional.of(existingLoan));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> loanService.createLoan(userId, bookId),
                "Ska kasta exception om användaren redan har ett aktivt lån för boken"
        );

        assertEquals("User already has an active loan for this book",
                exception.getMessage());

        System.out.println("✅ Användare kan inte låna samma bok två gånger");
    }

    @Test
    @DisplayName("Förläng lån - ska lägga till 14 dagar")
    void testExtendLoan_ShouldAdd14Days() {
        // Arrange
        Long loanId = 1L;
        LocalDate originalDueDate = LocalDate.now().plusDays(7);
        LocalDate expectedNewDueDate = originalDueDate.plusDays(14);

        Loan existingLoan = new Loan();
        existingLoan.setLoanId(loanId);
        existingLoan.setDueDate(originalDueDate);
        existingLoan.setReturnedDate(null);

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(existingLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(existingLoan);

        // Act
        Loan extendedLoan = loanService.extendLoan(loanId);

        // Assert
        assertEquals(expectedNewDueDate, extendedLoan.getDueDate(),
                "Lånet ska förlängas med exakt 14 dagar");

        System.out.println("✅ Lån förlängs korrekt med 14 dagar");
        System.out.println("   Original återlämning: " + originalDueDate);
        System.out.println("   Ny återlämning: " + extendedLoan.getDueDate());
    }
}