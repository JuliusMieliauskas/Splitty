package server.api;

import commons.Event;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import commons.exceptions.InvalidExpenseException;
import commons.exceptions.InvalidUserExpenseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.database.EventRepository;
import server.database.ExpenseRepository;
import server.database.UserExpenseRepository;
import server.database.UserRepository;
import server.services.ExpenseService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExpenseUnitTest {
    EventRepository mockEventRepository = Mockito.mock(EventRepository.class);
    ExpenseRepository mockExpenseRepository = Mockito.mock(ExpenseRepository.class);
    UserExpenseRepository mockUserExpenseRepository = Mockito.mock(UserExpenseRepository.class);
    UserRepository mockUserRepository = Mockito.mock(UserRepository.class);
    ExpenseService expenseService = new ExpenseService(
            mockExpenseRepository, mockUserExpenseRepository, mockUserRepository, mockEventRepository
    );
    Event mockEvent = Mockito.mock(Event.class);
    User mockUser = Mockito.mock(User.class);
    User mockUser2 = Mockito.mock(User.class);
    Expense mockExpense = Mockito.mock(Expense.class);
    UserExpense mockUserExpense = Mockito.mock(UserExpense.class);

    /**
     * Initialize mocks with correct responses to methods
     */
    @BeforeEach
    public void initMocks() {
        when(mockEvent.getExpenses()).thenReturn(new HashSet<>(List.of(mockExpense)));
        when(mockEvent.getId()).thenReturn(1L);
        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser, mockUser2)));

        when(mockUser.getId()).thenReturn(2L);
        when(mockUser2.getId()).thenReturn(10L);
        when(mockUser.getEvents()).thenReturn(new HashSet<>(List.of(mockEvent)));
        when(mockUser2.getEvents()).thenReturn(new HashSet<>(List.of(mockEvent)));
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>(List.of(mockUserExpense)));
        when(mockUser.getPaidExpenses()).thenReturn(new HashSet<>());
        when(mockUser2.getUserExpenses()).thenReturn(new HashSet<>());
        when(mockUser2.getPaidExpenses()).thenReturn(new HashSet<>(List.of(mockExpense)));
        when(mockExpense.getId()).thenReturn(3L);
        when(mockExpense.getEvent()).thenReturn(mockEvent);
        when(mockExpense.getUserExpenses()).thenReturn(new HashSet<>(List.of(mockUserExpense)));
        when(mockExpense.getAmount()).thenReturn(40.);
        when(mockExpense.getOriginalPayer()).thenReturn(mockUser2);
        when(mockExpense.getCreationDate()).thenReturn(LocalDateTime.now());
        when(mockExpense.getTag()).thenReturn("Food");
        when(mockExpense.getTitle()).thenReturn("Burgers");


        when(mockUserExpense.getId()).thenReturn(30L);
        when(mockUserExpense.getDebtor()).thenReturn(mockUser);
        when(mockUserExpense.getExpense()).thenReturn(mockExpense);
        when(mockUserExpense.getPaidAmount()).thenReturn(10.);
        when(mockUserExpense.getTotalAmount()).thenReturn(38.);
    }

    /**
     * Initialize mocks repositories with correct responses to methods
     */
    @BeforeEach
    public void initMockRepos() {
        when(mockUserRepository.existsById(2L)).thenReturn(true);
        when(mockUserRepository.existsById(10L)).thenReturn(true);
        when(mockUserRepository.getReferenceById(2L)).thenReturn(mockUser);
        when(mockUserRepository.getReferenceById(10L)).thenReturn(mockUser2);
        when(mockUserRepository.findById(2L)).thenReturn(Optional.ofNullable(mockUser));
        when(mockUserRepository.findById(10L)).thenReturn(Optional.ofNullable(mockUser2));

        when(mockEventRepository.existsById(1L)).thenReturn(true);
        when(mockEventRepository.getReferenceById(1L)).thenReturn(mockEvent);
        when(mockEventRepository.findById(1L)).thenReturn(Optional.ofNullable(mockEvent));

        when(mockExpenseRepository.existsById(3L)).thenReturn(true);
        when(mockExpenseRepository.getReferenceById(3L)).thenReturn(mockExpense);
        when(mockExpenseRepository.findById(3L)).thenReturn(Optional.ofNullable(mockExpense));
        when(mockExpenseRepository.findByOriginalPayer(mockUser2)).thenReturn(List.of(mockExpense));
        when(mockExpenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArguments()[0]);
        when(mockExpenseRepository.findAll()).thenReturn(List.of(mockExpense));

        when(mockUserExpenseRepository.existsById(30L)).thenReturn(true);
        when(mockUserExpenseRepository.getReferenceById(30L)).thenReturn(mockUserExpense);
        when(mockUserExpenseRepository.save(any(UserExpense.class))).thenAnswer(i -> i.getArguments()[0]);
        when(mockUserExpenseRepository.findAll()).thenReturn(List.of(mockUserExpense));
    }

    /**
     *
     */
    @Test
    public void testGetExpenseById() {
        assertEquals(mockExpense, expenseService.getExpenseById(3L));
    }

    /**
     *
     */
    @Test
    public void testGetExpenseByIdFail() {
        assertThrows(InvalidExpenseException.class, () -> {
            expenseService.getExpenseById(20L);
        });
    }

    /**
     *
     */
    @Test
    public void testCreateExpense() {
        assertEquals(mockExpense, expenseService.createExpense(mockExpense));
        verify(mockExpenseRepository).save(mockExpense);
    }

    /**
     *
     */
    @Test
    public void testCreateExpenseInvalidAmount() {
        when(mockExpense.getAmount()).thenReturn(0.);
        assertThrows(InvalidExpenseException.class, () -> {
            expenseService.createExpense(mockExpense);
        });
    }

    /**
     *
     */
    @Test
    public void testCreateExpenseNoTitle() {
        when(mockExpense.getTitle()).thenReturn("");
        assertThrows(InvalidExpenseException.class, () -> {
            expenseService.createExpense(mockExpense);
        });
    }

    /**
     *
     */
    @Test
    public void testCreateExpenseUserNotInEvent() {
        when(mockEvent.getUsers()).thenReturn(new HashSet<>());
        assertThrows(InvalidExpenseException.class, () -> {
            expenseService.createExpense(mockExpense);
        });
    }

    /**
     * Most validity control of expense is already tested in createExpense, and is the same across methods
     */
    @Test
    public void testUpdateExpense() {
        Expense updated = Mockito.mock(Expense.class);
        when(updated.getId()).thenReturn(3L);
        when(updated.getEvent()).thenReturn(mockEvent);
        when(updated.getAmount()).thenReturn(1e10);
        when(updated.getOriginalPayer()).thenReturn(mockUser);
        when(updated.getCreationDate()).thenReturn(LocalDateTime.now());
        when(updated.getTag()).thenReturn("Travel");
        when(updated.getTitle()).thenReturn("Elytra");

        assertEquals(mockExpense, expenseService.updateExpense(3L, updated));

        verify(mockExpense).setTitle("Elytra");
        verify(mockExpense).setTag("Travel");
        verify(mockExpense).setOriginalPayer(mockUser);
        verify(mockExpense).setAmount(1e10);
        verify(mockExpenseRepository).save(mockExpense);
    }

    /**
     * Most validity control of expense is already tested in createExpense, and is the same across methods
     */
    @Test
    public void testUpdateExpenseDifferentEvent() {
        Event otherEvent = Mockito.mock(Event.class);
        when(otherEvent.getId()).thenReturn(20L);
        when(mockEventRepository.existsById(20L)).thenReturn(true);
        when(mockEventRepository.getReferenceById(20L)).thenReturn(otherEvent);
        when(mockEventRepository.findById(20L)).thenReturn(Optional.of(otherEvent));

        Expense updated = Mockito.mock(Expense.class);
        when(updated.getId()).thenReturn(3L);
        when(updated.getEvent()).thenReturn(otherEvent);
        when(updated.getAmount()).thenReturn(1e10);
        when(updated.getOriginalPayer()).thenReturn(mockUser);
        when(updated.getCreationDate()).thenReturn(LocalDateTime.now());
        when(updated.getTag()).thenReturn("Travel");
        when(updated.getTitle()).thenReturn("Elytra");

        assertThrows(InvalidExpenseException.class, () -> {
            expenseService.updateExpense(3L, updated);
        });

    }

    /**
     *
     */
    @Test
    public void testDeleteExpenseById() {
        expenseService.deleteExpenseById(3L);
        verify(mockUserExpenseRepository).deleteById(mockUserExpense.getId());
        verify(mockExpenseRepository).deleteById(3L);
    }

    /**
     *
     */
    @Test
    public void testDeleteAllExpenses() {
        expenseService.deleteAllExpenses();
        verify(mockUserExpenseRepository).deleteById(mockUserExpense.getId());
        verify(mockExpenseRepository).deleteById(3L);
    }

    /**
     *
     */
    @Test
    public void testAddUserToExpense() {
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>());

        expenseService.addUserToExpense(3L, mockUserExpense);
        verify(mockUserExpenseRepository).save(mockUserExpense);
    }

    /**
     *
     */
    @Test
    public void testAddUserToExpenseWrongId() {
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>());

        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.addUserToExpense(4L, mockUserExpense);
        });
    }

    /**
     *
     */
    @Test
    public void testAddUserToExpenseTotalAmount0() {
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>());
        when(mockUserExpense.getPaidAmount()).thenReturn(0.);
        when(mockUserExpense.getTotalAmount()).thenReturn(0.);

        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.addUserToExpense(4L, mockUserExpense);
        });
    }

    /**
     *
     */
    @Test
    public void testAddUserToExpensePaidAmountMoreThanTotal() {
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>());
        when(mockUserExpense.getPaidAmount()).thenReturn(50.);

        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.addUserToExpense(4L, mockUserExpense);
        });
    }

    /**
     *
     */
    @Test
    public void testAddUserToExpenseUserPaidForExpense() {
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>());
        when(mockExpense.getOriginalPayer()).thenReturn(mockUser);

        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.addUserToExpense(4L, mockUserExpense);
        });
    }

    /**
     *
     */
    @Test
    public void testAddUserToExpenseUserIsAlreadyDebtorForExpense() {
        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.addUserToExpense(4L, mockUserExpense);
        });
    }

    /**
     * Assumes getUserExpense is working
     */
    @Test
    public void testUpdateUserExpense() {
        UserExpense updated = Mockito.mock(UserExpense.class);
        when(updated.getDebtor()).thenReturn(mockUser);
        when(updated.getExpense()).thenReturn(mockExpense);
        when(updated.getPaidAmount()).thenReturn(12.);
        when(updated.getTotalAmount()).thenReturn(40.);

        assertEquals(mockUserExpense, expenseService.updateUserExpense(3L, mockUser.getId(), updated));
        verify(mockUserExpenseRepository).save(mockUserExpense);
    }

    /**
     * Assumes getUserExpense is working
     * For this case no UserExpense with id 31 exists
     */
    @Test
    public void testUpdateUserExpenseWrongUserExpenseId1() {
        UserExpense updated = Mockito.mock(UserExpense.class);
        when(updated.getDebtor()).thenReturn(mockUser);
        when(updated.getExpense()).thenReturn(mockExpense);
        when(updated.getPaidAmount()).thenReturn(12.);
        when(updated.getTotalAmount()).thenReturn(40.);

        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.updateUserExpense(3L, 31L, updated);
        });
    }

    /**
     * Assumes getUserExpense is working
     * For this case a UserExpense with id 31 exists, but it has a different Debtor
     */
    @Test
    public void testUpdateUserExpenseWrongUserExpenseId2() {
        UserExpense updated = Mockito.mock(UserExpense.class);
        when(updated.getDebtor()).thenReturn(mockUser);
        when(updated.getExpense()).thenReturn(mockExpense);
        when(updated.getPaidAmount()).thenReturn(12.);
        when(updated.getTotalAmount()).thenReturn(40.);

        UserExpense other = Mockito.mock(UserExpense.class);
        when(other.getId()).thenReturn(31L);
        when(other.getDebtor()).thenReturn(mockUser2);
        when(other.getExpense()).thenReturn(mockExpense);
        when(other.getPaidAmount()).thenReturn(12.);
        when(other.getTotalAmount()).thenReturn(40.);

        when(mockUserExpenseRepository.existsById(31L)).thenReturn(true);
        when(mockUserExpenseRepository.getReferenceById(31L)).thenReturn(other);

        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.updateUserExpense(3L, 31L, updated);
        });
    }

    /**
     * Assumes getUserExpense is working
     * For this case a UserExpense with id 31 exists, but it is a part of a different expense
     */
    @Test
    public void testUpdateUserExpenseWrongUserExpenseId3() {
        Expense otherExpense = Mockito.mock(Expense.class);
        when(otherExpense.getId()).thenReturn(42L);

        UserExpense updated = Mockito.mock(UserExpense.class);
        when(updated.getDebtor()).thenReturn(mockUser);
        when(updated.getExpense()).thenReturn(mockExpense);
        when(updated.getPaidAmount()).thenReturn(12.);
        when(updated.getTotalAmount()).thenReturn(40.);

        UserExpense other = Mockito.mock(UserExpense.class);
        when(other.getId()).thenReturn(31L);
        when(other.getDebtor()).thenReturn(mockUser);
        when(other.getExpense()).thenReturn(otherExpense);
        when(other.getPaidAmount()).thenReturn(12.);
        when(other.getTotalAmount()).thenReturn(40.);

        when(mockExpenseRepository.existsById(42L)).thenReturn(true);
        when(mockExpenseRepository.getReferenceById(42L)).thenReturn(otherExpense);

        when(mockUserExpenseRepository.existsById(31L)).thenReturn(true);
        when(mockUserExpenseRepository.getReferenceById(31L)).thenReturn(other);

        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.updateUserExpense(42L, 31L, updated);
        });
    }

    /**
     *
     */
    @Test
    public void testGetUserExpense() {
        assertEquals(mockUserExpense, expenseService.getUserExpense(3L, 30L));
    }

    /**
     *
     */
    @Test
    public void testGetUserExpenseIdsNotMatching() {
        assertThrows(InvalidUserExpenseException.class, () -> {
            expenseService.getUserExpense(4L, 30L);
        });
    }

    /**
     * Assumes getUserExpense is working
     * Valid id check is already tested for method getUserExpense
     */
    @Test
    public void testDeleteUserFromExpense() {
        expenseService.deleteUserFromExpense(3L, 2L);
        verify(mockUserExpenseRepository).deleteById(30L);
    }

    /**
     *
     */
    @Test
    public void testGetDebtors() {
        assertEquals(new HashSet<>(List.of(mockUserExpense)), expenseService.getDebtorsFromExpense(3L));
    }
}
