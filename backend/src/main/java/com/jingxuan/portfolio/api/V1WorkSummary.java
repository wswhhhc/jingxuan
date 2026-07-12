package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkListVO;
import java.time.ZoneOffset;

/** v1 作品列表项。 */
public record V1WorkSummary(String id, String title, String summary, String status, String submitterId,
                            String submitterName, String batchId, String submittedAt, java.util.List<String> tags) {
}
