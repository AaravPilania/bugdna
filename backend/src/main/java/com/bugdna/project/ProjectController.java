package com.bugdna.project;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    public record ProjectResponse(
            Long id,
            String name,
            String projectKeyMasked,
            String allowedOrigin,
            LocalDateTime createdAt
    ) {}

    private final ProjectRepository projectRepository;

    public ProjectController(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects() {
        List<ProjectResponse> list = projectRepository.findAll().stream()
                .map(p -> {
                    String rawKey = p.getProjectKey();
                    String maskedKey = (rawKey != null && rawKey.length() > 6)
                            ? rawKey.substring(0, 4) + "****" + rawKey.substring(rawKey.length() - 2)
                            : "****";
                    return new ProjectResponse(p.getId(), p.getName(), maskedKey, p.getAllowedOrigin(), p.getCreatedAt());
                })
                .toList();
        return ResponseEntity.ok(list);
    }
}

