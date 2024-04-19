package server.database;

import org.springframework.data.jpa.repository.JpaRepository;

import commons.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String name);
    Optional<User> findByUsername(String name);

}