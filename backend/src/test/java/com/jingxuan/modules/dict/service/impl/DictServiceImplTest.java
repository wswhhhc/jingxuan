package com.jingxuan.modules.dict.service.impl;

import com.jingxuan.entity.SysDict;
import com.jingxuan.mapper.SysDictMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DictServiceImpl - 字典服务")
class DictServiceImplTest {

    @Mock private SysDictMapper sysDictMapper;

    private DictServiceImpl dictService;

    @BeforeEach
    void setUp() {
        dictService = new DictServiceImpl();
        ReflectionTestUtils.setField(dictService, "baseMapper", sysDictMapper);
    }

    @Nested
    @DisplayName("字典 CRUD")
    class DictCrud {

        @Test
        @DisplayName("成功创建字典项")
        void shouldCreateDict() {
            SysDict dict = new SysDict();
            dict.setDictType("class");
            dict.setDictLabel("2022级软件1班");
            dict.setDictValue("2022_soft_1");

            when(sysDictMapper.insert(any(SysDict.class))).thenAnswer(inv -> {
                ((SysDict) inv.getArgument(0)).setId(1L);
                return 1;
            });

            Long id = dictService.createDict(dict);
            assertEquals(1L, id);
            verify(sysDictMapper).insert(dict);
        }

        @Test
        @DisplayName("成功更新字典项")
        void shouldUpdateDict() {
            SysDict dict = new SysDict();
            dict.setId(1L);
            dict.setDictLabel("新标签");

            dictService.updateDict(dict);
            verify(sysDictMapper).updateById(dict);
        }
    }
}
