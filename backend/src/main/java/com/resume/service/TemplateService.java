package com.resume.service;

import com.resume.model.dto.ResumeContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Locale;

/**
 * 简历模板服务 - 管理模板定义与 HTML 渲染
 */
@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    private final TemplateEngine templateEngine;

    private static final List<TemplateInfo> TEMPLATES = List.of(
            TemplateInfo.builder()
                    .id("classic")
                    .name("经典")
                    .description("传统双列布局，简洁大方，适合各行业")
                    .previewUrl("/api/resume/templates/preview/classic.png")
                    .build(),
            TemplateInfo.builder()
                    .id("modern")
                    .name("现代")
                    .description("扁平设计，色块区分模块，视觉层次清晰")
                    .previewUrl("/api/resume/templates/preview/modern.png")
                    .build(),
            TemplateInfo.builder()
                    .id("minimal")
                    .name("简约")
                    .description("极简单列，注重内容，大量留白")
                    .previewUrl("/api/resume/templates/preview/minimal.png")
                    .build(),
            TemplateInfo.builder()
                    .id("tech")
                    .name("技术")
                    .description("突出技能栈，适合程序员和技术岗位")
                    .previewUrl("/api/resume/templates/preview/tech.png")
                    .build(),
            TemplateInfo.builder()
                    .id("creative")
                    .name("创意")
                    .description("独特排版，适合设计岗和创意类工作")
                    .previewUrl("/api/resume/templates/preview/creative.png")
                    .build(),
            TemplateInfo.builder()
                    .id("laoyu")
                    .name("老鱼简历")
                    .description("老鱼简历风格，清晰的模块划分，专业的排版设计")
                    .previewUrl("/api/resume/templates/preview/laoyu.png")
                    .build()
    );

    public TemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * 获取所有模板列表
     */
    public List<TemplateInfo> getTemplateList() {
        return TEMPLATES;
    }

    /**
     * 根据 ID 获取模板信息
     */
    public TemplateInfo getTemplate(String templateId) {
        return TEMPLATES.stream()
                .filter(t -> t.getId().equals(templateId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 使用 Thymeleaf 渲染简历 HTML
     *
     * @param templateId 模板ID
     * @param content    简历内容
     * @param language   语言 zh/en
     * @return 渲染后的 HTML 字符串
     */
    public String renderHtml(String templateId, ResumeContent content, String language) {
        // 验证模板存在
        if (getTemplate(templateId) == null) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }

        // 验证简历内容不为空
        if (content == null) {
            throw new IllegalArgumentException("简历内容为空，无法渲染模板");
        }

        Context context = new Context(Locale.forLanguageTag(language != null ? language : "zh"));
        context.setVariable("content", content);
        context.setVariable("language", language != null ? language : "zh");

        String templatePath = "resume/" + templateId;
        log.debug("渲染模板: {}, 语言: {}", templatePath, language);

        return templateEngine.process(templatePath, context);
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class TemplateInfo {
        private String id;
        private String name;
        private String description;
        private String previewUrl;
    }
}
