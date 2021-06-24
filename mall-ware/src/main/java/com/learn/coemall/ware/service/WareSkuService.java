package com.learn.coemall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.coemall.ware.vo.WareSkuLockVo;
import com.learn.common.to.SkuHasStockTo;
import com.learn.common.to.mq.OrderTo;
import com.learn.common.to.mq.StockLockedTo;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.ware.entity.WareSkuEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:37:26
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockTo> getSkuHasStock(List<Long> skuIds);

    /**
     * 为某个订单锁库存
     */
    Boolean orderLockStock(WareSkuLockVo vo);

    void handleStockLockedRelease(StockLockedTo to);

    void handleOrderCloseRelease(OrderTo orderTo);
}

