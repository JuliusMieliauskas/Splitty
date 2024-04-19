package server.api;

import java.util.Map;
import java.util.Set;
import commons.Event;
import commons.EventUpdate;
import commons.Expense;
import commons.User;
import commons.exceptions.InvalidEventException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import commons.exceptions.InvalidUserException;
import server.services.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final SimpMessagingTemplate messagingTemplate; // For websocket

    /**
     * Autowired constructor
     */
    @Autowired
    public EventController(EventService eventService,
                           SimpMessagingTemplate messagingTemplate) {
        this.eventService = eventService;
        this.messagingTemplate = messagingTemplate;
    }


    /**
     * Returns all Events that are stored in the database
     * @return List<Event>
     */
    @GetMapping
    public ResponseEntity<List<Event>> getAllEvents() {
        List<Event> events = eventService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Endpoint takes Event as body param, which contains already the creator(id)
     * Creates an Event in the database, with createdBy pointing to creatorId
     * @param event obj
     * @return Event
     */
    @PostMapping(path = {"","/"})
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        Event newEvent = eventService.createEvent(event);
        return ResponseEntity.ok(newEvent);
    }

    /**
     * Delete all events that exist
     * @return success message or bad request if no events exist
     */
    @DeleteMapping(path = {"","/"})
    public ResponseEntity<String> deleteAllEvents() {
        List<Event> events = eventService.getAllEvents();
        String deleted = eventService.deleteAllEvents();
        for (Event e : events) {
            messagingTemplate.convertAndSend("/api/events/websocket/" + e.getId(),
                    new EventUpdate(EventUpdate.Action.DELETED_EVENT));
        }
        return ResponseEntity.ok(deleted);
    }

    /**
     * Returns Event with specified ID
     * @param eventId The id of the event to look for
     * @return Event A responseEntity containing the event in case of success
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<Event> getEventById(@PathVariable Long eventId) {
        Event event = eventService.getEventById(eventId);
        return event != null ? ResponseEntity.ok(event) : ResponseEntity.notFound().build();
    }

    /**
     * Updates the event. Can be used for changing title or other properties of event
     * @param eventId
     * @param newEvent
     * @return Event
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<Event> updateEvent(@PathVariable Long eventId, @RequestBody Event newEvent) {
        Event updated = eventService.updateEvent(eventId, newEvent);
        messagingTemplate.convertAndSend("/api/events/websocket/" + eventId,
                new EventUpdate(EventUpdate.Action.UPDATED_EVENT));
        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes an event given by event id
     * @param eventId event to delete
     * @return String message
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<String> deleteEventById(@PathVariable Long eventId) {
        String deleted = eventService.deleteEventById(eventId);
        messagingTemplate.convertAndSend("/api/events/websocket/" + eventId,
                new EventUpdate(EventUpdate.Action.DELETED_EVENT));
        return ResponseEntity.ok(deleted);
    }

    /**
     * Get json String of an event
     * @param eventId
     * @return json responseEntity
     */
    @GetMapping("/{eventId}/json")
    public ResponseEntity<Map<String, Object>> getEventJson(@PathVariable Long eventId) {
        Event event = eventService.getEventById(eventId);
        Map<String, Object> json = eventService.getEventJson(event);
        return ResponseEntity.ok(json);
    }

    /**
     * Returns all users associated with event
     * @return List<User>
     */
    @GetMapping("/{eventId}/users")
    public ResponseEntity<Set<User>> getUsersOfEvent(@PathVariable Long eventId) {
        Set<User> users = eventService.getUsersOfEvent(eventId);
        return ResponseEntity.ok(users);
    }

    /**
     * adds a user to the specified event
     * @param eventId the event
     * @return message
     */
    @PostMapping("/{eventId}/users")
    public ResponseEntity<String> addUserToEvent(@PathVariable Long eventId, @RequestBody User user) {
        String saved = eventService.addUserToEvent(eventId, user);
        messagingTemplate.convertAndSend("/api/events/websocket/" + eventId,
                new EventUpdate(EventUpdate.Action.ADDED_USER, user.getId()));
        return ResponseEntity.ok(saved);
    }

    /**
     * Remove specified user from specified event (by id)
     * @return String containing success message
     */
    @DeleteMapping("/{eventId}/users/{userId}")
    public ResponseEntity<String> deleteUserFromEvent(@PathVariable Long eventId, @PathVariable Long userId) {
        String deleted = eventService.deleteUserFromEvent(eventId, userId);
        messagingTemplate.convertAndSend("/api/events/websocket/" + eventId,
                new EventUpdate(EventUpdate.Action.REMOVED_USER, userId));
        return ResponseEntity.ok(deleted);
    }

    /**
     * Returns all expenses associated with event
     * @return List<User>
     */
    @GetMapping("/{eventId}/expenses")
    public ResponseEntity<Set<Expense>> getExpensesOfEvent(@PathVariable Long eventId) {
        Set<Expense> expenses = eventService.getExpensesOfEvent(eventId);
        return ResponseEntity.ok(expenses);
    }

    /**
     * Get the event by giving invite-code
     * @param code the unique invite-code
     * @return the corresponding Event
     */
    @GetMapping("/invite-code/{code}")
    public ResponseEntity<Event> getEventByCode(@PathVariable String code) {
        Event event = eventService.getEventByCode(code);
        return ResponseEntity.ok(event);
    }

    /**
     * finds a user by username
     * @param eventId the event to search in
     * @param name name to find
     * @return the user with the corresponding username
     */
    @GetMapping("/{eventId}/username/{name}")
    public ResponseEntity<User> getByUsername(@PathVariable("eventId") Long eventId, @PathVariable("name") String name) {
        return ResponseEntity.ok(eventService.getByUsername(eventId, name));
    }

    /**
     * Handler for exception, so that if request fails, the client gets an informative message about
     * what went wrong
     * @param ex The exception
     * @return String ( error message )
     */
    @ExceptionHandler({InvalidUserException.class, InvalidEventException.class})
    public ResponseEntity<String> handleIllegalArgumentException(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getClass().getCanonicalName() + ":\n" + ex.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<String> handleException(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getClass().getCanonicalName() + ":\n" + ex.getMessage());
    }
}