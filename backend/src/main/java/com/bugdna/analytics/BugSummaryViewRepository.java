package com.bugdna.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BugSummaryViewRepository extends JpaRepository<BugSummaryView, Long> {
    Optional<BugSummaryView> findById(Long id);
}

