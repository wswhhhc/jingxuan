package com.jingxuan.modules.sensitive.service;

import com.jingxuan.common.Result;

/**
 * DeepSeek 内容审核服务
 */
public interface DeepSeekReviewService {

    /**
     * 审核文本内容
     * @param text 待审核文本
     * @param scene 审核场景（comment/score/profile）
     * @return 审核结果
     */
    ReviewResult review(String text, String scene);

    /**
     * 测试 DeepSeek API 连通性
     */
    Result<Void> testConnection();

    class ReviewResult {
        private boolean passed;
        private String category;
        private String reason;

        public static ReviewResult pass() {
            ReviewResult r = new ReviewResult();
            r.passed = true;
            return r;
        }

        public static ReviewResult fail(String category, String reason) {
            ReviewResult r = new ReviewResult();
            r.passed = false;
            r.category = category;
            r.reason = reason;
            return r;
        }

        public static ReviewResult error(String message) {
            ReviewResult r = new ReviewResult();
            r.passed = false;
            r.category = "system_error";
            r.reason = message;
            return r;
        }

        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}
