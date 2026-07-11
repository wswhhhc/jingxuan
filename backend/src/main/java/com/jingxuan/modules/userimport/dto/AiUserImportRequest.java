package com.jingxuan.modules.userimport.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiUserImportRequest {

    private List<AiImportMessage> messages;
}
