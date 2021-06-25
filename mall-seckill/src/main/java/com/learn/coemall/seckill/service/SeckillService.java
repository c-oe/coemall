package com.learn.coemall.seckill.service;

import com.learn.coemall.seckill.to.SecKillSkuRedisTo;

import java.util.List;

/**
 * @author coffee
 * @date 2021-06-24 15:10
 */
public interface SeckillService {

    void uploadSeckillSkuLatest3Days();

    /**
     * 获取当前时间可以参与秒杀的商品信息
     */
    List<SecKillSkuRedisTo> getCurrentSeckillSkus();

    SecKillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
