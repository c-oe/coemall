package com.learn.coemall.order.dao;

import com.learn.coemall.order.entity.RefundInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款信息
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:34:09
 */
@Mapper
public interface RefundInfoDao extends BaseMapper<RefundInfoEntity> {

}
