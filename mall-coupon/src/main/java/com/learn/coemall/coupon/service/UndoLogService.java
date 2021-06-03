package com.learn.coemall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.coupon.entity.UndoLogEntity;

import java.util.Map;

/**
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:19:05
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

