package com.bugdna.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByProjectIdAndEventId(Long projectId, String eventId);

    @Query("SELECT e FROM Event e WHERE e.session.id = :sessionId AND e.occurredAt BETWEEN :start AND :end ORDER BY e.occurredAt ASC")
    List<Event> findTimelineEvents(@Param("sessionId") Long sessionId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT e.occurredAt FROM Event e WHERE e.occurredAt >= :since AND e.type IN ('JS_ERROR', 'PROMISE_REJECTION', 'API_ERROR', 'NETWORK_ERROR')")
    List<LocalDateTime> findErrorEventTimestampsSince(@Param("since") LocalDateTime since);
}

