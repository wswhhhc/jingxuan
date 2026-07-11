package com.jingxuan.portfolio.api;
import com.jingxuan.modules.work.dto.WorkDetailVO;
public record V1WorkDetail(String id,String title,String summary,String status,String submitterId,String submitterName,java.util.List<String> tags){public static V1WorkDetail from(WorkDetailVO v){return new V1WorkDetail(v.getId().toString(),v.getTitle(),v.getSummary(),v.getStatusLabel(),v.getSubmitterId()==null?null:v.getSubmitterId().toString(),v.getSubmitterName(),v.getTags());}}
