package com.resume.util;

/**
 * JSON 工具类 - 标准化的 JSON 提取和清理方法
 * 遵循 AgentScope 规范：必须 try-catch + 降级默认值
 */
public final class JsonUtils {

    private JsonUtils() {
        // 工具类，禁止实例化
    }

    /**
     * 从 LLM 输出中提取 JSON 对象
     * @param text LLM 原始输出
     * @return JSON 字符串（如果找到）或原始文本
     */
    public static String extractJson(String text) {
        if (text == null) return null;
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * 从 LLM 输出中提取 JSON 数组
     * @param text LLM 原始输出
     * @return JSON 数组字符串（如果找到）或原始文本
     */
    public static String extractJsonArray(String text) {
        if (text == null) return null;
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    /**
     * 去掉 Markdown 代码块包裹
     * @param raw LLM 原始输出
     * @return 清理后的文本
     */
    public static String sanitize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        s = s.replaceAll("(?s)^```(?:json|text)?\\s*", "")
             .replaceAll("(?s)\\s*```$", "");
        return s.trim();
    }

    /**
     * 完整的 JSON 清理流程：去 Markdown 包裹 + 提取 JSON
     * @param raw LLM 原始输出
     * @return 干净的 JSON 字符串
     */
    public static String cleanAndExtractJson(String raw) {
        String sanitized = sanitize(raw);
        return extractJson(sanitized);
    }

    /**
     * 完整的 JSON 数组清理流程：去 Markdown 包裹 + 提取 JSON 数组
     * @param raw LLM 原始输出
     * @return 干净的 JSON 数组字符串
     */
    public static String cleanAndExtractJsonArray(String raw) {
        String sanitized = sanitize(raw);
        return extractJsonArray(sanitized);
    }
}
