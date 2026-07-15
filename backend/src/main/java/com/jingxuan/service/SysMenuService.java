package com.jingxuan.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.jingxuan.entity.SysMenu;

import java.util.List;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> getMenuTree();
}

