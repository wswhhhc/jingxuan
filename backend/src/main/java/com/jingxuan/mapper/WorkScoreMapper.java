package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.WorkScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface WorkScoreMapper extends BaseMapper<WorkScore> {

    @Select("SELECT * FROM work_score WHERE work_id = #{workId} AND deleted = 0")
    List<WorkScore> selectByWorkId(@Param("workId") Long workId);

    @Select("SELECT * FROM work_score WHERE work_id = #{workId} AND teacher_id = #{teacherId} AND deleted = 0")
    WorkScore selectByWorkAndTeacher(@Param("workId") Long workId, @Param("teacherId") Long teacherId);

    /**
     * INSERT … ON DUPLICATE KEY UPDATE 原子化 upsert
     * 利用 uk_work_teacher 唯一键判断是否已存在，避免并发下的竞态条件
     */
    @Update({"<script>",
            "INSERT INTO work_score (id, work_id, teacher_id, batch_id, innovation, difficulty, completion, practicality, total, comment, create_time, update_time, deleted)",
            "VALUES (#{id}, #{workId}, #{teacherId}, #{batchId}, #{innovation}, #{difficulty}, #{completion}, #{practicality}, #{total}, #{comment}, NOW(), NOW(), 0)",
            "ON DUPLICATE KEY UPDATE",
            "    innovation = VALUES(innovation),",
            "    difficulty = VALUES(difficulty),",
            "    completion = VALUES(completion),",
            "    practicality = VALUES(practicality),",
            "    total = VALUES(total),",
            "    comment = VALUES(comment),",
            "    update_time = NOW()",
            "</script>"})
    int upsert(@Param("id") Long id,
               @Param("workId") Long workId,
               @Param("teacherId") Long teacherId,
               @Param("batchId") Long batchId,
               @Param("innovation") BigDecimal innovation,
               @Param("difficulty") BigDecimal difficulty,
               @Param("completion") BigDecimal completion,
               @Param("practicality") BigDecimal practicality,
               @Param("total") BigDecimal total,
               @Param("comment") String comment);
}
