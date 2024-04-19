package commons;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true)
    private String inviteCode;

    @Column(nullable = false)
    private LocalDateTime creationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Column(nullable = false)
    private LocalDateTime lastActivity;

    @ManyToMany(mappedBy = "events", cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    private Set<User> users = new HashSet<>(); // All users participating in this event

    @OneToMany(mappedBy = "event")
    private Set<Expense> expenses = new HashSet<>(); // All expenses related to this event


    /**
     * Make given user a part of the event
     * @param user User object to add to event
     */
    public void addUser(User user) {
        users.add(user);
        user.getEvents().add(this);
    }

    /**
     * Remove given user from event
     * @param user User object to remove from object
     */
    public void removeUser(User user) {
        users.remove(user);
        user.getEvents().remove(this);
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
        expense.setEvent(this);
    }

    public void removeExpense(Expense expense) {
        expenses.remove(expense);
    }

    @JsonIgnore
    public Set<User> getUsers() {
        return users;
    }


    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getTitle() {
        return title;
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    /**
     * Construct a commons.Event with empty arrays for participants and expenses
     * @param title is the title of the commons.Event
     */
    public Event(String title) {
        this.title = title;
        this.creationDate = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.inviteCode = null;
    }

    @SuppressWarnings("unused")
    protected Event() {
        // for object mappers
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    @JsonIgnore
    public Set<Expense> getExpenses() {
        return expenses;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event event)) {
            return false;
        }
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", inviteCode='" + inviteCode + '\'' +
                ", creationDate=" + creationDate +
                ", lastActivity=" + lastActivity +
                ", users=" + users +
                ", expenses=" + expenses +
                '}';
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}