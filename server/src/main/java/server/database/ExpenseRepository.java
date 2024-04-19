package server.database;

import java.util.List;

import commons.Expense;
import commons.User;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByOriginalPayer(User originalPayer);

}
