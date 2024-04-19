package server.api;

import commons.Event;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import commons.exceptions.InvalidUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import server.database.EventRepository;
import server.database.ExpenseRepository;
import server.database.UserRepository;
import server.services.UserService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserUnitTest {
    EventRepository mockEventRepository = Mockito.mock(EventRepository.class);
    ExpenseRepository mockExpenseRepository = Mockito.mock(ExpenseRepository.class);
    UserRepository mockUserRepository = Mockito.mock(UserRepository.class);
    UserService userService = new UserService(mockUserRepository, mockEventRepository, mockExpenseRepository);
    Event mockEvent = Mockito.mock(Event.class);
    User mockUser = Mockito.mock(User.class);
    Expense mockExpense = Mockito.mock(Expense.class);
    Expense mockPaidExpense = Mockito.mock(Expense.class);
    UserExpense mockUserExpense = Mockito.mock(UserExpense.class);

    /**
     * Initialize mocks with correct responses to method
     */
    @BeforeEach
    public void initMocks() {
        when(mockEvent.getId()).thenReturn(1L);
        when(mockUser.getId()).thenReturn(2L);
        when(mockUser.getUsername()).thenReturn("Jeroen");
        when(mockUser.getEvents()).thenReturn(new HashSet<>(List.of(mockEvent)));
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>(List.of(mockUserExpense)));
        when(mockUser.getPaidExpenses()).thenReturn(new HashSet<>(List.of(mockPaidExpense)));
        when(mockExpense.getId()).thenReturn(3L);
        when(mockPaidExpense.getId()).thenReturn(4L);

        when(mockUserExpense.getDebtor()).thenReturn(mockUser);
        when(mockUserExpense.getExpense()).thenReturn(mockExpense);

        when(mockUserRepository.existsById(2L)).thenReturn(true);
        when(mockUserRepository.getReferenceById(2L)).thenReturn(mockUser);
        when(mockUserRepository.findById(2L)).thenReturn(Optional.ofNullable(mockUser));
        when(mockUserRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        when(mockEventRepository.existsById(1L)).thenReturn(true);
        when(mockEventRepository.getReferenceById(1L)).thenReturn(mockEvent);
        when(mockEventRepository.findById(1L)).thenReturn(Optional.ofNullable(mockEvent));

        when(mockExpenseRepository.findByOriginalPayer(mockUser)).thenReturn(List.of(mockPaidExpense));
    }

    /**
     *
     */
    @Test
    public void testCreateUser() {
        assertEquals(mockUser, userService.createUser(mockUser));
        verify(mockUserRepository).save(mockUser);
    }

    /**
     *
     */
    @Test
    public void testCreateInvalidUser() {
        when(mockUser.getUsername()).thenReturn("");
        assertThrows(InvalidUserException.class,
                () -> {
                    userService.createUser(mockUser);
                });
    }

    /**
     * also tests checkSafeDeleteUser(long id)
     */
    @Test
    public void testDeleteUserInvalidId() {
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>());
        when(mockExpenseRepository.findByOriginalPayer(mockUser)).thenReturn(new ArrayList<>());
        assertThrows(InvalidUserException.class,
                () -> {
                    userService.deleteUser(-1L);
                });
    }

    /**
     * also tests checkSafeDeleteUser(long id)
     */
    @Test
    public void testDeleteUserUserOwnsExpense() {
        when(mockExpenseRepository.findByOriginalPayer(mockUser)).thenReturn(new ArrayList<>());
        assertThrows(DataIntegrityViolationException.class,
                () -> {
                    userService.deleteUser(2L);
                });
    }

    /**
     * also tests checkSafeDeleteUser(long id)
     */
    @Test
    public void testDeleteUserUserPaidForExpenses() {
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>());
        assertThrows(DataIntegrityViolationException.class,
                () -> {
                    userService.deleteUser(2L);
                });
    }


    /**
     * also tests checkSafeDeleteUser(long id)
     */
    @Test
    public void testDeleteUserSuccess() {
        when(mockUser.getUserExpenses()).thenReturn(new HashSet<>());
        when(mockExpenseRepository.findByOriginalPayer(mockUser)).thenReturn(new ArrayList<>());

        userService.deleteUser(2L);
        verify(mockEventRepository).save(mockEvent); // user was removed from event
        verify(mockUserRepository).deleteById(2L);
    }


    @Test
    public void testGetEvent() {
        assertEquals(new HashSet<>(List.of(mockEvent)), userService.getEvents(2L));
    }

    @Test
    public void testGetExpenses() {
        assertEquals(new HashSet<>(List.of(mockUserExpense)), userService.getExpenses(2L));
    }

    @Test
    public void testGetById() {
        assertEquals(mockUser, userService.getById(2L));
    }

    @Test
    public void testGetByIdInvalid() {
        assertThrows(InvalidUserException.class, () -> {
            userService.getById(10L);
        });
    }

    /**
     * The valid user check is the same as for creating a user,
     * so that won't be tested here
     */
    @Test
    public void testUpdateUser() {
        User updated = Mockito.mock(User.class);
        when(updated.getId()).thenReturn(2L);
        when(updated.getUsername()).thenReturn("Jeroen2");
        when(updated.getEmail()).thenReturn("Jeroen2@mail.com"); // even when email and iban are set in updated user
        when(updated.getIban()).thenReturn("NL98INGB0003856626");// they should not be stored

        userService.updateUser(2L, updated);

        verify(mockUser).setUsername("Jeroen2");
        verify(mockUser, times(0)).setEmail("Jeroen2@mail.com"); // should not be called
        verify(mockUser, times(0)).setIban("NL98INGB0003856626"); // should not be called
    }
}
