package com.resume.service;

import com.resume.model.dto.ResumeContent;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 导出服务 - 支持 PDF 和 Word 导出
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final TemplateService templateService;

    @Value("${app.upload.path:${user.home}/resume-assistant/uploads}")
    private String uploadPath;

    public ExportService(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 导出 PDF
     *
     * @param content    简历内容
     * @param templateId 模板ID
     * @param language   语言
     * @return PDF 字节数组
     */
    public byte[] exportPdf(ResumeContent content, String templateId, String language) {
        try {
            // 渲染 HTML
            String html = templateService.renderHtml(templateId, content, language);

            // 使用 Flying Saucer 将 HTML 转为 PDF
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            // 设置中文字体支持
            configureFonts(renderer);

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            log.info("PDF导出成功, 模板: {}, 大小: {} bytes", templateId, outputStream.size());
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("PDF导出失败: {}", e.getMessage(), e);
            throw new RuntimeException("PDF导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 配置 PDF 渲染器字体
     */
    private void configureFonts(ITextRenderer renderer) {
        try {
            // 尝试添加系统中文字体
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.contains("win")) {
                String fontsDir = "C:/Windows/Fonts/";
                addFontIfExists(renderer, fontsDir + "simsun.ttc");
                addFontIfExists(renderer, fontsDir + "msyh.ttc");
                addFontIfExists(renderer, fontsDir + "simhei.ttf");
            } else if (osName.contains("mac")) {
                addFontIfExists(renderer, "/System/Library/Fonts/PingFang.ttc");
                addFontIfExists(renderer, "/Library/Fonts/Arial Unicode.ttf");
            } else {
                // Linux
                addFontIfExists(renderer, "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc");
                addFontIfExists(renderer, "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc");
            }
        } catch (Exception e) {
            log.warn("字体配置警告: {}", e.getMessage());
        }
    }

    private void addFontIfExists(ITextRenderer renderer, String fontPath) {
        try {
            if (Files.exists(Paths.get(fontPath))) {
                renderer.getFontResolver().addFont(fontPath, true);
                log.debug("已添加字体: {}", fontPath);
            }
        } catch (Exception e) {
            log.debug("添加字体失败: {} - {}", fontPath, e.getMessage());
        }
    }

    /**
     * 导出 Word 文档
     *
     * @param content    简历内容
     * @param templateId 模板ID（Word导出不使用HTML模板，但保持接口一致）
     * @param language   语言
     * @return docx 字节数组
     */
    public byte[] exportWord(ResumeContent content, String templateId, String language) {
        try (XWPFDocument document = new XWPFDocument()) {
            // 设置页边距
            CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
            var pgMar = sectPr.addNewPgMar();
            pgMar.setTop(BigInteger.valueOf(1440));    // 1 inch
            pgMar.setBottom(BigInteger.valueOf(1440));
            pgMar.setLeft(BigInteger.valueOf(1440));
            pgMar.setRight(BigInteger.valueOf(1440));

            boolean isZh = !"en".equals(language);

            // 基本信息
            if (content.getBasicInfo() != null) {
                ResumeContent.BasicInfo info = content.getBasicInfo();
                addTitle(document, info.getName() != null ? info.getName() : "");

                StringBuilder contactLine = new StringBuilder();
                if (info.getPhone() != null) contactLine.append(info.getPhone()).append("  ");
                if (info.getEmail() != null) contactLine.append(info.getEmail()).append("  ");
                if (info.getLocation() != null) contactLine.append(info.getLocation());
                if (!contactLine.isEmpty()) {
                    addCenteredParagraph(document, contactLine.toString().trim(), 10);
                }
                addEmptyLine(document);
            }

            // 个人总结
            if (content.getSummary() != null && !content.getSummary().isBlank()) {
                addSectionTitle(document, isZh ? "个人总结" : "Summary");
                addParagraph(document, content.getSummary(), 10);
                addEmptyLine(document);
            }

            // 工作经历
            if (content.getWorkExperience() != null && !content.getWorkExperience().isEmpty()) {
                addSectionTitle(document, isZh ? "工作经历" : "Work Experience");
                for (ResumeContent.WorkExperience work : content.getWorkExperience()) {
                    addBoldParagraph(document, work.getCompany() + " — " + work.getPosition(), 11);
                    addItalicParagraph(document, work.getStartDate() + " - " + work.getEndDate(), 9);
                    if (work.getDescription() != null) {
                        addParagraph(document, work.getDescription(), 10);
                    }
                    if (work.getAchievements() != null) {
                        for (String achievement : work.getAchievements()) {
                            addBulletParagraph(document, achievement, 10);
                        }
                    }
                    addEmptyLine(document);
                }
            }

            // 项目经验
            if (content.getProjectExperience() != null && !content.getProjectExperience().isEmpty()) {
                addSectionTitle(document, isZh ? "项目经验" : "Projects");
                for (ResumeContent.ProjectExperience proj : content.getProjectExperience()) {
                    String projTitle = proj.getName();
                    if (proj.getRole() != null) projTitle += " — " + proj.getRole();
                    addBoldParagraph(document, projTitle, 11);
                    if (proj.getStartDate() != null) {
                        addItalicParagraph(document, proj.getStartDate() + " - " + proj.getEndDate(), 9);
                    }
                    if (proj.getDescription() != null) {
                        addParagraph(document, proj.getDescription(), 10);
                    }
                    if (proj.getTechStack() != null) {
                        addItalicParagraph(document, (isZh ? "技术栈：" : "Tech Stack: ") + proj.getTechStack(), 9);
                    }
                    if (proj.getHighlights() != null) {
                        for (String highlight : proj.getHighlights()) {
                            addBulletParagraph(document, highlight, 10);
                        }
                    }
                    addEmptyLine(document);
                }
            }

            // 教育背景
            if (content.getEducation() != null && !content.getEducation().isEmpty()) {
                addSectionTitle(document, isZh ? "教育背景" : "Education");
                for (ResumeContent.Education edu : content.getEducation()) {
                    String eduTitle = edu.getSchool() + " — " + edu.getDegree() + " · " + edu.getMajor();
                    addBoldParagraph(document, eduTitle, 11);
                    if (edu.getStartDate() != null) {
                        addItalicParagraph(document, edu.getStartDate() + " - " + edu.getEndDate(), 9);
                    }
                    if (edu.getDescription() != null) {
                        addParagraph(document, edu.getDescription(), 10);
                    }
                }
                addEmptyLine(document);
            }

            // 技能
            if (content.getSkills() != null && !content.getSkills().isEmpty()) {
                addSectionTitle(document, isZh ? "专业技能" : "Skills");
                addParagraph(document, String.join("、", content.getSkills()), 10);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);

            log.info("Word导出成功, 大小: {} bytes", outputStream.size());
            return outputStream.toByteArray();
        } catch (IOException e) {
            log.error("Word导出失败: {}", e.getMessage(), e);
            throw new RuntimeException("Word导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存导出文件到本地
     */
    public String saveExportFile(byte[] data, String format, Long resumeId) {
        try {
            Path dir = Paths.get(uploadPath, "exports");
            Files.createDirectories(dir);

            String filename = "resume_" + resumeId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + format;
            Path filePath = dir.resolve(filename);
            Files.write(filePath, data);

            log.info("导出文件已保存: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("保存导出文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("保存导出文件失败: " + e.getMessage(), e);
        }
    }

    // =============== Word 文档辅助方法 ===============

    private void addTitle(XWPFDocument doc, String text) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(22);
        run.setFontFamily("SimSun");
    }

    private void addSectionTitle(XWPFDocument doc, String text) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setSpacingBefore(200);
        paragraph.setBorderBottom(Borders.SINGLE);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(14);
        run.setFontFamily("SimHei");
    }

    private void addParagraph(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setFontFamily("SimSun");
    }

    private void addCenteredParagraph(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setFontFamily("SimSun");
    }

    private void addBoldParagraph(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(fontSize);
        run.setFontFamily("SimSun");
    }

    private void addItalicParagraph(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph paragraph = doc.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setItalic(true);
        run.setFontSize(fontSize);
        run.setColor("666666");
        run.setFontFamily("SimSun");
    }

    private void addBulletParagraph(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph paragraph = doc.createParagraph();
        paragraph.setIndentationLeft(400);
        XWPFRun run = paragraph.createRun();
        run.setText("• " + text);
        run.setFontSize(fontSize);
        run.setFontFamily("SimSun");
    }

    private void addEmptyLine(XWPFDocument doc) {
        doc.createParagraph();
    }
}
