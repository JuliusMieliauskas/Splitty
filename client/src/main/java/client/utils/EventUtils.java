package client.utils;

import commons.Event;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import commons.exceptions.FailedRequestException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public class EventUtils {
    /**
     * create a new event
     *
     * @param event the event to create
     * @return the created event
     */
    public static Event createEvent(Event event) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl()).path("api/events/").
                request(MediaType.APPLICATION_JSON).post(Entity.entity(event, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(Event.class);
    }

    /**
     * join an event method
     *
     * @param event to join
     * @param user  current user
     */
    public static void joinEvent(Event event, User user) throws FailedRequestException {
        String eid = event.getId().toString();
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl()).path("api/events/" + eid + "/users").
                request(MediaType.APPLICATION_JSON).post(Entity.entity(user, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
    }

    /**
     * gets the events of the current user
     *
     * @param user current user
     * @return list of the events user is a part of
     */
    public static List<Event> getEvents(User user) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/users/" + user.getId() + "/events").request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(new GenericType<List<Event>>() { });
    }

    /**
     * gets the events all users (for admin)
     *
     * @return list of all events
     */
    public static List<Event> getEvents() throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/events").request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(new GenericType<List<Event>>() { });
    }

    /**
     * remove given user from event
     *
     * @param event event to be left
     * @param user  user to be removed
     */
    public static void removeUser(Event event, User user) throws FailedRequestException {
        String eid = event.getId().toString();
        String uid = user.getId().toString();
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/events/" + eid + "/users/" + uid).request(MediaType.APPLICATION_JSON).delete();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
    }

    /**
     * returns an event by its invite code
     *
     * @param inviteCode
     * @return the event
     */
    public static Event getEventByCode(String inviteCode) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/events/invite-code/" + inviteCode).request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(Event.class);
    }

    /**
     * returns an event by its id
     *
     * @param eventId
     * @return the event
     */
    public static Event getEventById(Long eventId) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/events/" + eventId).request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(Event.class);
    }

    /**
     * Gets all expenses of an event
     *
     * @param eventId id of the event of the expenses
     * @return set of expenses associated with the id.
     */
    public static List<Expense> getExpensesOfEvent(Long eventId) throws FailedRequestException {
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/events/" + eventId + "/expenses").request(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(new GenericType<List<Expense>>() { });
    }

    /**
     * checks if user is not involved in any expenses as debtor
     */
    public static boolean isUserInvolvedInExpenses(Long eventId, Long userId) throws FailedRequestException {
        List<Expense> expenses = getExpensesOfEvent(eventId);
        for (Expense expense : expenses) {
            if (Objects.equals(expense.getOriginalPayer().getId(), userId)) {
                return true; // The user is the original payer of some expense
            }
            List<UserExpense> userExpenses = (new ExpenseUtils()).getDebtorsOfExpense(expense.getId());
            if (userExpenses.stream().anyMatch(userExpense -> userExpense.getDebtor().getId().equals(userId))) {
                return true; // User is debtor of some expense
            }
        }
        return false;
    }

    /**
     * Get all participants of an Event
     *
     * @param eid the event id
     * @return the set of participants
     */
    public static Set<User> getParticipants(long eid) throws FailedRequestException {
        Response response = ConfigUtils.client
                .target(ConfigUtils.getServerUrl()).path("api/events/" + eid + "/users")
                .request(APPLICATION_JSON)
                .accept(APPLICATION_JSON)
                .get();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
        return response.readEntity(new GenericType<Set<User>>() {
        });
    }

    /**
     * Gets all the users in an event
     *
     * @param eventId id of the event
     * @return all users of event with eventId as id.
     */
    public static List<User> getUsersOfEvent(Long eventId) throws FailedRequestException {
        WebTarget webTarget = ConfigUtils.client.target(ConfigUtils.getServerUrl()).path("api/events/" + eventId + "/users");
        Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }

        return response.readEntity(new GenericType<List<User>>() { });
    }

    /**
     * Replaces the event with the event passed as parameter
     * @param event event that'll be passed on to the database
     * @return The ConfigUtils.getServerUrl()'s response to the update request.
     */
    public static void updateEvent(Event event) throws FailedRequestException {
        Long eventId = event.getId();
        Response response = ConfigUtils.client.target(ConfigUtils.getServerUrl())
                .path("api/events/" + eventId)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(event, MediaType.APPLICATION_JSON));

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
    }

    /**
     * deletes event form database
     * @param event event to be left
     * @return The ConfigUtils.getServerUrl()'s response to the delete request.
     */
    public static void deleteEvent(Event event) throws FailedRequestException {
        String eid = event.getId().toString();
        Response response = ConfigUtils.client //
                .target(ConfigUtils.getServerUrl()).path("api/events/" + eid) //
                .request(APPLICATION_JSON) //
                .accept(APPLICATION_JSON) //
                .delete();

        if (response.getStatus() != 200) {
            throw new FailedRequestException(response.getStatus(), response.readEntity(String.class));
        }
    }

    /**
     * Get a json string of the given event
     * @param eventId event id
     * @return String (json)
     */
    public static String getJson(Long eventId) {
        WebTarget webTarget = ConfigUtils.client.target(ConfigUtils.getServerUrl()).path("api/events/" + eventId + "/json");
        Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() != 200) {
            ConfigUtils.client.close();
            throw new RuntimeException("Http error code:" + response.getStatus());
        }
        String json = response.readEntity(String.class);
        return json;
    }
}
