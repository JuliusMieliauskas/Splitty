package commons;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Every participant and expense that now is an Integer should in the future be changed to the corresponding class.
 * These test thereby need to be changed for a tiny bit when that happens.
 */
public class EventTest {

    private User u0;
    private User u11;
    private User u12;
    private User u21;
    private Event event;
    private Expense expense;
    private UserExpense userExpense21;

    @BeforeEach
    void initSomeData() {
        u0 = new User();
        u0.setId(Long.parseLong("1"));
        u11 = new User("mymail@google.com", "00ing00nl001234567", "bernard");
        u11.setId(Long.parseLong("2"));
        u12 = new User("mymail@google.com", "00ing00nl001234567", "bernard");
        u12.setId(Long.parseLong("3"));
        u21 = new User("some@mail.com", "00this0is0iban0012", "jack");
        u21.setId(Long.parseLong("4"));

        event = new Event("myEvent!");
        event.setId(Long.parseLong("1"));

        expense = new Expense(14.99, "Toy", u11, event);
        expense.setId(Long.parseLong("1"));

        userExpense21 = new UserExpense(u21, expense, 0.0, 0.0);
        userExpense21.setId(Long.parseLong("1"));
    }

    /**
     * Test for the basic functionality of the Event class
     */
    @Test
    void eventBasicTests() {
        Event e1 = new Event("hello");
        Event e2 = new Event("hi");
        assertEquals("hello", e1.getTitle());
        assertEquals("hi", e2.getTitle());
    }

    @Test
    void addUser() {
        Set<User> set = new HashSet<>();
        assertEquals(set, event.getUsers());
        set.add(u0);
        event.addUser(u0);
        assertEquals(set, event.getUsers());
    }

    @Test
    void removeUser() {
        Set<User> set = new HashSet<>();
        assertEquals(set, event.getUsers());
        event.addUser(u0);
        event.removeUser(u0);
        assertEquals(set, event.getUsers());
    }

    @Test
    void addExpense() {
        Set<Expense> set = new HashSet<>();
        assertEquals(set, event.getExpenses());
        set.add(expense);
        event.addExpense(expense);
        assertEquals(set, event.getExpenses());
    }

    @Test
    void removeExpense() {
        Set<Expense> set = new HashSet<>();
        assertEquals(set, event.getExpenses());
        event.addExpense(expense);
        event.removeExpense(expense);
        assertEquals(set, event.getExpenses());
    }

    @Test
    void setInviteCode() {
        assertNull(event.getInviteCode());
        event.setInviteCode("01invite00me");
        assertEquals("01invite00me", event.getInviteCode());
    }


    @Test
    void setTitle() {
        assertEquals("myEvent!", event.getTitle());
        event.setTitle("hahahaEventMeNow");
        assertEquals("hahahaEventMeNow", event.getTitle());
    }

    @Test
    void testEquals() {
        Event event2 = new Event("secondary");
        event2.setId(Long.parseLong("2"));
        assertNotEquals(event2, event);
        assertEquals(event, event);
    }

    @Test
    void testHashCode() {
        Event event2 = new Event("secondary");
        event2.setId(Long.parseLong("2"));
        assertNotEquals(event2.hashCode(), event.hashCode());
        assertEquals(event.hashCode(), event.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("Event{" +
                "id=1" +
                ", title='myEvent!'" +
                ", inviteCode='null'" +
                ", creationDate=" + event.getCreationDate() +
                ", lastActivity=" + event.getLastActivity() +
                ", users=[]" +
                ", expenses=[]" +
                '}', event.toString());
    }

}
