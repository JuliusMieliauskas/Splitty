package client.utils;

import client.MockConfigUtils;
import commons.Expense;
import commons.UserExpense;
import jakarta.ws.rs.core.GenericType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MissingJavadocMethod")
public class ExpenseUtilsTest {
    @Test
    public void testGetDebtorsOfExpense() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long expenseId = 10L;
            List<UserExpense> mockUserExpenses = List.of(Mockito.mock(UserExpense.class));

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/expenses/" + expenseId + "/debtors", MockConfigUtils.APIMethod.GET,
                    new GenericType<List<UserExpense>>() { }, mockUserExpenses);

            assertEquals(mockUserExpenses, ExpenseUtils.getDebtorsOfExpense(expenseId));
            MockConfigUtils.verifyRequest("api/expenses/" + expenseId + "/debtors", MockConfigUtils.APIMethod.GET);
        }
    }

    @Test
    public void testGetExpenseById() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long expenseId = 10L;
            Expense mockExpense = Mockito.mock(Expense.class);
            HashSet<UserExpense> mockUserExpenses = new HashSet<>(List.of(Mockito.mock(UserExpense.class)));
            when(mockExpense.getId()).thenReturn(expenseId);
            when(mockExpense.getUserExpenses()).thenReturn(mockUserExpenses);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/expenses/" + expenseId, MockConfigUtils.APIMethod.GET, Expense.class, mockExpense);
            MockConfigUtils.mockEndPoint("api/expenses/" + expenseId + "/debtors", MockConfigUtils.APIMethod.GET,
                    new GenericType<List<UserExpense>>() { }, List.copyOf(mockUserExpenses));

            Expense retrievedExpense = ExpenseUtils.getExpenseById(expenseId);
            assertEquals(mockExpense, retrievedExpense);
            assertEquals(mockUserExpenses, retrievedExpense.getUserExpenses());
            verify(mockExpense).setUserExpenses(mockUserExpenses);
        }
    }

    @Test
    public void testCreateExpense() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Expense mockExpense = Mockito.mock(Expense.class);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/expenses", MockConfigUtils.APIMethod.POST, Expense.class, mockExpense);

            assertEquals(mockExpense, ExpenseUtils.createExpense(mockExpense));
            MockConfigUtils.verifyRequest("api/expenses", MockConfigUtils.APIMethod.POST);
        }
    }

    @Test
    public void testChangeTagOfExpense() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long expenseId = 10L;
            Expense mockExpense = Mockito.mock(Expense.class);
            HashSet<UserExpense> mockUserExpenses = new HashSet<>(List.of(Mockito.mock(UserExpense.class)));
            when(mockExpense.getId()).thenReturn(expenseId);
            when(mockExpense.getUserExpenses()).thenReturn(mockUserExpenses);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/expenses/" + expenseId, MockConfigUtils.APIMethod.GET,
                    Expense.class, mockExpense);
            MockConfigUtils.mockEndPoint("api/expenses/" + expenseId, MockConfigUtils.APIMethod.PUT);
            MockConfigUtils.mockEndPoint("api/expenses/" + expenseId + "/debtors", MockConfigUtils.APIMethod.GET,
                    new GenericType<List<UserExpense>>() { }, List.copyOf(mockUserExpenses));

            String newTag = "Travel";
            ExpenseUtils.changeTagOfExpense(expenseId, newTag);
            verify(mockExpense).setTag(newTag);
            MockConfigUtils.verifyRequest("api/expenses/" + expenseId, MockConfigUtils.APIMethod.PUT);
        }
    }

    @Test
    public void testAddUserToExpense() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long expenseId = 10L;
            UserExpense mockUserExpense = Mockito.mock(UserExpense.class);

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/expenses/" + expenseId + "/debtors",
                    MockConfigUtils.APIMethod.POST, UserExpense.class, mockUserExpense);

            assertEquals(mockUserExpense, ExpenseUtils.addUserToExpense(expenseId, mockUserExpense));
            MockConfigUtils.verifyRequest("api/expenses/" + expenseId + "/debtors", MockConfigUtils.APIMethod.POST);
        }
    }

    @Test
    public void testRemoveExpense() {
        try (MockedStatic<ConfigUtils> config = Mockito.mockStatic(ConfigUtils.class)) {
            Long expenseId = 10L;

            MockConfigUtils.init(config);
            MockConfigUtils.mockEndPoint("api/expenses/" + expenseId, MockConfigUtils.APIMethod.DELETE);

            ExpenseUtils.removeExpense(expenseId);
            MockConfigUtils.verifyRequest("api/expenses/" + expenseId, MockConfigUtils.APIMethod.DELETE);
        }
    }
}
