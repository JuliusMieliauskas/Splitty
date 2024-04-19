package server.api;

import commons.Event;
import commons.Expense;
import commons.User;
import commons.exceptions.InvalidEventException;
import commons.exceptions.InvalidUserException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.database.EventRepository;
import server.database.ExpenseRepository;
import server.database.UserExpenseRepository;
import server.database.UserRepository;
import server.services.EventService;
import server.services.ExpenseService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventUnitTest {
    EventRepository mockEventRepository = Mockito.mock(EventRepository.class);
    ExpenseService mockExpenseService = Mockito.mock(ExpenseService.class);
    ExpenseRepository mockExpenseRepository = Mockito.mock(ExpenseRepository.class);
    UserRepository mockUserRepository = Mockito.mock(UserRepository.class);
    UserExpenseRepository mockUserExpenseRepository = Mockito.mock(UserExpenseRepository.class);
    EventService eventService = new EventService(mockEventRepository,
            mockUserRepository,
            mockExpenseRepository, 
            mockExpenseService,
            mockUserExpenseRepository);
    Event mockEvent = Mockito.mock(Event.class);
    User mockUser = Mockito.mock(User.class);
    Expense mockExpense = Mockito.mock(Expense.class);

    /**
     * Initialize mocks with correct responses to method
     */
    @BeforeEach
    public void initMocks() {
        when(mockEvent.getTitle()).thenReturn("Cool event");
        when(mockEvent.getCreationDate()).thenReturn(LocalDateTime.now());
        when(mockEvent.getId()).thenReturn(1L);
        when(mockEvent.getUsers()).thenReturn(new HashSet<>());
        when(mockEvent.getExpenses()).thenReturn(new HashSet<>());
        when(mockUser.getId()).thenReturn(2L);
        when(mockExpense.getId()).thenReturn(3L);

        when(mockUserRepository.existsById(2L)).thenReturn(true);
        when(mockUserRepository.getReferenceById(2L)).thenReturn(mockUser);
        when(mockUserRepository.findById(2L)).thenReturn(Optional.ofNullable(mockUser));

        when(mockEventRepository.save(any(Event.class))).thenAnswer(i -> i.getArguments()[0]);
        when(mockEventRepository.existsById(1L)).thenReturn(true);
        when(mockEventRepository.getReferenceById(1L)).thenReturn(mockEvent);
        when(mockEventRepository.findById(1L)).thenReturn(Optional.ofNullable(mockEvent));

    }

    /**
     * Assumes addUserToEvent is working
     */
    @Test
    public void testCreateEventSuccess() {
        when(mockEventRepository.existsByInviteCode(anyString())).thenReturn(false);

        eventService.createEvent(mockEvent);
        verify(mockEventRepository).existsByInviteCode(anyString()); // check if invite code is taken

        verify(mockEvent).setInviteCode(anyString()); // check if invite code is set
        verify(mockEventRepository, times(1)).save(mockEvent);
    }

    /**
     * Assumes addUserToEvent is working
     */
    @Test
    public void testCreateEventSuccessInviteCodeTaken() {
        when(mockEventRepository.existsByInviteCode(anyString())).thenReturn(true).thenReturn(false);

        eventService.createEvent(mockEvent);
        verify(mockEventRepository, times(2)).existsByInviteCode(anyString());

        verify(mockEvent).setInviteCode(anyString()); // check if invite code is set
        verify(mockEventRepository, times(1)).save(mockEvent);
    }

    /**
     *
     */
    @Test
    public void testCreateEventCreationDateNull() {
        when(mockEvent.getCreationDate()).thenReturn(null);

        assertThrows(InvalidEventException.class, () -> {
            eventService.createEvent(mockEvent);
        });
    }

    /**
     *
     */
    @Test
    public void testCreateEventTitleNull() {
        when(mockEvent.getTitle()).thenReturn(null);

        assertThrows(InvalidEventException.class, () -> {
            eventService.createEvent(mockEvent);
        });
    }

    /**
     *
     */
    @Test
    public void testGetEventById() {

        assertEquals(mockEvent, eventService.getEventById(1L));
    }

    /**
     *
     */
    @Test
    public void testGetEventByIdNull() {


        assertThrows(InvalidEventException.class, () -> {
            assertEquals(mockEvent, eventService.getEventById(null));
        });
    }

    /**
     *
     */
    @Test
    public void testGetEventByIdFails() {
        when(mockEventRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(InvalidEventException.class, () -> {
            eventService.getEventById(2L);
        });
    }

    /**
     *
     */
    @Test
    public void testDeleteEvent() {
        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser)));
        when(mockEvent.getExpenses()).thenReturn(new HashSet<>(List.of(mockExpense)));
        when(mockUser.getEvents()).thenReturn(new HashSet<>(List.of(mockEvent)));

        eventService.deleteEventById(1L);
        verify(mockEventRepository).existsById(1L);
        verify(mockEventRepository).getReferenceById(1L);

        verify(mockExpenseService).deleteExpenseById(3L, false);
        verify(mockUserRepository).save(argThat((User user) -> user.getId() == 2L));

        verify(mockEventRepository).deleteById(1L);
    }

    /**
     *
     */
    @Test
    public void testDeleteEventInvalidId() {
        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser)));
        when(mockEvent.getExpenses()).thenReturn(new HashSet<>(List.of(mockExpense)));
        when(mockUser.getEvents()).thenReturn(new HashSet<>(List.of(mockEvent)));

        assertThrows(InvalidEventException.class, () -> {
            eventService.deleteEventById(2L);
        });
        verify(mockEventRepository).existsById(2L);
    }

    /**
     *
     */
    @Test
    public void testDeleteEventNull() {
        assertThrows(InvalidEventException.class, () -> {
            eventService.deleteEventById(null);
        });
    }

    /**
     * Assumes deleteEventById is working
     */
    @Test
    public void testDeleteAll() {
        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser)));
        when(mockEvent.getExpenses()).thenReturn(new HashSet<>(List.of(mockExpense)));
        when(mockUser.getEvents()).thenReturn(new HashSet<>(List.of(mockEvent)));

        when(mockEventRepository.findAll()).thenReturn(List.of(mockEvent, mockEvent, mockEvent));

        eventService.deleteAllEvents();
        verify(mockEventRepository, times(3)).deleteById(1L);
    }


    /**
     * Assumes getEventById is working
     */
    @Test
    public void testGetUsers() {
        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser)));

        assertEquals(1, eventService.getUsersOfEvent(1L).size());
        verify(mockEvent).getUsers();
    }

    /**
     * Assumes getEventById is working
     */
    @Test
    public void testGetUsersInvalidEvent() {
        when(mockEventRepository.findById(2L)).thenReturn(Optional.empty());
        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser)));

        assertThrows(InvalidEventException.class,
                () -> {
                    eventService.getUsersOfEvent(2L);
                });
        verify(mockEvent, times(0)).getUsers();
    }

    /**
     *
     */
    @Test
    public void testAddUserToEvent() {
        eventService.addUserToEvent(1L, mockUser);
        verify(mockEvent).addUser(mockUser);
        verify(mockEventRepository).save(mockEvent);
    }

    /**
     *
     */
    @Test
    public void testAddUserToEventInvalidUser() {
        when(mockUserRepository.existsById(anyLong())).thenReturn(false);

        assertThrows(InvalidUserException.class,
            () -> {
                eventService.addUserToEvent(1L, mockUser);
            });

    }

    /**
     *
     */
    @Test
    public void testAddUserToEventUserAlreadyInEvent() {
        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser)));

        assertThrows(InvalidEventException.class,
                () -> {
                    eventService.addUserToEvent(1L, mockUser);
                });

    }

    /**
     *
     */
    @Test
    public void testDeleteUserFromEvent() {
        Set mockUserSet = Mockito.mock(Set.class);
        Set mockEventSet = Mockito.mock(Set.class);

        when(mockEvent.getUsers()).thenReturn(mockUserSet);
        when(mockUser.getEvents()).thenReturn(mockEventSet);

        when(mockUserSet.contains(mockUser)).thenReturn(true);

        eventService.deleteUserFromEvent(1L, 2L);
        verify(mockUserSet).remove(mockUser);
        verify(mockEventRepository).save(mockEvent);
        verify(mockEventSet).remove(mockEvent);
        verify(mockUserRepository).save(mockUser);

    }

    /**
     *
     */
    @Test
    public void testDeleteInvalidUserFromEvent() {
        assertThrows(InvalidUserException.class,
                () -> {
                    eventService.deleteUserFromEvent(1L, 6L);
                });

    }

    /**
     *
     */
    @Test
    public void testDeleteUserFromInvalidEvent() {
        assertThrows(InvalidEventException.class,
                () -> {
                    eventService.deleteUserFromEvent(6L, 2L);
                });

    }

    /**
     *
     */
    @Test
    public void testDeleteUserNotInEventFromEvent() {
        Set mockUserSet = Mockito.mock(Set.class);

        when(mockEvent.getUsers()).thenReturn(mockUserSet);
        when(mockUserSet.contains(mockUser)).thenReturn(false);

        assertThrows(InvalidEventException.class,
                () -> {
                    eventService.deleteUserFromEvent(1L, 2L);
                });

    }

    /**
     *
     */
    @Test
    public void testUpdateEvent() {
        // most checks have already been tested, since they are shared between update and add event
        Event mockUpdated = Mockito.mock(Event.class);
        when(mockUpdated.getTitle()).thenReturn("Cool event2");
        when(mockUpdated.getCreationDate()).thenReturn(LocalDateTime.now());
        when(mockUpdated.getId()).thenReturn(1L);
        when(mockUpdated.getInviteCode()).thenReturn("abcd");

        eventService.updateEvent(1L, mockUpdated);
        verify(mockEvent).setInviteCode("abcd");
        verify(mockEvent).setTitle("Cool event2");
    }

    /**
     * The valid user check is the same as for creating a user,
     * so that won't be tested here
     */
    @Test
    public void testGetByUsername() {
        User mockUser2 = Mockito.mock(User.class);
        when(mockUser.getUsername()).thenReturn("george");
        when(mockUser2.getUsername()).thenReturn("jeroen");

        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser, mockUser2)));
        assertEquals(mockUser2, eventService.getByUsername(1L, "   JerOeN  "));
    }

    /**
     * The valid user check is the same as for creating a user,
     * so that won't be tested here
     */
    @Test
    public void testGetByInvalidUsername() {
        User mockUser2 = Mockito.mock(User.class);
        when(mockUser.getUsername()).thenReturn("george");
        when(mockUser2.getUsername()).thenReturn("jeroen");

        when(mockEvent.getUsers()).thenReturn(new HashSet<>(List.of(mockUser, mockUser2)));
        assertThrows(InvalidUserException.class, () -> {
            eventService.getByUsername(1L, "   mAx  ");
        });
    }


}
