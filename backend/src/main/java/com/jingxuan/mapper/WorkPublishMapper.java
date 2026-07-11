package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.WorkPublish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkPublishMapper extends BaseMapper<WorkPublish> {

    @Select("SELECT * FROM work_publish WHERE work_id = #{workId} AND deleted = 0")
    WorkPublish selectByWorkId(Long workId);
}
