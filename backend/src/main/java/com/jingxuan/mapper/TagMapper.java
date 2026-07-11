package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Delete("DELETE FROM tag WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
