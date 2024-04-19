package server.database;

import commons.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;


@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    boolean existsByInviteCode(String inviteCode);
    Event getByInviteCode(String code);
}