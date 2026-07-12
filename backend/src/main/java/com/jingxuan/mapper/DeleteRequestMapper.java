package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.DeleteRequest;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeleteRequestMapper extends BaseMapper<DeleteRequest> {
    @Delete("DELETE FROM delete_request WHERE student_id = #{studentId}")
    int physicalDeleteByStudentId(@Param("studentId") Long studentId);
}
