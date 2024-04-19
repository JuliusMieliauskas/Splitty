package server.api;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import commons.EventUpdate;
import commons.UserExpense;
import commons.exceptions.InvalidExpenseException;
import commons.exceptions.InvalidUserExpenseException;
import org.springframework.http.ResponseEntity;

import commons.Expense;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import server.services.ExpenseService;


@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final SimpMessagingTemplate messagingTemplate; // For websocket
    private final ExecutorService longPollThreads = Executors.newFixedThreadPool(10);
    // Used to update long polling thread of update in users of event
    public final Map<Long, Object> userUpdateLocks = new ConcurrentHashMap<>();

    public ExpenseController(ExpenseService expenseService, SimpMessagingTemplate messagingTemplate) {
        this.expenseService = expenseService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Long poll for updates for the users of an event
     * @return The set of users after the update
     */
    @GetMapping(path = {"/{expenseId}/debtors/long-poll"})
    public DeferredResult<Set<UserExpense>> longPollUsers(@PathVariable Long expenseId) {
        DeferredResult<Set<UserExpense>> output = new DeferredResult<>();
        System.out.println("Creating long-poll");
        longPollThreads.execute(() -> {
            Object userUpdateLock = userUpdateLocks.computeIfAbsent(expenseId, x -> new Object());
            synchronized (userUpdateLock) {
                try {
                    userUpdateLock.wait(100000); // max waiting time is 100 seconds
                    System.out.println("Sending update");
                    Set<UserExpense> res = expenseService.getDebtorsFromExpense(expenseId);
                    output.setResult(res);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return output;
    }

    @GetMapping(path = {"", "/"})
    public ResponseEntity<List<Expense>> getAllExpenses() {
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    /**
     * Creates a new expense
     * @param expense the body of the request with the new expense info
     * @return the new expense info or a bad request
     */
    @PostMapping(path = {"","/" })
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense) {
        Expense saved = expenseService.createExpense(expense);
        messagingTemplate.convertAndSend("/api/events/websocket/" + saved.getEvent().getId(),
                new EventUpdate(EventUpdate.Action.ADDED_EXPENSE, saved.getId()));
        return ResponseEntity.ok(saved);
    }

    /**
     * Deletes all expenses
     * @return either a bad request or a completion message
     */
    // Seems a bit dangerous to include; But oh well, what's the worst that can happen (: lol
    @DeleteMapping(path = {"", "/"})
    public ResponseEntity<String> deleteAllExpenses() {
        List<Expense> expenses = expenseService.getAllExpenses();
        String deleted = expenseService.deleteAllExpenses();
        Set<Long> eventsWithExpenses = new HashSet<>();
        for (Expense e : expenses) {
            eventsWithExpenses.add(e.getEvent().getId());
        }
        for (Long eventId : eventsWithExpenses) {
            messagingTemplate.convertAndSend("/api/events/websocket/" + eventId,
                    new EventUpdate(EventUpdate.Action.REMOVED_ALL_EXPENSES));
        }
        return ResponseEntity.ok(deleted);
    }

    /**
     * gets an expense by its id
     * @param id id of the expense you want to retrieve
     * @return either a bad request or the user you wanted
     */
    @GetMapping(path = "/{id}")
    public ResponseEntity<Expense> getExpenseById(@PathVariable("id") long id) {
        Expense expense = expenseService.getExpenseById(id);
        return ResponseEntity.ok(expense);
    }

    /**
     * Updates an Expense
     * @param id id of the expense to update
     * @param expense the body of the request with the new information
     * @return the json for the newly updated expense or bad request
     */
    @PutMapping(path = {"/{id}"})
    public ResponseEntity<Expense> updateExpense(@PathVariable("id") long id, @RequestBody Expense expense) {
        Expense updatedExpense = expenseService.updateExpense(id, expense);
        Object lock = userUpdateLocks.get(id);
        if (lock != null) {
            synchronized (lock) {
                lock.notifyAll(); // Used to update threads waiting on long-polling
            }
        }
        messagingTemplate.convertAndSend("/api/events/websocket/" + updatedExpense.getEvent().getId(),
                new EventUpdate(EventUpdate.Action.UPDATED_EXPENSE, id));
        return ResponseEntity.ok(updatedExpense);
    }

    /**
     * Deletes a specific expense
     * @param id id of expense to delete
     * @return either bad request or completion message
     */
    @DeleteMapping(path = {"/{id}"})
    public ResponseEntity<String> deleteExpense(@PathVariable("id") long id) {
        Long eventId = expenseService.getExpenseById(id).getEvent().getId();
        String deleted = expenseService.deleteExpenseById(id);
        messagingTemplate.convertAndSend("/api/events/websocket/" + eventId,
                new EventUpdate(EventUpdate.Action.REMOVED_EXPENSE, id));
        return ResponseEntity.ok(deleted);
    }

    /**
     * Gets the list of all debtors of an expense
     * @param id the id of the expense
     * @return a list of UserExpense relations
     */
    @GetMapping(path = "/{id}/debtors")
    public ResponseEntity<Set<UserExpense>> getDebtorsFromExpense(@PathVariable("id") long id) {
        Set<UserExpense> debtors = expenseService.getDebtorsFromExpense(id);
        return ResponseEntity.ok(debtors);
    }

    /**
     * adds a user to an expense by adding a row in the userExpense repo
     * @param id the id of the expense
     * @param userExpense the userExpense to add, with also info like totalAmount to pay
     * @return either a bad request or the saved userExpense
     */
    @PostMapping(path = {"/{id}/debtors"})
    public ResponseEntity<UserExpense> addUserToExpense(@PathVariable("id") long id, @RequestBody UserExpense userExpense) {
        UserExpense saved = expenseService.addUserToExpense(id, userExpense);
        Object lock = userUpdateLocks.get(id);
        if (lock != null) {
            synchronized (lock) {
                lock.notifyAll(); // Used to update threads waiting on long-polling
            }
        }
        return ResponseEntity.ok(saved);
    }

    /**
     * Get a user from an expense
     * @param eid the expense id
     * @param uid the user id to delete from the expense
     * @return the user expense relation
     */
    @GetMapping(path = {"/{e_id}/debtors/{u_id}"})
    public ResponseEntity<UserExpense> getUserExpense(@PathVariable("e_id") long eid, @PathVariable("u_id") long uid) {
        UserExpense userExpense = expenseService.getUserExpenseByExpenseAndUser(eid, uid).get();
        return ResponseEntity.ok(userExpense);
    }

    /**
     * Update a user from an expense
     * @param eid the expense id
     * @param uid the user id to update from the expense
     * @return either a bad request or a completion message
     */
    @PutMapping(path = {"/{ex_id}/debtors/{u_id}"})
    public ResponseEntity<UserExpense> updateUserExpense(@PathVariable("ex_id") long eid,
                                                         @PathVariable("u_id") long uid,
                                                         @RequestBody UserExpense userExpense) {
        UserExpense edited = expenseService.updateUserExpense(eid, uid, userExpense);
        System.out.println("detect user added!");
        Object lock = userUpdateLocks.get(eid);
        if (lock != null) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
        return ResponseEntity.ok(edited);
    }

    /**
     * Deletes a user from an expense by deleting a column in the userExpense repo
     * @param eid the expense id
     * @param uid the user id to delete from the expense
     * @return either a bad request or a completion message
     */
    @DeleteMapping(path = {"/{e_id}/debtors/{u_id}"})
    public ResponseEntity<String> deleteUserFromExpense(@PathVariable("e_id") long eid, @PathVariable("u_id") long uid) {
        String deleted = expenseService.deleteUserFromExpense(eid, uid);
        Object lock = userUpdateLocks.get(eid);
        if (lock != null) {
            synchronized (lock) {
                lock.notifyAll(); // Used to update threads waiting on long-polling
            }
        }
        return ResponseEntity.ok(deleted);
    }

    /**
     * Handler for exception, so that if request fails, the client gets an informative message about
     * what went wrong
     * @param ex
     * @return String ( error message )
     */
    @ExceptionHandler({InvalidExpenseException.class, InvalidUserExpenseException.class})
    public ResponseEntity<String> handleIllegalArgumentException(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getClass().getCanonicalName() + ":\n" + ex.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<String> handleException(Exception ex) {
        return ResponseEntity.badRequest().body(ex.getClass().getCanonicalName() + ":\n" + ex.getMessage());
    }
}