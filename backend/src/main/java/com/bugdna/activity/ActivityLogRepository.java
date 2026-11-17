package com.bugdna.activity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @Query("SELECT a FROM ActivityLog a ORDER BY a.id DESC")
    List<ActivityLog> findRecent(Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE a.id > :afterId ORDER BY a.id DESC")
    List<ActivityLog> findNewerThan(@Param("afterId") Long afterId, Pageable pageable);

    @Query("SELECT MAX(a.id) FROM ActivityLog a")
    Long findMaxId();
}

