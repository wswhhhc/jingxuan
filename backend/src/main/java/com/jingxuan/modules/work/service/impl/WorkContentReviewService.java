package com.jingxuan.modules.work.service.impl;

import com.jingxuan.exception.BusinessException;
import com.jingxuan.modules.sensitive.service.DeepSeekReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class WorkContentReviewService {

    private final DeepSeekReviewService deepSeekReviewService;

    void review(String title, String summary, String runDesc) {
        String reviewText = String.join("\n",
                title != null ? title : "",
                summary != null ? summary : "",
                runDesc != null ? runDesc : ""
        ).trim();
        if (reviewText.isEmpty()) {
            return;
        }

        DeepSeekReviewService.ReviewResult review = deepSeekReviewService.review(reviewText, "work");
        if (!review.isPassed()) {
            throw new BusinessException("作品内容违规：" + review.getReason());
        }
    }
}
