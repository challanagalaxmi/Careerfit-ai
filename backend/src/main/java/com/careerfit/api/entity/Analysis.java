package com.careerfit.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "resume_text", nullable = false, columnDefinition = "LONGTEXT")
    private String resumeText;

    @Lob
    @Column(name = "job_description", nullable = false, columnDefinition = "LONGTEXT")
    private String jobDescription;

    @Column(name = "match_score", nullable = false)
    private Integer matchScore;

    @Lob
    @Column(name = "matching_skills", nullable = false, columnDefinition = "JSON")
    private String matchingSkills;

    @Lob
    @Column(name = "missing_skills", nullable = false, columnDefinition = "JSON")
    private String missingSkills;

    @Lob
    @Column(name = "suggestions", nullable = false, columnDefinition = "JSON")
    private String suggestions;

    @Lob
    @Column(name = "recommended_keywords", nullable = false, columnDefinition = "JSON")
    private String recommendedKeywords;

    @Lob
    @Column(name = "skill_gap_roadmap", nullable = false, columnDefinition = "JSON")
    private String skillGapRoadmap;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
