package commons;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserTest {

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

        expense = new Expense(14.99, "Toy", u11, event);
        expense.setId(Long.parseLong("1"));

        userExpense21 = new UserExpense(u21, expense, 0.0, 0.0);
    }


    @Test
    void getId() {
        assertEquals(2, u11.getId());
    }

    @Test
    void getPaidExpenses() {
        assertEquals(new HashSet<>(), u11.getPaidExpenses());
    }

    @Test
    void addEvent() {
        u21.addEvent(event);
        Set<Event> somelist = new HashSet<Event>();
        somelist.add(event);
        assertEquals(somelist, u21.getEvents());
    }

    @Test
    void removeEvent() {
        u0.addEvent(event);
        u0.removeEvent(event);
        assertEquals(new HashSet<>(), u0.getEvents());
    }

    @Test
    void addExpense() {
        Set<UserExpense> set = new HashSet<>();
        assertEquals(set, u21.getUserExpenses());
        u21.addExpense(expense);
        set.add(userExpense21);
        assertEquals(set, u21.getUserExpenses());
    }

    @Test
    void removeExpense() {
        Set<UserExpense> set = new HashSet<>();
        assertEquals(set, u21.getUserExpenses());
        u21.addExpense(expense);
        u21.removeExpense(expense);
        assertEquals(set, u21.getUserExpenses());
    }

    @Test
    void getUsername() {
        assertNull(u0.getUsername());
        assertEquals("bernard", u11.getUsername());
    }

    @Test
    void setUsername() {
        u0.setUsername("carlos");
        assertEquals("carlos", u0.getUsername());
    }

    @Test
    void getIban() {
        assertEquals("00this0is0iban0012", u21.getIban());
        assertEquals("00ing00nl001234567", u11.getIban());
        assertNull(u0.getIban());
    }

    @Test
    void setIban() {
        u0.setIban("0088iban0088");
        assertEquals("0088iban0088", u0.getIban());
    }

    @Test
    void getEmail() {
        assertEquals("some@mail.com", u21.getEmail());
        assertEquals("mymail@google.com", u11.getEmail());
        assertNull(u0.getEmail());
    }

    @Test
    void setEmail() {
        u0.setEmail("mym@iladdress.com");
        assertEquals("mym@iladdress.com", u0.getEmail());
    }

    @Test
    void testEquals() {
        assertEquals(u0, u0);
        assertNotEquals(u11, u12);
        assertNotEquals(u11, u21);
    }

    @Test
    void testHashCode() {
        assertEquals(u0.hashCode(), u0.hashCode());
        assertNotEquals(u11.hashCode(), u12.hashCode());
        assertNotEquals(u11.hashCode(), u21.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("User{" +
                "id=1" +
                ", username='null'" +
                ", iban='null'" +
                ", email='null'" +
                ", events=[]" +
                ", userExpenses=[]" +
                '}', u0.toString());
        assertEquals("User{" +
                "id=2" +
                ", username='bernard'" +
                ", iban='00ing00nl001234567'" +
                ", email='mymail@google.com'" +
                ", events=[]" +
                ", userExpenses=[]" +
                '}', u11.toString());

    }
}