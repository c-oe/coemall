package com.learn.coemall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.coemall.product.vo.AttrRespVo;
import com.learn.coemall.product.vo.AttrVo;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.product.entity.AttrEntity;

import java.util.Map;

/**
 * 商品属性
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 12:41:21
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId);

    AttrRespVo getAttrInfo(Long attrId);
}

