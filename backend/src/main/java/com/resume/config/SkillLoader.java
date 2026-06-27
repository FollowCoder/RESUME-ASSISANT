package com.resume.config;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.repository.ClasspathSkillRepository;
import io.agentscope.core.tool.Toolkit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Skill 加载器配置类
 * 使用 ClasspathSkillRepository 从 classpath:skills/ 目录加载 AgentScope Skill，
 * 并注册到 SkillBox 供智能体按需使用。
 *
 * 资源目录结构要求：
 *   src/main/resources/skills/
 *   └── resume-generator/
 *       └── SKILL.md
 */
@Slf4j
@Configuration
public class SkillLoader {

    /**
     * AgentScope 工具集 Bean
     */
    @Bean
    public Toolkit agentToolkit() {
        return new Toolkit();
    }

    /**
     * SkillBox Bean - 自动从 classpath:skills/ 加载所有 Skill 并注册
     */
    @Bean
    public SkillBox skillBox(Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);

        try (ClasspathSkillRepository repository = new ClasspathSkillRepository("skills")) {
            List<AgentSkill> skills = repository.getAllSkills();
            log.info("从 classpath:skills/ 发现 {} 个 Skill", skills.size());
            for (AgentSkill skill : skills) {
                skillBox.registerSkill(skill);
                log.info("已注册 Skill: {} - {}", skill.getName(), skill.getDescription());
            }
        } catch (Exception e) {
            log.error("加载 Skill 失败: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to load skills from classpath:skills/", e);
        }

        return skillBox;
    }
}
