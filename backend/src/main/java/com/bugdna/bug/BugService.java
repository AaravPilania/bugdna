package com.bugdna.bug;

import com.bugdna.analytics.BugSummaryView;
import com.bugdna.analytics.BugSummaryViewRepository;
import com.bugdna.bug.dto.BugDetailDto;
import com.bugdna.bug.dto.BugListItemDto;
import com.bugdna.bug.dto.BugPatchRequest;
import com.bugdna.common.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class BugService {

    private final BugRepository bugRepository;
    private final BugSummaryViewRepository bugSummaryViewRepository;

    public BugService(BugRepository bugRepository, BugSummaryViewRepository bugSummaryViewRepository) {
        this.bugRepository = bugRepository;
        this.bugSummaryViewRepository = bugSummaryViewRepository;
    }

    @Transactional(readOnly = true)
    public List<BugListItemDto> listBugs(BugStatus status, BugPriority priority, ErrorType errorType, String q, String sort) {
        Specification<Bug> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (errorType != null) {
                predicates.add(cb.equal(root.get("errorType"), errorType));
            }
            if (q != null && !q.isBlank()) {
                String search = "%" + q.trim().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), search);
                Predicate fpLike = cb.like(cb.lower(root.get("fingerprint")), search);
                Predicate endpointLike = cb.like(cb.lower(root.get("endpoint")), search);
                predicates.add(cb.or(titleLike, fpLike, endpointLike));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sortOrder = Sort.by(Sort.Direction.DESC, "lastSeenAt");
        if ("occurrences".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "occurrenceCount");
        } else if ("priority".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "priority");
        }

        return bugRepository.findAll(spec, sortOrder)
                .stream()
                .map(BugListItemDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public BugDetailDto getBugDetail(Long id) {
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + id));

        BugSummaryView summary = bugSummaryViewRepository.findById(id).orElse(null);
        long sessionsAffected = summary != null ? summary.getSessionsAffected() : 1;
        long commentCount = summary != null ? summary.getCommentCount() : 0;
        Long minutesOpen = summary != null ? summary.getMinutesOpen() : null;

        return BugDetailDto.fromEntity(bug, sessionsAffected, commentCount, minutesOpen);
    }

    @Transactional
    public BugDetailDto patchBug(Long id, BugPatchRequest request) {
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + id));

        if (request.status() != null && request.status() != bug.getStatus()) {
            BugStatus newStatus = request.status();
            if (newStatus == BugStatus.RESOLVED && bug.getResolvedAt() == null) {
                bug.setResolvedAt(LocalDateTime.now());
            } else if (newStatus != BugStatus.RESOLVED && newStatus != BugStatus.CLOSED) {
                bug.setResolvedAt(null);
            }
            bug.setStatus(newStatus);
        }

        if (request.priority() != null) {
            bug.setPriority(request.priority());
        }

        if (request.assignee() != null) {
            bug.setAssignee(request.assignee().isBlank() ? null : request.assignee().trim());
        }

        if (request.title() != null && !request.title().isBlank()) {
            bug.setTitle(request.title().trim());
        }

        Bug saved = bugRepository.saveAndFlush(bug);
        return getBugDetail(saved.getId());
    }

    @Transactional
    public void deleteBug(Long id) {
        if (!bugRepository.existsById(id)) {
            throw new ResourceNotFoundException("Bug not found with id: " + id);
        }
        bugRepository.deleteById(id);
        bugRepository.flush();
    }
}

