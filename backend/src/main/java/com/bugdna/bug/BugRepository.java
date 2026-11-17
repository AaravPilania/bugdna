package com.bugdna.bug;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BugRepository extends JpaRepository<Bug, Long>, JpaSpecificationExecutor<Bug> {

    Optional<Bug> findByProjectIdAndFingerprint(Long projectId, String fingerprint);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId AND b.fingerprint = :fingerprint")
    Optional<Bug> findForUpdateByProjectIdAndFingerprint(@Param("projectId") Long projectId, @Param("fingerprint") String fingerprint);

    @Query("SELECT MAX(b.updatedAt) FROM Bug b")
    LocalDateTime findMaxUpdatedAt();
}

