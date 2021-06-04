package com.learn.coemall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.coemall.ware.vo.MergeVo;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.ware.entity.PurchaseEntity;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:37:26
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceive(Map<String, Object> params);

    void mergePurchase(MergeVo mergeVo);

    void received(List<Long> ids);
}

