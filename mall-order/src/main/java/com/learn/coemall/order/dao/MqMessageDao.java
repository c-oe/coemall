package com.learn.coemall.order.dao;

import com.learn.coemall.order.entity.MqMessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 
 * 
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:34:09
 */
@Mapper
public interface MqMessageDao extends BaseMapper<MqMessageEntity> {
	
}
