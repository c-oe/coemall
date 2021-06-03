package com.learn.coemall.order.dao;

import com.learn.coemall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:34:09
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {

}
