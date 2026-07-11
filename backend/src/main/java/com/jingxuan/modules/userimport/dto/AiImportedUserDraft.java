package com.jingxuan.modules.userimport.dto;

import lombok.Data;

@Data
public class AiImportedUserDraft {

    private String username;

    private String password;

    private String realName;

    private Integer roleId;

    private String roleName;

    private Long classId;

    private String className;

    private String phone;

    private String email;

    private Integer status;
}
