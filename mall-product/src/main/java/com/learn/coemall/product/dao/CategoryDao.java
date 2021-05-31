package com.learn.coemall.product.dao;

import com.learn.coemall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 12:41:21
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
