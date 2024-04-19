package server.api;

import java.util.List;
import java.util.Set;

import commons.EventUpdate;
import commons.UserExpense;
import commons.exceptions.InvalidUserException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import commons.User;
import commons.Event;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;
import server.services.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    public UserController(UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping(path = { "", "/" })
    public ResponseEntity<List<User>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    /**
     * Creates a new user
     * @param user the body of the request with the new user info
     * @return the new user info or a bad request
     */
    @PostMapping(path = { "", "/" })
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User saved = userService.createUser(user);
        return ResponseEntity.ok(saved);
    }

    /**
     * Deletes all users
     * @return either a bad request or a completion message
     */
    //Execute order 66, delete all users >:)
    @DeleteMapping(path = {"","/"})
    public ResponseEntity<String> deleteAllUsers() {
        return ResponseEntity.ok(userService.deleteAllUsers());
    }

    /**
     * gets a user by their id
     * @param id id of user you want to retrieve
     * @return either a bad request or the user you wanted
     */
    @GetMapping(path = "/{id}")
    public ResponseEntity<User> getById(@PathVariable("id") long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    /**
     * Updates a User
     * @param id id of the user to update
     * @param user the body of the request with the new information
     * @return the json for the newly updated user or bad request
     */
    @PutMapping(path = {"/{id}"})
    public ResponseEntity<User> updateUser(@PathVariable("id") long id, @RequestBody User user) {
        User updated = userService.updateUser(id, user);
        for (Event e : updated.getEvents()) {
            messagingTemplate.convertAndSend("/api/events/websocket/" + e.getId(),
                    new EventUpdate(EventUpdate.Action.UPDATED_USER, id));
        }
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a specific user
     * @param id id of user to delete
     * @return either bad request or completion message
     */
    @DeleteMapping(path = {"/{id}"})
    public ResponseEntity<String> deleteUser(@PathVariable("id") long id) {
        Set<Event> events = userService.getEvents(id);
        String deleted = userService.deleteUser(id);
        for (Event e : events) {
            messagingTemplate.convertAndSend("/api/events/websocket/" + e.getId(),
                    new EventUpdate(EventUpdate.Action.REMOVED_USER, id));
        }
        return ResponseEntity.ok(deleted);
    }

    /**
     * return all the events of a user
     * @param id id of user
     * @return all of that user's events
     */
    @GetMapping(path = { "/{id}/events/", "/{id}/events"})
    public ResponseEntity<Set<Event>> getEvents(@PathVariable("id") long id) {
        return ResponseEntity.ok(userService.getEvents(id));
    }

    /**
     * return all the expenses of a user
     * @param id id of user
     * @return all of that user's expenses
     */
    @GetMapping(path = {"/{id}/expenses/", "/{id}/expenses"})
    public ResponseEntity<Set<UserExpense>> getExpenses(@PathVariable("id") long id) {
        return ResponseEntity.ok(userService.getExpenses(id));
    }

    /**
     * Handler for exception, so that if request fails, the client gets an informative message about
     * what went wrong
     * @param ex
     * @return String ( error message )
     */
    @ExceptionHandler({InvalidUserException.class})
    public ResponseEntity<String> handleIllegalArgumentException(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getClass().getCanonicalName() + ":\n" + ex.getMessage());
    }

    /**
     * Handler for exception, so that if request fails, the client gets an informative message about
     * what went wrong
     * @param ex
     * @return String ( error message )
     */
    @ExceptionHandler({DataIntegrityViolationException.class})
    public ResponseEntity<String> handleDataIntegrityViolationException(Exception ex) {
        return ResponseEntity.badRequest().body("User still has dependencies:\n" + ex.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<String> handleException(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getClass().getCanonicalName() + ":\n" + ex.getMessage());
    }

}