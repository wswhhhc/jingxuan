package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkMemberDTO;
import com.jingxuan.modules.work.dto.WorkRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 编辑草稿或驳回作品的 v1 输入；省略字段表示保持原值。 */
public record V1UpdateWorkRequest(
        @Size(max = 200) String title,
        String summary, String techStack, String advisor, String coverUrl, String previewUrl, String runDescription,
        @Valid List<WorkMemberDTO> members,
        List<@Pattern(regexp = "[0-9]{1,19}") String> attachmentIds) {
    public WorkRequest toLegacyRequest() {
        WorkRequest request = new WorkRequest();
        request.setTitle(title); request.setSummary(summary); request.setTechStack(techStack); request.setAdvisor(advisor);
        request.setCoverUrl(coverUrl); request.setPreviewUrl(previewUrl); request.setRunDesc(runDescription);
        request.setMembers(members); request.setAttachmentIds(attachmentIds);
        return request;
    }
}
