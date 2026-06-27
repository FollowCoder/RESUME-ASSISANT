package com.resume.service;

import com.resume.util.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

@Service
public class FileParserService {

    /**
     * 解析 PDF 文件，提取纯文本内容
     */
    public String parsePdf(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             PDDocument document = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).trim();
        } catch (IOException e) {
            throw new BusinessException("PDF文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 Word (.docx) 文件，提取纯文本内容
     */
    public String parseDocx(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            return document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new BusinessException("Word文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 Word (.doc) 文件，提取纯文本内容
     */
    public String parseDoc(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             HWPFDocument document = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText().trim();
        } catch (IOException e) {
            throw new BusinessException("Doc文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 根据文件扩展名/Content-Type 自动选择解析器
     */
    public String parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("无法识别文件类型，请上传 PDF 或 Word 文件");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "pdf" -> parsePdf(file);
            case "docx" -> parseDocx(file);
            case "doc" -> parseDoc(file);
            default -> throw new BusinessException("不支持的文件格式: ." + extension + "，仅支持 .pdf, .docx, .doc");
        };
    }
}
