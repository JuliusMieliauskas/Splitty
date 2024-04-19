package commons;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "user_expenses")
public class UserExpense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "debtor_id")
    private User debtor; // The user who owes money
    @ManyToOne
    @JoinColumn(name = "expense_id")
    private Expense expense; // The expense to which the user owes money

    @Column(name = "total_amount")
    private Double totalAmount; // The total amount of the expense. ( The share of the expense that the user has to pay)

    @Column(name = "paid_amount")
    private Double paidAmount; // Already paid amount by the user

    public void setId(Long id) {
        this.id = id;
    }

    public UserExpense() {
    }

    /**
     * Create a new User-Expense relation
     * @param debtor The user that needs to pay for the expense
     * @param expense The expense
     * @param paidAmount The amount the user has already paid
     * @param totalAmount The total amount the user has to pay
     */
    public UserExpense(User debtor, Expense expense, double paidAmount, double totalAmount) {
        this.debtor = debtor;
        this.expense = expense;
        this.paidAmount = paidAmount;
        this.totalAmount = totalAmount;
    }

    public Long getId() {
        return this.id;
    }


    public User getDebtor() {
        return debtor;
    }

    public void setDebtor(User debtor) {
        this.debtor = debtor;
    }

    public Expense getExpense() {
        return expense;
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Double paidAmount) {
        this.paidAmount = paidAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserExpense that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "UserExpense{" +
                "id=" + id +
                ", debtor=" + debtor +
                ", expense=" + expense +
                ", totalAmount=" + totalAmount +
                ", paidAmount=" + paidAmount +
                '}';
    }
}
