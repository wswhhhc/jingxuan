package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkListVO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

class V1WorkSummaryTest {
 @Test void mapsOpaqueIdsAndOffsetTime() {
  WorkListVO value=new WorkListVO(); value.setId(9007199254740993L); value.setSubmitterId(7L); value.setStatus(3); value.setSubmitTime(LocalDateTime.of(2026,1,1,8,0));
  var mapped=V1WorkSummary.from(value);
  assertEquals("9007199254740993", mapped.id()); assertEquals("APPROVED", mapped.status()); assertEquals("+08:00", mapped.submittedAt().substring(mapped.submittedAt().length()-6));
 }
}
