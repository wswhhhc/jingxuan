package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.StudentTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentTaskMapper extends BaseMapper<StudentTask> {
    @Delete("DELETE FROM student_task WHERE user_id = #{userId}")
    int physicalDeleteByUserId(@Param("userId") Long userId);
}
