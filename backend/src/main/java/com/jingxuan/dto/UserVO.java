package com.jingxuan.dto;

import com.jingxuan.entity.SysUser;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
public class UserVO extends SysUser {

    private String roleName;
    private String className;

    public static UserVO from(SysUser user, String roleName, String className) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        vo.setRoleName(roleName);
        vo.setClassName(className);
        return vo;
    }
}
