package commons;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseTest {

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
    void setTitle() {
        expense.setTitle("Books");
        assertEquals("Books", expense.getTitle());
    }

    @Test
    void setTag() {
        expense.setTag("Time");
        assertEquals("Time", expense.getTag());
    }

    @Test
    void setOriginalPayer() {
        expense.setOriginalPayer(u0);
        assertEquals(u0, expense.getOriginalPayer());
    }

    @Test
    void setEvent() {
        expense.setEvent(event);
        assertEquals(event, expense.getEvent());
    }

    @Test
    void setUserExpenses() {
        assertEquals(new HashSet<>(), expense.getUserExpenses());
        Set<UserExpense> set = new HashSet<>();
        set.add(userExpense21);
        expense.setUserExpenses(set);
        assertEquals(set, expense.getUserExpenses());
    }

    @Test
    void addUser() {
        assertFalse(expense.getUserExpenses().contains(userExpense21));
        expense.addUser(u21);
        assertTrue(expense.getUserExpenses().contains(userExpense21));
    }

    @Test
    void removeUser() {
        assertFalse(expense.getUserExpenses().contains(userExpense21));
        expense.addUser(u21);
        expense.removeUser(u21);
        assertFalse(expense.getUserExpenses().contains(userExpense21));
    }

    @Test
    void setAmount() {
        assertEquals(14.99, expense.getAmount());
        expense.setAmount(89.00);
        assertEquals(89.00, expense.getAmount());
    }

    @Test
    void testEquals() {
        Expense expense2 = new Expense(14.99, "Toy", u11, event);
        expense2.setId(Long.parseLong("2"));
        assertEquals(expense, expense);
        assertNotEquals(expense2, expense);
    }

    @Test
    void testHashCode() {
        Expense expense2 = new Expense(14.99, "Toy", u11, event);
        expense2.setId(Long.parseLong("2"));
        assertEquals(expense.hashCode(), expense.hashCode());
        assertNotEquals(expense2.hashCode(), expense.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("Expense{" +
                "id=1" +
                ", amount=14.99" +
                ", title='Toy'" +
                ", tag='null'" +
                ", originalPayer=User{id=2, username='bernard', iban='00ing00nl001234567', " +
                "email='mymail@google.com', events=[], userExpenses=[]}" +
                ", event=" + event + // because of the creationDate
                ", userExpenses=[]" +
                '}', expense.toString());
    }
}