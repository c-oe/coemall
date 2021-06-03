package com.learn.coemall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.order.entity.MqMessageEntity;

import java.util.Map;

/**
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:34:09
 */
public interface MqMessageService extends IService<MqMessageEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

