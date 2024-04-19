package client.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import commons.exceptions.FailedRequestException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;

import jakarta.ws.rs.client.WebTarget;

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;

import java.util.HashSet;
import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public class ExpenseUtils {

    /**
     * Get all the debtors of certain expense
     *
     * @param expenseId the id of the expense
     * @return A list with all the found userExpense relations
     */
    public static List<UserExpense> getDebtorsOfExpense(Long expenseId) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/expenses/" + expenseId + "/debtors").request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(new GenericType<List<UserExpense>>() {
        });
    }

    /**
     * Find a certain expense using the id
     *
     * @param id The id of the expense to look for
     * @return The found expense
     */
    public static Expense getExpenseById(Long id) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/expenses/" + id).request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }

        Expense expense = response.readEntity(Expense.class);

        HashSet<UserExpense> userExpenses = new HashSet<>(getDebtorsOfExpense(id));

        expense.setUserExpenses(userExpenses);

        return expense;
    }

    /**
     * Sends api request to create a new expense
     *
     * @param expense object to add
     * @return the expense
     */
    public static Expense createExpense(Expense expense) throws FailedRequestException {
        Response response = ConfigUtils.client
                .target(ConfigUtils.getServerUrl()).path("api/expenses")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .post(Entity.entity(expense, APPLICATION_JSON));
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(Expense.class);
    }

    /**
     * Changes the tag of an event in the database
     *
     * @param expenseId the id of the expense getting its tag changed
     * @param newTag    the new tag of the expense
     */
    public static void changeTagOfExpense(Long expenseId, String newTag) throws FailedRequestException {
        try {
            Expense expense = getExpenseById(expenseId);
            expense.setTag(newTag);
            WebTarget webTarget = ConfigUtils.client.target(ConfigUtils.getServerUrl()).path("api/expenses/" + expenseId);
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            String expenseJson = mapper.writeValueAsString(expense);
            Response response = webTarget.request(MediaType.APPLICATION_JSON).put(Entity.entity(expenseJson, MediaType.APPLICATION_JSON));
            if (response.getStatus() != 200) {
                throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the given expense in the backend
     * Relies on the given expense having a correct id
     */
    public static void updateExpense(Expense expense) throws FailedRequestException {
        Response response = ClientBuilder.newClient(new ClientConfig())
                .target(ConfigUtils.getServerUrl()).path("api/expenses/" + expense.getId())
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .put(Entity.entity(expense, APPLICATION_JSON));
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
    }

    /**
     * Makes a ConfigUtils.getServerUrl() call to add a user to an expense
     *
     * @param expenseId   id of the expense
     * @param userExpense the userexpense obj to add
     * @return the obj (it has the id that was auto generated)
     */
    public static UserExpense addUserToExpense(Long expenseId, UserExpense userExpense) throws FailedRequestException {
        Response response = ConfigUtils.client
                .target(ConfigUtils.getServerUrl()).path("api/expenses/" + expenseId + "/debtors")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .post(Entity.entity(userExpense, APPLICATION_JSON));
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(UserExpense.class);
    }

    /**
     * Deletes user from expense
     * @param expenseId id of expense user is getting deleted from
     * @param user the user egetting removed
     * @return response
     * @throws FailedRequestException when request fails
     */
    public static Response deleteUserFromExpense(Long expenseId, User user) throws FailedRequestException {
        Response response = ClientBuilder.newClient(new ClientConfig())
                .target(ConfigUtils.getServerUrl())
                .path("api/expenses/" + expenseId + "/debtors/" + user.getId())
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .delete();
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response;
    }

    /**
     * removes expesne from the database
     *
     * @param expenseId the id of the expense getting its tag changed
     * @return The ConfigUtils.getServerUrl()'s response to the remove request.
     */
    public static void removeExpense(Long expenseId) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/expenses/" + expenseId.toString()).request(MediaType.APPLICATION_JSON).delete();
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
    }

    /**
     * Changes all userExpense of an expense
     */
    public static void changeUserExpense(Long expenseId, List<UserExpense> userExpenseList) throws FailedRequestException {
        for (UserExpense ue : getDebtorsOfExpense(expenseId)) {
            Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                    .path("api/expenses/" + expenseId + "/debtors/" + ue.getDebtor().getId()).request(MediaType.APPLICATION_JSON).delete();
            if (response.getStatus() != 200) {
                throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
            }
        }
        for (UserExpense ue : userExpenseList) {
            addUserToExpense(expenseId, ue);
        }
    }

    /**
     * Edit a UserExpense
     * @param newUserExpense the new userExpense
     */
    public static void editDebtorOfExpense(UserExpense newUserExpense) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/expenses/" + newUserExpense.getExpense().getId() + "/debtors/" + newUserExpense.getDebtor().getId())
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(newUserExpense, APPLICATION_JSON));

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
    }
}