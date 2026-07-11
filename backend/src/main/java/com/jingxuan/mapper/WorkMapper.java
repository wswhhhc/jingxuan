package com.jingxuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jingxuan.entity.Work;
import com.jingxuan.modules.rank.dto.RankVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WorkMapper extends BaseMapper<Work> {

    /**
     * 综合排行榜查询（XML 定义）
     */
    List<RankVO> selectRankList(@Param("batchId") Long batchId, @Param("techStack") String techStack);

    /**
     * 获取有评分数据的作品涉及的技术栈分类
     */
    List<String> selectDistinctTechStacksFromRanked(@Param("batchId") Long batchId);

    /**
     * 统计指定批次中有评分数据的作品数
     */
    Long countRankedWorksByBatch(@Param("batchId") Long batchId);

    /**
     * 评分分布统计（按分数段）
     */
    List<Map<String, Object>> selectScoreDistribution();

    /**
     * 查询作品在同批次中的排名
     */
    Integer selectWorkRank(@Param("workId") Long workId, @Param("batchId") Long batchId);
}
