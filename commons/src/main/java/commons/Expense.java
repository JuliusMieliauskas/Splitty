package commons;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


@Entity
@Table(name = "expenses")
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount; // The total amount of the expense

    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private LocalDateTime creationDate;

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setOriginalPayer(User originalPayer) {
        this.originalPayer = originalPayer;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @Column()
    private String tag; // Tag to categorize the expense

    @ManyToOne
    @JoinColumn(name = "originalpayer_id", nullable = false)
    private User originalPayer; // The User who paid for the expense (the one who is owed money)

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event; // Event to which the expense is related

    @OneToMany(mappedBy = "expense")
    private Set<UserExpense> userExpenses = new HashSet<>(); // All users involved in the expense, who owes money to the original payer

    public Expense() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public String getTitle() {
        return title;
    }

    public String getTag() {
        return tag;
    }

    public User getOriginalPayer() {
        return originalPayer;
    }

    public Event getEvent() {
        return event;
    }

    public void setUserExpenses(Set<UserExpense> userExpenses) {
        this.userExpenses = userExpenses;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    /**
     * Add a user that owes money
     * @param user User object that will be a part of the expense
     */
    public void addUser(User user) {
        UserExpense userExpense = new UserExpense();
        userExpense.setExpense(this);
        userExpense.setDebtor(user);
        userExpenses.add(userExpense);
        user.getUserExpenses().add(userExpense);
    }

    /**
     * Remove user from expense
     * @param user User object that will be removed from expense
     */
    public void removeUser(User user) {
        UserExpense userExpense = new UserExpense();
        userExpense.setExpense(this);
        userExpense.setDebtor(user);
        userExpenses.remove(userExpense);
        user.getUserExpenses().remove(userExpense);
    }

    @JsonIgnore
    public Set<UserExpense> getUserExpenses() {
        return userExpenses;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    /**
     * Constructor for new expense with all properties apart from id,
     * the id will be auto generated when adding it to the database
     * @param amount The cost of the expense
     * @param title The title of the expense
     * @param originalPayer The user who originally paid for the expense
     * @param event The event the expense is a part of
     * @param userExpenses The amount of money every user that's a part of the expense owes
     */
    public Expense(Double amount, String title, User originalPayer, Event event, Set<UserExpense> userExpenses) {
        this.amount = amount;
        this.title = title;
        this.originalPayer = originalPayer;
        this.event = event;
        this.userExpenses = userExpenses;
        this.creationDate = LocalDateTime.now();
    }

    /**
     * Copy constructor
     */
    public Expense(Expense other) {
        this.id = other.id;
        this.tag = other.tag;
        this.amount = other.amount;
        this.title = other.title;
        this.originalPayer = other.originalPayer;
        this.event = other.event;
        this.userExpenses = null;
        this.creationDate = other.creationDate;
    }

    /**
     * Constructor for an expense
     * @param amount The cost of the expense
     * @param title The title of the expense
     * @param originalPayer The user who originally paid for the expense
     * @param event The event the expense is a part of
     */
    public Expense(Double amount, String title, User originalPayer, Event event) {
        this.amount = amount;
        this.title = title;
        this.originalPayer = originalPayer;
        this.event = event;
        this.creationDate = LocalDateTime.now();
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Expense expense)) {
            return false;
        }
        return Objects.equals(id, expense.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Expense{" +
                "id=" + id +
                ", amount=" + amount +
                ", title='" + title + '\'' +
                ", tag='" + tag + '\'' +
                ", originalPayer=" + originalPayer +
                ", event=" + event +
                ", userExpenses=" + userExpenses +
                '}';
    }

}
