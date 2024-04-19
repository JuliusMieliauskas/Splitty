package server.services;

import commons.Event;
import commons.User;
import commons.UserExpense;
import commons.exceptions.InvalidUserException;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import server.database.EventRepository;
import server.database.ExpenseRepository;
import server.database.UserRepository;

import java.util.List;
import java.util.Set;

@Service
public class UserService {
    private final UserRepository repo;
    private final EventRepository eventRepository;
    private final ExpenseRepository expenseRepository;

    /**
     * Autowired constructor for userService
     */
    @Autowired
    public UserService(UserRepository repo,
                       EventRepository eventRepository,
                       ExpenseRepository expenseRepository) {
        this.repo = repo;
        this.eventRepository = eventRepository;
        this.expenseRepository = expenseRepository;
    }

    public List<User> getAll() {
        return repo.findAll();
    }

    /**
     * Creates a new user
     * @param user the body of the request with the new user info
     * @return the new user info or a bad request
     */
    @Transactional
    public User createUser(User user) {
        if (user == null ||
                isNullOrEmpty(user.getUsername())) {
            throw new InvalidUserException("User is not a valid user");
        }
        user.setIban(null);
        user.setEmail(null);
        User saved = repo.save(user);
        return saved;
    }

    /**
     * Delete a user from the repo
     * @param id the expense id
     * @return either null or conformation message
     */
    @Transactional
    public String deleteUser(long id) {
        checkSafeDeleteUser(id);
        User user = repo.getReferenceById(id);
        // Let user leave all events
        for (Event event : user.getEvents()) {
            event.getUsers().remove(user);
            event.updateLastActivity();
            eventRepository.save(event);
        }
        repo.deleteById(id);
        return "Expense with id " + id + " is successfully deleted";
    }

    /**
     * If user is not safe to delete, throw InvalidUserException or DataIntegrityViolationException
     * @throws InvalidUserException If user id is invalid
     * @throws DataIntegrityViolationException if user can't be deleted
     */
    private void checkSafeDeleteUser(long id) {
        if (id < 0 || !repo.existsById(id)) {
            throw new InvalidUserException("User id is invalid");
        }
        User user = repo.getReferenceById(id);
        if (!user.getUserExpenses().isEmpty()) {
            throw new DataIntegrityViolationException("User still has expenses");
        }
        if (!expenseRepository.findByOriginalPayer(user).isEmpty()) {
            throw new DataIntegrityViolationException("User still owns expenses");
        }
    }

    /**
     * return all the events of a user
     * @param id id of user
     * @return all of that user's events
     */
    public Set<Event> getEvents(long id) {
        if (id < 0 || !repo.existsById(id)) {
            throw new InvalidUserException("Provided id is invalid");
        }
        Set<Event> events = repo.findById(id).get().getEvents();
        return events;
    }

    /**
     * return all the expenses of a user
     * @param id id of user
     * @return all of that user's expenses
     */
    public Set<UserExpense> getExpenses(long id) {
        if (id < 0 || !repo.existsById(id)) {
            throw new InvalidUserException("Provided id is invalid");
        }
        return repo.findById(id).get().getUserExpenses();
    }

    /**
     * Deletes all users
     * @return a string containing the result
     */
    @Transactional
    public String deleteAllUsers() {
        for (User user : repo.findAll()) { // Check if it's possible to delete all users
            checkSafeDeleteUser(user.getId());
        }
        for (User user : repo.findAll()) {
            deleteUser(user.getId());
        }
        return "All Users Deleted";
    }

    /**
     * gets a user by their id
     * @param id id of user you want to retrieve
     * @return the requested user
     */
    @Transactional
    public User getById(long id) {
        if (id < 0 || !repo.existsById(id)) {
            throw new InvalidUserException("Provided id is invalid");
        }
        User res = repo.findById(id).get();
        Hibernate.initialize(res);
        return res;
    }

    /**
     * Updates a User
     * @param id id of the user to update
     * @param user the body of the request with the new information
     * @return the json for the newly updated user
     */
    @Transactional
    public User updateUser(long id, User user) {
        if (id < 0 || !repo.existsById(id) || user == null) {
            throw new InvalidUserException("Provided id is invalid");
        }

        if (isNullOrEmpty(user.getUsername())) {
            throw new InvalidUserException("User is not a valid user");
        }

        User updatedUser = repo.findById(id).get();
        updatedUser.setUsername(user.getUsername());

        return repo.save(updatedUser);
    }

    /**
     * a check for if the string is either null or empty
     * @param s string to check
     * @return true of false boolean
     */
    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
