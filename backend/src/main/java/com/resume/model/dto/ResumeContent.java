package com.resume.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeContent {

    private BasicInfo basicInfo;
    private List<Education> education;
    private List<WorkExperience> workExperience;
    private List<ProjectExperience> projectExperience;
    private List<String> skills;
    private String summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BasicInfo {
        private String name;
        private String phone;
        private String email;
        private String location;
        private String website;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Education {
        private String school;
        private String degree;
        private String major;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkExperience {
        private String company;
        private String position;
        private String startDate;
        private String endDate;
        private String description;
        private List<String> achievements;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectExperience {
        private String name;
        private String role;
        private String startDate;
        private String endDate;
        private String description;
        private String techStack;
        private List<String> highlights;
    }
}
