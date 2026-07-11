package com.jingxuan.modules.userimport.service;

import com.jingxuan.modules.userimport.dto.AiUserImportRequest;
import com.jingxuan.modules.userimport.dto.AiUserImportResponse;

public interface AiUserImportService {

    AiUserImportResponse parse(AiUserImportRequest request);
}
