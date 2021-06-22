package com.learn.coemall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.coemall.order.vo.OrderConfirmVo;
import com.learn.coemall.order.vo.OrderSubmitVo;
import com.learn.coemall.order.vo.SubmitOrderResponseVo;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:34:09
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);
}

