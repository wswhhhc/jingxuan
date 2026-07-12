package com.jingxuan.identityaccess;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UserApprovalPermissionMigrationTest {

    @Test
    void grantsTheDedicatedApprovalPermissionToTheBuiltInAdministratorRole() throws Exception {
        String migration = new ClassPathResource("db/migration/V3__grant_teacher_approval_permission.sql")
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(migration.contains("'user:approve'"));
        assertTrue(migration.contains("VALUES (120, 3, 20)"));
    }
}
