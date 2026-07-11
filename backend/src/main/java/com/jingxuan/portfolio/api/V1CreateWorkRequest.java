package com.jingxuan.portfolio.api;

import com.jingxuan.modules.work.dto.WorkMemberDTO;
import com.jingxuan.modules.work.dto.WorkRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 创建作品草稿的 v1 输入。雪花 ID 均以字符串传递。 */
public record V1CreateWorkRequest(
        @NotBlank @Size(max = 200) String title,
        String summary, String techStack, String advisor, String coverUrl, String previewUrl, String runDescription,
        @Valid List<WorkMemberDTO> members,
        List<@Pattern(regexp = "[0-9]{1,19}") String> attachmentIds,
        @Pattern(regexp = "[0-9]{1,19}") String batchId) {

    public WorkRequest toLegacyRequest() {
        WorkRequest request = new WorkRequest();
        request.setTitle(title);
        request.setSummary(summary);
        request.setTechStack(techStack);
        request.setAdvisor(advisor);
        request.setCoverUrl(coverUrl);
        request.setPreviewUrl(previewUrl);
        request.setRunDesc(runDescription);
        request.setMembers(members);
        request.setAttachmentIds(attachmentIds);
        request.setBatchId(batchId == null ? null : Long.valueOf(batchId));
        return request;
    }
}
