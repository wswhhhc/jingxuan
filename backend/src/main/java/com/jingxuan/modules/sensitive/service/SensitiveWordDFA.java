package com.jingxuan.modules.sensitive.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * DFA (确定性有限自动机) 敏感词过滤器
 * <p>
 * 基于 Trie 树实现，将敏感词列表编译为状态机，扫描文本时 O(n) 时间复杂度完成匹配。
 * 仅收录高置信度的敏感词（在中文语境下几乎总是违规的词汇），
 * 边缘/歧义情况交由 AI 审核处理。
 * </p>
 */
@Slf4j
@Component
public class SensitiveWordDFA {

    private final TrieNode root = new TrieNode();

    private static class TrieNode {
        private final Map<Character, TrieNode> children = new HashMap<>();
        private boolean isEnd = false;
    }

    @PostConstruct
    public void init() {
        loadBuiltinWords();
        log.info("SensitiveWordDFA 初始化完成，共 {} 个敏感词", countWords());
    }

    // ==================== 公开 API ====================

    /**
     * 添加一个敏感词
     */
    public void addWord(String word) {
        if (word == null || word.isBlank()) return;
        String w = word.trim().toLowerCase(Locale.ROOT);
        TrieNode node = root;
        for (int i = 0; i < w.length(); i++) {
            char c = w.charAt(i);
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd = true;
    }

    /**
     * 批量添加敏感词
     */
    public void addWords(Collection<String> words) {
        for (String word : words) {
            addWord(word);
        }
    }

    /**
     * 检查文本是否包含敏感词（O(n)）
     */
    public boolean contains(String text) {
        if (text == null || text.isEmpty()) return false;
        String lower = text.toLowerCase(Locale.ROOT);

        for (int i = 0; i < lower.length(); i++) {
            TrieNode node = root;
            for (int j = i; j < lower.length(); j++) {
                node = node.children.get(lower.charAt(j));
                if (node == null) break;
                if (node.isEnd) return true;
            }
        }
        return false;
    }

    /**
     * 找出文本中所有命中的敏感词及其位置
     */
    public List<Hit> findAll(String text) {
        List<Hit> hits = new ArrayList<>();
        if (text == null || text.isEmpty()) return hits;
        String lower = text.toLowerCase(Locale.ROOT);

        for (int i = 0; i < lower.length(); i++) {
            TrieNode node = root;
            for (int j = i; j < lower.length(); j++) {
                node = node.children.get(lower.charAt(j));
                if (node == null) break;
                if (node.isEnd) {
                    hits.add(new Hit(text.substring(i, j + 1), i, j + 1));
                }
            }
        }
        return hits;
    }

    /**
     * 将敏感词替换为指定字符（如 *）
     */
    public String replace(String text, char c) {
        if (text == null || text.isEmpty()) return text;
        String lower = text.toLowerCase(Locale.ROOT);
        boolean[] mask = new boolean[text.length()];

        for (int i = 0; i < lower.length(); i++) {
            TrieNode node = root;
            for (int j = i; j < lower.length(); j++) {
                node = node.children.get(lower.charAt(j));
                if (node == null) break;
                if (node.isEnd) {
                    Arrays.fill(mask, i, j + 1, true);
                }
            }
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            sb.append(mask[i] ? c : text.charAt(i));
        }
        return sb.toString();
    }

    /**
     * 当前词库中敏感词数量
     */
    public int countWords() {
        return count(root);
    }

    // ==================== 内部实现 ====================

    private int count(TrieNode node) {
        int n = node.isEnd ? 1 : 0;
        for (TrieNode child : node.children.values()) {
            n += count(child);
        }
        return n;
    }

    private void loadBuiltinWords() {
        // ── 1. 辱骂攻击 ──
        addWords(List.of(
                "傻逼", "傻b", "煞笔", "沙比", "傻比",
                "草泥马", "操你妈", "操你", "操尼玛",
                "日你妈", "日你", "日尼玛",
                "去你妈的", "去尼玛的",
                "去死", "滚蛋", "滚犊子",
                "他妈的", "他妈", "特么的", "特么",
                "妈逼", "麻痹", "马勒戈壁",
                "贱人", "贱货",
                "废物", "蠢货", "蠢猪",
                "脑残", "智障", "白痴",
                "nmsl", "cnmd", "wcnm", "tmd",
                "fuck", "shit", "bitch", "asshole", "idiot"
        ));

        // ── 2. 色情低俗 ──
        addWords(List.of(
                "约炮", "约p",
                "裸聊", "裸照", "裸体",
                "嫖娼", "卖淫", "招嫖",
                "迷药", "催情药",
                "一夜情",
                "援交", "包养",
                "av", "三级片", "毛片", "a片",
                "porn"
        ));

        // ── 3. 暴力血腥 ──
        addWords(List.of(
                "杀人", "自杀", "自残",
                "砍人", "捅人", "碎尸",
                "恐怖袭击", "炸弹",
                "血腥"
        ));

        // ── 4. 政治敏感 ──
        addWords(List.of(
                "法轮功", "flg",
                "台独", "藏独", "疆独", "港独",
                "邪教"
        ));

        // ── 5. 广告引流 ──
        addWords(List.of(
                "微信号", "qq号",
                "加微信", "加qq", "加vx",
                "兼职日结", "工资日结",
                "代写作业", "代写论文", "代做", "代考",
                "刷单", "刷钻"
        ));
    }

    // ==================== 内部类型 ====================

    /**
     * 匹配命中结果
     *
     * @param word  命中的敏感词原文
     * @param start 起始索引（含）
     * @param end   结束索引（不含）
     */
    public record Hit(String word, int start, int end) {

        @Override
        public String toString() {
            return String.format("「%s」@[%d,%d)", word, start, end);
        }
    }
}
