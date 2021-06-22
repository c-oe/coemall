package com.learn.coemall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.coemall.ware.vo.WareSkuLockVo;
import com.learn.common.to.SkuHasStockTo;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.ware.entity.WareSkuEntity;

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
     * @return
     */
    Boolean orderLockStock(WareSkuLockVo vo);
}

