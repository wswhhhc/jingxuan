package com.jingxuan.campaign.internal.application;

import com.jingxuan.entity.ScoreBatch;
import com.jingxuan.entity.SysDict;
import com.jingxuan.entity.SysUser;
import com.jingxuan.mapper.ScoreBatchMapper;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.mapper.SysUserMapper;
import com.jingxuan.modules.task.service.StudentTaskService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CampaignQueryServiceTest {
    private final ScoreBatchMapper batchMapper = mock(ScoreBatchMapper.class);
    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final SysDictMapper dictMapper = mock(SysDictMapper.class);
    private final StudentTaskService taskService = mock(StudentTaskService.class);
    private final CampaignQueryService service = new CampaignQueryService(batchMapper, userMapper, dictMapper, taskService);

    @Test
    void filtersBatchesByLegacyClassValueAndMapsOffsetTime() {
        SysUser user = new SysUser(); user.setClassId(1L);
        SysDict clazz = new SysDict(); clazz.setId(1L); clazz.setDictValue("soft_2401");
        ScoreBatch matching = batch(1L, "[\"soft_2401\"]");
        ScoreBatch other = batch(2L, "[\"other\"]");
        when(userMapper.selectById(7L)).thenReturn(user);
        when(dictMapper.selectById(1L)).thenReturn(clazz);
        when(batchMapper.selectList(any())).thenReturn(List.of(matching, other));

        var batches = service.availableBatches(7L);

        assertEquals(1, batches.size());
        assertEquals("1", batches.get(0).id());
        assertEquals("+08:00", batches.get(0).startAt().substring(batches.get(0).startAt().length() - 6));
    }

    private static ScoreBatch batch(Long id, String scopes) {
        ScoreBatch batch = new ScoreBatch();
        batch.setId(id); batch.setBatchName("批次"); batch.setClassScopes(scopes); batch.setStatus(1);
        batch.setStartTime(LocalDateTime.now().minusDays(1)); batch.setEndTime(LocalDateTime.now().plusDays(1));
        return batch;
    }
}
