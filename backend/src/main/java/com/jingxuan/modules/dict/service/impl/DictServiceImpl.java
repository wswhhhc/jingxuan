package com.jingxuan.modules.dict.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jingxuan.entity.SysDict;
import com.jingxuan.mapper.SysDictMapper;
import com.jingxuan.modules.dict.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典 服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictServiceImpl extends ServiceImpl<SysDictMapper, SysDict> implements DictService {

    @Override
    public List<SysDict> getByType(String dictType) {
        return lambdaQuery()
                .eq(SysDict::getDictType, dictType)
                .orderByAsc(SysDict::getSort)
                .list();
    }

    @Override
    public Map<String, List<SysDict>> getAllGroupByType() {
        List<SysDict> allList = lambdaQuery()
                .orderByAsc(SysDict::getSort)
                .list();
        return allList.stream()
                .collect(Collectors.groupingBy(SysDict::getDictType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDict(SysDict dict) {
        baseMapper.insert(dict);
        return dict.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDict(SysDict dict) {
        baseMapper.updateById(dict);
    }
}
