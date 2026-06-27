package com.resume.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 加载器 - 外部化 Prompt 管理
 * 从 classpath:prompts/*.txt 加载 Prompt 模板
 */
@Component
public class PromptLoader {

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * 加载 Prompt 文件内容
     * @param path 类路径下的 Prompt 文件路径，例如 "prompts/interviewer.txt"
     * @return Prompt 文本内容
     */
    public String load(String path) {
        return cache.computeIfAbsent(path, this::doLoad);
    }

    private String doLoad(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return Files.readString(
                    resource.getFile().toPath(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt: " + path, e);
        }
    }
}
