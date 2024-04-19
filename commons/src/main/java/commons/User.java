package commons;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username; // How user is called in the application

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Transient // not stored in db
    private String iban; // User's iban

    @Transient  // not stored in db
    private String email; // User's email

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(
            name = "user_event",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id"))
    private Set<Event> events = new HashSet<>(); // Many-to-many relationship with Event. All events in which the user is participating

    @OneToMany(mappedBy = "debtor")
    private Set<UserExpense> userExpenses = new HashSet<>(); // All expenses in which the user is involved

    @OneToMany(mappedBy = "originalPayer")
    private Set<Expense> paidExpenses = new HashSet<>();

    @JsonIgnore
    public Set<Expense> getPaidExpenses() {
        return this.paidExpenses;
    }

    public void addEvent(Event event) {
        events.add(event);
        event.getUsers().add(this);
    }

    public void removeEvent(Event event) {
        events.remove(event);
        event.getUsers().remove(this);
    }

    /**
     * Add the user to an expense
     * @param expense The expense the user will be added to
     */
    public void addExpense(Expense expense) {
        UserExpense userExpense = new UserExpense();
        userExpense.setDebtor(this);
        userExpense.setExpense(expense);
        userExpenses.add(userExpense);
        expense.getUserExpenses().add(userExpense);
    }

    /**
     * Remove the user from the expense
     * @param expense The expense the user will be removed from
     */
    public void removeExpense(Expense expense) {
        UserExpense userExpense = new UserExpense();
        userExpense.setDebtor(this);
        userExpense.setExpense(expense);
        userExpenses.remove(userExpense);
        expense.getUserExpenses().remove(userExpense);
    }

    @JsonIgnore
    public Set<Event> getEvents() {
        return events;
    }

    @JsonIgnore
    public Set<UserExpense> getUserExpenses() {
        return userExpenses;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username.strip().toLowerCase();
    }

    public String getIban() {
        return iban;
    }

    /**
     *
     */
    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * constructor class for the user
     * @param username takes the username and makes it all lowercase and removes trailing spaces
     * @param iban takes iban and removes all spaces and makes it lowercase
     * @param email takes email and makes it lowercase and removes trailing spaces
     */
    public User(String email, String iban, String username) {
        this.username = username.strip().toLowerCase();
        this.iban = iban.replaceAll(" ", "").toLowerCase();
        this.email = email.strip().toLowerCase();
    }
    public User(String username) {
        this.username = username.strip().toLowerCase();
    }

    @SuppressWarnings("unused")
    public User() {}

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", iban='" + iban + '\'' +
                ", email='" + email + '\'' +
                ", events=" + events +
                ", userExpenses=" + userExpenses +
                '}';
    }
}