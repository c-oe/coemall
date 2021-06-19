package com.learn.coemall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.learn.coemall.member.exception.PhoneExsitException;
import com.learn.coemall.member.exception.UsernameExistException;
import com.learn.coemall.member.vo.MemberLoginVo;
import com.learn.coemall.member.vo.MemberRegistVo;
import com.learn.common.utils.PageUtils;
import com.learn.coemall.member.entity.MemberEntity;

import java.util.Map;

/**
 * 会员
 *
 * @author coffee
 * @email coffee@gmail.com
 * @date 2021-05-31 15:25:15
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    void checkPhoneUnique(String phone) throws PhoneExsitException;

    void checkUserNameUnique(String userName) throws UsernameExistException;

    MemberEntity login(MemberLoginVo vo);
}

