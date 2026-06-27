package com.resume.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProfileRequest {
    private String workYears;           // 应届/1-3年/3-5年/5-10年/10年+
    private List<String> techDirection;  // Java/Python/前端/大数据/AI
    private String targetPosition;      // 目标岗位
    private List<String> targetIndustry; // 互联网/金融/游戏
    private String salaryRange;         // 20k-30k
    private List<Map<String, Object>> education; // [{degree:"本科", school:"xxx", major:"xxx"}, ...]
    private List<String> coreSkills;    // Spring Boot, MySQL, ...
}
