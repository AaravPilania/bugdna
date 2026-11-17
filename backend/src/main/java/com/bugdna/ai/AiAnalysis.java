package com.bugdna.ai;

import com.bugdna.bug.Bug;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analyses")
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bug_id", nullable = false)
    private Bug bug;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false, length = 80)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiStatus status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "evidence_json", columnDefinition = "JSON")
    private String evidenceJson;

    @Column(name = "hypotheses_json", columnDefinition = "JSON")
    private String hypothesesJson;

    @Column(name = "debug_steps_json", columnDefinition = "JSON")
    private String debugStepsJson;

    @Column(name = "repro_steps_json", columnDefinition = "JSON")
    private String reproStepsJson;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public AiAnalysis() {
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public AiStatus getStatus() {
        return status;
    }

    public void setStatus(AiStatus status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getHypothesesJson() {
        return hypothesesJson;
    }

    public void setHypothesesJson(String hypothesesJson) {
        this.hypothesesJson = hypothesesJson;
    }

    public String getDebugStepsJson() {
        return debugStepsJson;
    }

    public void setDebugStepsJson(String debugStepsJson) {
        this.debugStepsJson = debugStepsJson;
    }

    public String getReproStepsJson() {
        return reproStepsJson;
    }

    public void setReproStepsJson(String reproStepsJson) {
        this.reproStepsJson = reproStepsJson;
    }

    public String getInputHash() {
        return inputHash;
    }

    public void setInputHash(String inputHash) {
        this.inputHash = inputHash;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

