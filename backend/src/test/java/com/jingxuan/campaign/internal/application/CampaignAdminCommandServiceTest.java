package com.jingxuan.campaign.internal.application;

import com.jingxuan.campaign.api.V1BatchDetail;
import com.jingxuan.common.PageResult;
import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.modules.scorebatch.service.ScoreBatchService;
import com.jingxuan.exception.NotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampaignAdminCommandServiceTest {

    private final ScoreBatchService batchService = mock(ScoreBatchService.class);
    private final CampaignAdminCommandService service = new CampaignAdminCommandService(batchService);

    @Test
    void listsBatchesWithPagination() {
        ScoreBatch b = new ScoreBatch(); b.setId(1L); b.setBatchName("测试批次");
        when(batchService.queryBatchList(1, 20))
                .thenReturn(new PageResult<>(List.of(b), 1L, 1L, 20L));
        var page = service.listBatches(1, 20);
        assertEquals(1, page.items().size());
        assertEquals("测试批次", page.items().get(0).name());
    }

    @Test
    void getsBatchDetail() {
        ScoreBatch batch = new ScoreBatch();
        batch.setId(1L);
        batch.setBatchName("测试批次");
        when(batchService.getById(1L)).thenReturn(batch);

        V1BatchDetail detail = service.getBatch("1");
        assertEquals("测试批次", detail.name());
    }

    @Test
    void throwsForMissingBatch() {
        when(batchService.getById(999L)).thenReturn(null);
        assertThrows(NotFoundException.class, () -> service.getBatch("999"));
    }
}
