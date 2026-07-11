package com.jingxuan.modules.userimport.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiUserImportResponse {

    private String assistantReply;

    private boolean ready;

    private List<String> requiredFields = new ArrayList<>();

    private List<String> optionalFields = new ArrayList<>();

    private List<String> missingFields = new ArrayList<>();

    private List<String> assumptions = new ArrayList<>();

    private List<AiImportedUserDraft> users = new ArrayList<>();
}
