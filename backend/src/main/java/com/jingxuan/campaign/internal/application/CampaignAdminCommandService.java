package com.jingxuan.campaign.internal.application;

import com.jingxuan.campaign.api.V1BatchDetail;
import com.jingxuan.campaign.api.V1BatchPage;
import com.jingxuan.campaign.api.V1BatchRequest;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.exception.NotFoundException;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import com.jingxuan.api.V1PageInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 管理端批次管理与待办发布命令用例。 */
@Service
public class CampaignAdminCommandService {
    private final ScoreBatchService scoreBatchService;

    public CampaignAdminCommandService(ScoreBatchService scoreBatchService) {
        this.scoreBatchService = scoreBatchService;
    }

    @Transactional(rollbackFor = Exception.class)
    public V1BatchDetail createBatch(V1BatchRequest request) {
        ScoreBatch batch = new ScoreBatch();
        batch.setBatchName(request.batchName());
        batch.setClassScopes(request.classScopes());
        batch.setStartTime(request.startTime() == null ? null : LocalDateTime.parse(request.startTime()));
        batch.setEndTime(request.endTime() == null ? null : LocalDateTime.parse(request.endTime()));
        batch.setStatus(0); // DRAFT
        batch.setRankPublished(0);
        Long id = scoreBatchService.createBatch(batch);
        ScoreBatch saved = scoreBatchService.getById(id);
        if (saved == null) throw new BusinessException("创建批次失败");
        return V1BatchDetail.from(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public V1BatchDetail updateBatch(String id, V1BatchRequest request) {
        Long batchId = parseId(id);
        ScoreBatch batch = scoreBatchService.getById(batchId);
        if (batch == null) throw new NotFoundException("批次不存在");

        batch.setBatchName(request.batchName());
        batch.setClassScopes(request.classScopes());
        if (request.startTime() != null) batch.setStartTime(LocalDateTime.parse(request.startTime()));
        if (request.endTime() != null) batch.setEndTime(LocalDateTime.parse(request.endTime()));
        scoreBatchService.updateBatch(batch);

        ScoreBatch updated = scoreBatchService.getById(batchId);
        return V1BatchDetail.from(updated);
    }

    public V1BatchDetail getBatch(String id) {
        Long batchId = parseId(id);
        ScoreBatch batch = scoreBatchService.getById(batchId);
        if (batch == null) throw new NotFoundException("批次不存在");
        return V1BatchDetail.from(batch);
    }

    public V1BatchPage listBatches(int page, int pageSize) {
        PageResult<ScoreBatch> result = scoreBatchService.queryBatchList(page, pageSize);
        List<V1BatchDetail> items = result.getRecords().stream().map(V1BatchDetail::from).toList();
        return new V1BatchPage(
                V1PageInfo.of((int) result.getPageNum(), (int) result.getPageSize(), result.getTotal()),
                items);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(String id) {
        Long batchId = parseId(id);
        ScoreBatch batch = scoreBatchService.getById(batchId);
        if (batch == null) throw new NotFoundException("批次不存在");
        scoreBatchService.deleteBatch(batchId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveNotice(String id, String title, String content) {
        Long batchId = parseId(id);
        scoreBatchService.saveNotice(batchId, title, content);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishTasks(String id) {
        Long batchId = parseId(id);
        scoreBatchService.publishTask(batchId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishRanking(String id) {
        Long batchId = parseId(id);
        scoreBatchService.publishRanking(batchId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unpublishRanking(String id) {
        Long batchId = parseId(id);
        scoreBatchService.unpublishRanking(batchId);
    }

    private static Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(400, "无效的批次 ID");
        }
    }
}
