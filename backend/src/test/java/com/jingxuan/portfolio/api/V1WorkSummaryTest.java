package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkListVO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;

class V1WorkSummaryTest {
 @Test void mapsOpaqueIdsAndOffsetTime() {
  WorkListVO value=new WorkListVO(); value.setId(9007199254740993L); value.setSubmitterId(7L); value.setStatus(3); value.setSubmitTime(LocalDateTime.of(2026,1,1,8,0));
  var mapped = toSummary(value);
  assertEquals("9007199254740993", mapped.id()); assertEquals("APPROVED", mapped.status()); assertEquals("+08:00", mapped.submittedAt().substring(mapped.submittedAt().length()-6));
 }
 private V1WorkSummary toSummary(WorkListVO v) {
  return new V1WorkSummary(idStr(v.getId()), v.getTitle(), v.getSummary(),
   switch (v.getStatus() == null ? 0 : v.getStatus()) { case 1 -> "SUBMITTED"; case 2 -> "REJECTED"; case 3 -> "APPROVED"; default -> "DRAFT"; },
   idStr(v.getSubmitterId()), v.getSubmitterName(), idStr(v.getBatchId()),
   v.getSubmitTime() == null ? null : v.getSubmitTime().atOffset(ZoneOffset.ofHours(8)).toString(), v.getTags());
 }
 private static String idStr(Long v) { return v == null ? null : v.toString(); }
}
