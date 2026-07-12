package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.SysNotification;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysNotificationMapper extends BaseMapper<SysNotification> {
    @Delete("DELETE FROM sys_notification WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);
}
