package com.learn.coemall.order.dao;

import com.learn.coemall.order.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单项信息
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:34:09
 */
@Mapper
public interface OrderItemDao extends BaseMapper<OrderItemEntity> {

}
