package com.bugdna.bug;

import com.bugdna.event.Event;
import com.bugdna.event.TrackingSession;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bug_occurrences", uniqueConstraints = {
    @UniqueConstraint(name = "uk_occ_event", columnNames = {"trigger_event_pk"})
})
public class BugOccurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bug_id", nullable = false)
    private Bug bug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_pk", nullable = false)
    private TrackingSession session;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trigger_event_pk", nullable = false)
    private Event triggerEvent;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "was_regression", nullable = false)
    private boolean wasRegression = false;

    public BugOccurrence() {
    }

    public BugOccurrence(Bug bug, TrackingSession session, Event triggerEvent, LocalDateTime occurredAt, boolean wasRegression) {
        this.bug = bug;
        this.session = session;
        this.triggerEvent = triggerEvent;
        this.occurredAt = occurredAt;
        this.wasRegression = wasRegression;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Bug getBug() {
        return bug;
    }

    public void setBug(Bug bug) {
        this.bug = bug;
    }

    public TrackingSession getSession() {
        return session;
    }

    public void setSession(TrackingSession session) {
        this.session = session;
    }

    public Event getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(Event triggerEvent) {
        this.triggerEvent = triggerEvent;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public boolean isWasRegression() {
        return wasRegression;
    }

    public void setWasRegression(boolean wasRegression) {
        this.wasRegression = wasRegression;
    }
}

