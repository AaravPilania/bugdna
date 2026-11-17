package com.bugdna.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {
    Optional<AiAnalysis> findFirstByBugIdOrderByCreatedAtDesc(Long bugId);
    Optional<AiAnalysis> findByBugIdAndInputHash(Long bugId, String inputHash);
}

