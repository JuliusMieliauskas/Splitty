package server.services;

import commons.Event;
import commons.Expense;
import commons.User;
import commons.UserExpense;
import commons.exceptions.InvalidEventException;
import commons.exceptions.InvalidExpenseException;
import commons.exceptions.InvalidUserException;
import commons.exceptions.InvalidUserExpenseException;
import jakarta.transaction.Transactional;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import server.database.EventRepository;
import server.database.ExpenseRepository;
import server.database.UserExpenseRepository;
import server.database.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class ExpenseService {
    private final ExpenseRepository repo;
    private final UserExpenseRepository userExpenseRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    /**
     * Autowired constructor for ExpenseService
     */
    @Autowired
    public ExpenseService(ExpenseRepository repo,
                          UserExpenseRepository userExpenseRepository,
                          UserRepository userRepository,
                          EventRepository eventRepository) {
        this.repo = repo;
        this.userExpenseRepository = userExpenseRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * get all the expenses from the repo
     * @return all expenses
     */
    public List<Expense> getAllExpenses() {
        return repo.findAll();
    }

    /**
     * get a certain expense from the repo
     * @param id the id of the expense
     * @return either null or the found expense
     */
    public Expense getExpenseById(long id) {
        if (id < 0 || !repo.existsById(id)) {
            throw new InvalidExpenseException("Expense id is invalid");
        }
        return repo.findById(id).get();
    }

    /**
     * save an expense in the repo
     * @param expense the expense object to add
     * @return either null or the saved expense
     */
    @Transactional
    public Expense createExpense(Expense expense) {
        if (expense == null) {
            throw new InvalidExpenseException("Expense is null");
        }
        checkUserInRepository(expense.getOriginalPayer());
        checkEventInRepository(expense.getEvent());

        if (nullOrLessThan0(expense.getAmount(), false) ||
                isNullOrEmpty(expense.getTitle()) ||
                expense.getCreationDate() == null ||
                !eventRepository.getReferenceById(expense.getEvent().getId()).getUsers().contains(expense.getOriginalPayer())
        ) {
            throw new InvalidExpenseException("Invalid expense");
        }
        Event event = expense.getEvent();
        event.updateLastActivity();
        eventRepository.save(event);
        return repo.save(expense);
    }

    private void checkUserInRepository(User user) {
        if (!(user != null &&
                userRepository.findById(user.getId()).isPresent())) {
            throw new InvalidUserException("Invalid user");
        }
    }

    private void checkEventInRepository(Event event) {
        if (!(event != null &&
                eventRepository.findById(event.getId()).isPresent())) {
            throw new InvalidEventException("Invalid event");
        }
    }

    /**
     * update all info about an expense
     * @param id the id of the expense
     * @param expense the new expense data
     * @return either null or the updated expense
     */
    @Transactional
    public Expense updateExpense(long id, Expense expense) {
        if (expense == null) {
            throw new InvalidExpenseException("Expense is null");
        }
        if (id < 0 || !repo.existsById(id)) {
            throw new InvalidExpenseException("Expense id is invalid");
        }
        checkUserInRepository(expense.getOriginalPayer());
        checkEventInRepository(expense.getEvent());
        if (nullOrLessThan0(expense.getAmount(), false) ||
                isNullOrEmpty(expense.getTitle()) ||
                expense.getCreationDate() == null ||
                !eventRepository.getReferenceById(expense.getEvent().getId()).getUsers().contains(expense.getOriginalPayer())
        ) {
            throw new InvalidExpenseException("New expense is invalid");
        }

        Expense updatedExpense = repo.findById(id).get();
        if (!updatedExpense.getEvent().equals(expense.getEvent())) {
            throw new InvalidExpenseException("Expense does not belong to the same event as original");
        }

        updatedExpense.setTitle(expense.getTitle());
        updatedExpense.setTag(expense.getTag());
        updatedExpense.setOriginalPayer(expense.getOriginalPayer());
        updatedExpense.setAmount(expense.getAmount());

        repo.save(updatedExpense);

        Event event = expense.getEvent();
        event.updateLastActivity();
        eventRepository.save(event);
        return updatedExpense;
    }

    /**
     * Delete an expense from the repo
     * @param id the expense id
     * @return either null or conformation message
     */
    @Transactional
    public String deleteExpenseById(long id) {
        return deleteExpenseById(id, true);
    }

    /**
     * Delete an expense from the repo
     * @param id the expense id
     * @param updateLastActivity Only update the event last activity if this is true
     * @return either null or conformation message
     */
    public String deleteExpenseById(long id, boolean updateLastActivity) {
        if (id < 0 || !repo.existsById(id)) {
            throw new InvalidExpenseException("Expense id is invalid");
        }
        Expense expense = repo.getReferenceById(id);
        for (UserExpense userExpense : expense.getUserExpenses()) {
            userExpenseRepository.deleteById(userExpense.getId());
        }
        if (updateLastActivity) {
            Event event = expense.getEvent();
            event.updateLastActivity();
            eventRepository.save(event);
        }
        repo.deleteById(id);
        return "Expense with id " + id + " is successfully deleted";
    }

    /**
     * Delete all expenses from repo
     * @return either null if there are no expenses or completion message
     */
    @Transactional
    public String deleteAllExpenses() {
        for (Expense e : repo.findAll()) {
            deleteExpenseById(e.getId());
        }
        return "All Expenses Deleted";
    }

    /**
     * a check for if the string is either null or empty
     * @param s string to check
     * @return true of false boolean
     */
    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Add a debtor/user to expense by adding to the userExpense repo
     * @param eid the id of the expense
     * @param userExpense the userExpense object to save
     * @return either null or the saved userExpense
     */
    @Transactional
    public UserExpense addUserToExpense(long eid, UserExpense userExpense) {
        if (userExpense == null) {
            throw new InvalidUserExpenseException("UserExpense is null");
        }
        checkUserInRepository(userExpense.getDebtor());
        if (userExpense.getExpense() == null ||
                !userExpense.getExpense().getId().equals(eid) ||
                repo.findById(eid).isEmpty() || // if expense doesn't exist
                nullOrLessThan0(userExpense.getPaidAmount(), true) ||
                nullOrLessThan0(userExpense.getTotalAmount(), false) ||
                userExpense.getPaidAmount() > userExpense.getTotalAmount()
        ) {
            throw new InvalidUserExpenseException("UserExpense is not valid");
        }
        if (// if debtor is creditor:
            userExpense.getExpense().getOriginalPayer().equals(userExpense.getDebtor()) ||
            // if debtor is already part of this expense:
            userExpenseRepository.findByExpense(userExpense.getExpense()).stream()
                    .anyMatch(o -> o.getDebtor().equals(userExpense.getDebtor()))
        ) {
            throw new InvalidUserExpenseException("User is already a part of expense");
        }
        userExpenseRepository.save(userExpense);
        Event event = userExpense.getExpense().getEvent();
        event.updateLastActivity();
        eventRepository.save(event);
        return userExpense;
    }

    boolean nullOrLessThan0(Double x, boolean allowEquals) {
        return x == null || x < 0 || (x == 0 && !allowEquals);
    }

    /**
     * Edit a debtor/user to expense by adding to the userExpense repo
     * @param exid the id of the expense
     * @param userExpense the userExpense object to save
     * @return either null or the saved userExpense
     */
    @Transactional
    public UserExpense updateUserExpense(long exid, long uid, UserExpense userExpense) {
        if (userExpense == null) {
            throw new InvalidUserExpenseException("UserExpense is null");
        }
        if (!repo.existsById(exid)) {
            throw new InvalidUserExpenseException("User is not a part of expense");
        }

        Optional<UserExpense> optionalUserExpense = getUserExpenseByExpenseAndUser(exid, uid);
        UserExpense existingUserExpense = null;
        if (optionalUserExpense.isPresent()) {
            existingUserExpense = optionalUserExpense.get();
        }

        if (existingUserExpense == null ||
                !userExpense.getDebtor().equals(existingUserExpense.getDebtor()) ||
                !userExpense.getExpense().equals(existingUserExpense.getExpense()) ||
                nullOrLessThan0(userExpense.getPaidAmount(), true) ||
                nullOrLessThan0(userExpense.getTotalAmount(), false)
        ) {
            throw new InvalidUserExpenseException("UserExpense is not valid");
        }

        existingUserExpense.setPaidAmount(userExpense.getPaidAmount());
        existingUserExpense.setTotalAmount(userExpense.getTotalAmount());
        userExpenseRepository.save(existingUserExpense);
        Event event = existingUserExpense.getExpense().getEvent();
        event.updateLastActivity();
        eventRepository.save(event);
        return existingUserExpense;
    }

    /**
     * Get a user expense link from an event id and userExpenseId
     * @param eid The event the userExpense should be a part of
     * @param ueid The id of the userExpense
     * @return The userExpense
     * @throws InvalidUserExpenseException If it is not found
     */
    public UserExpense getUserExpense(long eid, long ueid) {
        if (!userExpenseRepository.existsById(ueid)) {
            throw new InvalidUserExpenseException("User-expense id is invalid");
        }
        UserExpense res = userExpenseRepository.getReferenceById(ueid);
        if (res.getExpense().getId() != eid) {
            throw new InvalidUserExpenseException("User-expense is not a part of event");
        }
        return (UserExpense) Hibernate.unproxy(res); // TBH don't know why you have to unproxy
    }

    /**
     * Gets UserExpense using user id and expense id
     * @param exid id of the expense related to UserExpense
     * @param uid id of the user related to UserExpense
     * @return Optional of a UserExpense matching ex_id and u_id
     */
    public Optional<UserExpense> getUserExpenseByExpenseAndUser(long exid, long uid) {
        List<UserExpense> allUserExpenses = userExpenseRepository.findAll();

        return allUserExpenses.stream().filter(
                user_expense -> user_expense.getExpense().getId().equals(exid) && user_expense.getDebtor().getId().equals(uid))
                .findAny();
    }

    /**
     * Delete a user from expense by deleting row from userExpense repo
     * @param eid the expense id
     * @param uid the id of the user to be deleted
     * @return either null or a return message
     */
    @Transactional
    public String deleteUserFromExpense(long eid, long uid) {
        Optional<UserExpense> userExpenseOptional = getUserExpenseByExpenseAndUser(eid, uid);
        if (userExpenseOptional.isEmpty()) {
            throw new InvalidUserExpenseException("User is not a part of expense");
        }
        UserExpense userExpense = userExpenseOptional.get();
        userExpenseRepository.deleteById(userExpense.getId());
        Event event = getExpenseById(eid).getEvent();
        event.updateLastActivity();
        eventRepository.save(event);
        return "User with id " + uid + " is successfully deleted from expense with id " + eid;
    }

    /**
     * get all the debtors from one expense
     * @param id the expense id
     * @return a set of users, who are in debt in this expense
     */
    @Transactional
    public Set<UserExpense> getDebtorsFromExpense(long id) {
        if (id < 0 || !repo.existsById(id)) {
            throw new InvalidExpenseException("Expense id in invalid");
        }
        Set<UserExpense> res = repo.getReferenceById(id).getUserExpenses();
        Hibernate.initialize(res);
        return res;
    }

}
