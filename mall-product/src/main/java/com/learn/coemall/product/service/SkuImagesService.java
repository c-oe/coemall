package com.learn.coemall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.product.entity.SkuImagesEntity;

import java.util.Map;

/**
 * sku图片
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 12:41:21
 */
public interface SkuImagesService extends IService<SkuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

