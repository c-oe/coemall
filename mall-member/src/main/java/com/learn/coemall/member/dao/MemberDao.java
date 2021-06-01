package com.learn.coemall.member.dao;

import com.learn.coemall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:25:15
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
