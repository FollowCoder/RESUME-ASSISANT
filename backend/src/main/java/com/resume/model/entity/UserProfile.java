package com.resume.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "work_years", length = 20)
    private String workYears;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tech_direction", columnDefinition = "jsonb")
    private List<String> techDirection;

    @Column(name = "target_position", length = 100)
    private String targetPosition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_industry", columnDefinition = "jsonb")
    private List<String> targetIndustry;

    @Column(name = "salary_range", length = 50)
    private String salaryRange;

    @Setter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "education", columnDefinition = "jsonb")
    private List<Map<String, Object>> education;

    /**
     * 自定义 setter，兼容旧数据格式（单个对象转为数组）
     */
    public void setEducation(Object education) {
        if (education == null) {
            this.education = null;
        } else if (education instanceof List) {
            this.education = (List<Map<String, Object>>) education;
        } else if (education instanceof Map) {
            // 兼容旧数据格式：将单个对象包装为数组
            this.education = List.of((Map<String, Object>) education);
        } else {
            throw new IllegalArgumentException("Invalid education format: " + education.getClass());
        }
    }

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "core_skills", columnDefinition = "jsonb")
    private List<String> coreSkills;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
