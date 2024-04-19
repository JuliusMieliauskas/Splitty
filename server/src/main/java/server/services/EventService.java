package server.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import commons.Event;
import commons.Expense;
import commons.User;
import commons.exceptions.InvalidEventException;
import commons.exceptions.InvalidUserException;
import jakarta.transaction.Transactional;
import server.database.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.database.ExpenseRepository;
import server.database.UserExpenseRepository;
import server.database.UserRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseService expenseService;
    private final UserExpenseRepository userExpenseRepository;

    /**
     * autowired constructor
     * @param eventRepository
     * @param userRepository
     * @param expenseRepository
     * @param expenseService
     * @param userExpenseRepository
     */
    @Autowired
    public EventService(EventRepository eventRepository,
                        UserRepository userRepository,
                        ExpenseRepository expenseRepository,
                        ExpenseService expenseService,
                        UserExpenseRepository userExpenseRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
        this.expenseService = expenseService;
        this.userExpenseRepository = userExpenseRepository;
    }

    /**
     * Create a new event. This method will generate the invite code for the given event.
     * It will also add the event to the database.
     *
     * @param event The event to create, the id and inviteCode do not have to be set.
     * @return The created event, including the generated properties
     */
    @Transactional
    public Event createEvent(Event event) {
        eventValid(event);

        String inviteCode;
        do {
            inviteCode = generateInviteCode();
        } while (eventRepository.existsByInviteCode(inviteCode));
        event.setInviteCode(inviteCode);
        Event result = eventRepository.save(event);
        return result;
    }

    private String generateInviteCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder inviteCode = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            inviteCode.append(characters.charAt(rnd.nextInt(characters.length())));
        }
        return inviteCode.toString();
    }

    public Event getEventById(Long id) {
        return eventRepository.findById(id).orElseThrow(() -> new InvalidEventException("Event not found with id: " + id));
    }

    /**
     * Delete an event given by an id
     * @param id The id of the event to look for
     * @return A string containing the result of the action
     */
    @Transactional
    public String deleteEventById(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new InvalidEventException("Event not found with id: " + id);
        }
        Event event = eventRepository.getReferenceById(id);
        List<Long> expenseIds = event.getExpenses().stream().map(Expense::getId).toList();
        for (Long eid : expenseIds) {
            expenseService.deleteExpenseById(eid, false);
        }
        for (User user : event.getUsers()) {
            user.getEvents().remove(event);
            userRepository.save(user);
        }
        eventRepository.deleteById(id);
        return "Event with id " + id + " is deleted";
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * Delete all events in the db
     * @return A string containing the result of the action
     */
    @Transactional
    public String deleteAllEvents() {
        for (Event event : eventRepository.findAll()) {
            deleteEventById(event.getId());
        }
        return "All events deleted";
    }

    @Transactional
    public Set<User> getUsersOfEvent(Long id) {
        Event event = this.getEventById(id);
        return event.getUsers();
    }

    /**
     * adds a user to an event, duplicate users in an event may not occur
     * @param id the event id
     * @param user the user to add (object)
     * @return a success message
     */
    @Transactional
    public String addUserToEvent(Long id, User user) {
        if (user == null) {
            throw new InvalidUserException("user is null");
        }
        Long uid = user.getId();
        if (!userRepository.existsById(uid)) {
            throw new InvalidUserException("User is not a valid user");
        }
        user = userRepository.getReferenceById(user.getId()); // Make sure the user obj has correct events
        if (!eventRepository.existsById(id) ||
                // checks if user is already in the event (cannot be twice in event)
                eventRepository.getReferenceById(id).getUsers().contains(user)
        ) {
            throw new InvalidEventException("Event id is invalid or already contains user");
        }
        Event event = eventRepository.getReferenceById(id);
        event.addUser(user);
        event.updateLastActivity();
        eventRepository.save(event);
        return "User with name " + user.getUsername() + " is successfully added to the event with id " + id;
    }

    /**
     * Removes a user from an event
     * @param eid the event id
     * @param uid the user id to remove
     * @return message
     */
    @Transactional
    public String deleteUserFromEvent(Long eid, Long uid) {
        if (!userRepository.existsById(uid)) {
            throw new InvalidUserException("User id is invalid");
        }
        if (!eventRepository.existsById(eid) ||
                !eventRepository.findById(eid).get().getUsers()
                        .contains(userRepository.findById(uid).get())
        ) {
            throw new InvalidEventException("Event id is invalid or does not contain user");
        }
        User user = userRepository.getReferenceById(uid);
        Event event = eventRepository.getReferenceById(eid);
        event.getUsers().remove(user);
        event.updateLastActivity();
        eventRepository.save(event);
        user.getEvents().remove(event);
        userRepository.save(user);
        return "User with id " + uid + " is removed from event with id " + eid;
    }

    public Set<Expense> getExpensesOfEvent(Long id) {
        Event event = this.getEventById(id);
        return event.getExpenses();
    }

    /**
     * Get the event by giving the invite-code as identifier
     * @param code the invite code
     * @return the event corresponding with the invite code
     */
    public Event getEventByCode(String code) {
        if (code == null || !eventRepository.existsByInviteCode(code)) {
            throw new InvalidEventException("The invite code does not exist");
        }
        return eventRepository.getByInviteCode(code);
    }

    /**
     * Updates the event. Can be used for changing title or other properties of event
     * @param eventId
     * @param newEvent
     * @return Event
     */
    @Transactional
    public Event updateEvent(Long eventId, Event newEvent) {
        eventValid(newEvent);

        Event existingEvent = getEventById(eventId); // if it does not exist it will automatically throw exception
        existingEvent.setTitle(newEvent.getTitle());
        existingEvent.setInviteCode(newEvent.getInviteCode());
        existingEvent.updateLastActivity();
        return eventRepository.save(existingEvent);
    }

    /**
     * finds a user by username
     * @param name name to find
     * @return the user with the corresponding username
     */
    public User getByUsername(Long eventId, String name) {
        name = name.strip().toLowerCase();
        String finalName = name;
        List<User> res = getUsersOfEvent(eventId).stream().filter(x -> x.getUsername().equals(finalName)).toList();
        if (res.size() > 1) {
            throw new InvalidUserException("Multiple users in event with this name");
        } else if (res.isEmpty()) {
            throw new InvalidUserException("No user with username " + name + " in event with id " + eventId);
        }

        return res.get(0);
    }

    private void eventValid(Event event) {
        if (event == null) {
            throw new InvalidEventException("Event is null");
        }
        if (event.getTitle() == null || event.getTitle().isEmpty()) {
            throw new InvalidEventException("Title is required");
        }
        if (event.getCreationDate() == null) {
            throw new InvalidEventException("Creation date is required");
        }
    }

    /**
     * Return a JSON string of the specified event
     * @param event
     * @return json string of event, expenses, users, and userexpenses
     * @throws JsonProcessingException
     */
    public Map<String, Object> getEventJson(Event event) {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        List<Expense> eventExpenses = expenseRepository.findAll().stream().filter(o -> o.getEvent().equals(event)).toList();

        jsonMap.put("event", event);
        jsonMap.put("users", event.getUsers());
        jsonMap.put("expenses", eventExpenses);
        jsonMap.put("userExpenses", userExpenseRepository.findAll().stream()
                .filter(o -> eventExpenses.contains(o.getExpense())).toList());

        return jsonMap;
    }
}