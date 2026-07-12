package com.jingxuan.modules.work.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileUploadControllerTest {

    @Test
    void testFileUploadControllerInstantiation() {
        // FileUploadController uses @Autowired field injection;
        // verify it can be instantiated without NPE on basic operations
        new FileUploadController();
    }
}
