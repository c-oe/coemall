package com.learn.coemall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.member.entity.UndoLogEntity;

import java.util.Map;

/**
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:25:15
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

