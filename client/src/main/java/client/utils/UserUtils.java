package client.utils;

import commons.User;
import commons.UserExpense;
import commons.exceptions.FailedRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public class UserUtils {

    /**
     * retrieves all users from the database
     */
    public static List<User> getAllUsers() throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/users").request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(new GenericType<List<User>>() {
        });
    }

    /**
     * Create a new user with given username, or if there already exists a user like that in db, return that user
     *
     * @param username the Username, will be checked for validity
     * @return the User
     */
    public static User createOrGet(String username) throws FailedRequestException {
        if (username.isEmpty()) {
            throw new FailedRequestException(400, "Username cannot be empty");
        }
        if (EventUtils.getUsersOfEvent(ConfigUtils.getCurrentEvent().getId())
                .stream().map(User::getUsername).toList().contains(username)) {
            throw new FailedRequestException(400, "User " + username + " is already in event");
        }
        return UserUtils.createUser(new User(username));
    }

    /**
     * Renames the given user
     */
    public static void renameUser(Long id, String newUserName) throws FailedRequestException {
        if (newUserName.isEmpty()) {
            throw new FailedRequestException(400, "Username cannot be empty");
        }
        if (EventUtils.getUsersOfEvent(ConfigUtils.getCurrentEvent().getId())
                .stream().map(User::getUsername).toList().contains(newUserName)) {
            throw new FailedRequestException(400, "User " + newUserName + " is already in event");
        }
        User old = getUserById(id.intValue());
        old.setUsername(newUserName);
        updateUser(id, old);
    }

    /**
     * Get user by id
     *
     * @param userId the id of the user
     */

    public static User getUserById(int userId) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/users/" + userId).request(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(User.class);
    }

    /**
     * get the user object corresponding to the username
     * @param userName the username
     * @return the user from the database
     */
    public static User getUserByName(String userName) throws FailedRequestException {
        Response response = ConfigUtils.client
                .target(ConfigUtils.getServerUrl()).path("api/events/" + ConfigUtils.getCurrentEvent().getId() + "/username/" + userName)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get();
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(User.class);
    }

    /**
     * Create a new user
     */
    public static User createUser(User user) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl()).path("api/users").
                request(MediaType.APPLICATION_JSON).post(Entity.entity(user, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(User.class);
    }

    /**
     * Deletes a user
     */
    public static void deleteUser(User user) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/users/" + user.getId()).request(MediaType.APPLICATION_JSON).delete();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
    }

    /**
     * Gets users expenses
     */
    public static List<UserExpense> getUserExpenses(User user) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/users/" + user.getId() + "/expenses").request(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(new GenericType<List<UserExpense>>() { });
    }

    /**
     * Calls the update user to change it in the database
     * @param uid the user id
     * @param user the user object to change it to
     * @return the updated user
     */
    public static User updateUser(long uid, User user) throws FailedRequestException {
        Response response = ConfigUtils.client
                .target(ConfigUtils.getServerUrl())
                .path("api/users/" + uid)
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .put(Entity.entity(user, APPLICATION_JSON));
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(User.class);
    }

    /**
     * Verify whether the admin password is correct
     * @param pass The attempt
     */
    public static boolean verifyAdminPassword(String pass) throws FailedRequestException {
        var requestBody = Entity.entity(pass, MediaType.APPLICATION_JSON);
        var response = ConfigUtils.client
                .target(ConfigUtils.getServerUrl())
                .path("api/admin")
                .request(MediaType.APPLICATION_JSON)
                .post(requestBody);
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(Boolean.class);
    }

    /**
     * updates userexpense in the database
     * @param userExpense the new userexpense
     * @return The ConfigUtils.getServerUrl()'s response to the update request
     */
    public static Response updateUserExpense(UserExpense userExpense) {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/expenses/" + userExpense.getExpense().getId() + "/debtors/" + userExpense.getDebtor().getId())
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(userExpense, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            System.out.println(response.getHeaders());
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response;
    }
}


