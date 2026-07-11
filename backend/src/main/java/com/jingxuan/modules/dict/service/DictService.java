package com.jingxuan.modules.dict.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jingxuan.entity.SysDict;

import java.util.List;
import java.util.Map;

public interface DictService extends IService<SysDict> {

    /**
     * 根据字典类型获取数据
     */
    List<SysDict> getByType(String dictType);

    /**
     * 获取所有字典（按类型分组）
     */
    Map<String, List<SysDict>> getAllGroupByType();

    /**
     * 创建字典项
     */
    Long createDict(SysDict dict);

    /**
     * 更新字典项
     */
    void updateDict(SysDict dict);
}
