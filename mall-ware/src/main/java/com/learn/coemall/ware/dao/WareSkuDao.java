package com.learn.coemall.ware.dao;

import com.learn.coemall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:37:26
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

}
