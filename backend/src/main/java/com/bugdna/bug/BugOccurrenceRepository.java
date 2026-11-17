package com.bugdna.bug;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BugOccurrenceRepository extends JpaRepository<BugOccurrence, Long> {
    List<BugOccurrence> findByBugIdOrderByOccurredAtDesc(Long bugId);
}

