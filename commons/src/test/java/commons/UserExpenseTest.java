package commons;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class UserExpenseTest {

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
        userExpense21.setId(Long.parseLong("1"));
    }

    @Test
    void getId() {
        assertEquals(1, userExpense21.getId());
    }

    @Test
    void getDebtor() {
        assertEquals(u21, userExpense21.getDebtor());
    }

    @Test
    void setDebtor() {
        userExpense21.setDebtor(u0);
        assertEquals(u0, userExpense21.getDebtor());
    }

    @Test
    void getExpense() {
        assertEquals(expense, userExpense21.getExpense());
    }

    @Test
    void setExpense() {
        Expense expense2 = new Expense(2.05, "Candy", u0, event);
        expense2.setId(Long.parseLong("2"));
        userExpense21.setExpense(expense2);
        assertEquals(expense2, userExpense21.getExpense());
    }

    @Test
    void setTotalAmount() {
        assertEquals(0.0, userExpense21.getTotalAmount());
        userExpense21.setTotalAmount(5.00);
        assertEquals(5.00, userExpense21.getTotalAmount());
    }

    @Test
    void setPaidAmount() {
        assertEquals(0.0, userExpense21.getPaidAmount());
        userExpense21.setPaidAmount(5.00);
        assertEquals(5.00, userExpense21.getPaidAmount());
    }

    @Test
    void testEquals() {
        Expense expense2 = new Expense(2.05, "Candy", u0, event);
        expense2.setId(Long.parseLong("2"));
        UserExpense userExpense3 = new UserExpense(u21, expense2, 0.0, 0.0);
        userExpense3.setId(Long.parseLong("2"));
        assertNotEquals(userExpense3, userExpense21);
        assertEquals(userExpense21, userExpense21);
    }

    @Test
    void testHashCode() {
        Expense expense2 = new Expense(2.05, "Candy", u0, event);
        expense2.setId(Long.parseLong("2"));
        UserExpense userExpense3 = new UserExpense(u21, expense2, 0.0, 0.0);
        userExpense3.setId(Long.parseLong("2"));
        assertNotEquals(userExpense3.hashCode(), userExpense21.hashCode());
        assertEquals(userExpense21.hashCode(), userExpense21.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("UserExpense{" +
                "id=1" +
                ", debtor=User{id=4, username='jack', iban='00this0is0iban0012', email='some@mail.com', events=[], userExpenses=[]}" +
                ", expense=" + expense + // creation date, so must be variable
                ", totalAmount=0.0" +
                ", paidAmount=0.0" +
                '}', userExpense21.toString());
    }
}