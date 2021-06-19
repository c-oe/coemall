package com.learn.coemall.member.service.impl;

import com.learn.coemall.member.dao.MemberLevelDao;
import com.learn.coemall.member.entity.MemberLevelEntity;
import com.learn.coemall.member.exception.PhoneExsitException;
import com.learn.coemall.member.exception.UsernameExistException;
import com.learn.coemall.member.vo.MemberLoginVo;
import com.learn.coemall.member.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;

import com.learn.coemall.member.dao.MemberDao;
import com.learn.coemall.member.entity.MemberEntity;
import com.learn.coemall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {
        MemberEntity entity = new MemberEntity();

        //默认等级
        MemberLevelEntity levelEntity = memberLevelDao.selectOne(new QueryWrapper<MemberLevelEntity>().eq("default_status", 1));
        entity.setLevelId(levelEntity.getId());

        checkPhoneUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());

        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        //spring盐值加密
        String encode = new BCryptPasswordEncoder().encode(vo.getPassword());
        entity.setPassword(encode);

        //其他信息......

        baseMapper.insert(entity);
    }

    @Override
    public void checkPhoneUnique(String phone) throws PhoneExsitException{
        if (baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("mobile",phone)) != null){
            throw new PhoneExsitException();
        }
    }

    @Override
    public void checkUserNameUnique(String userName) throws UsernameExistException{
        if (baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username",userName)) != null){
            throw new UsernameExistException();
        }

    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct)
                .or().eq("mobile", loginacct));
        if (entity != null){
            String passwordDb = entity.getPassword();
            boolean matches = new BCryptPasswordEncoder().matches(password, passwordDb);
            if(matches){
                return entity;
            }else{
                return null;
            }
        }else {
            return null;
        }
    }

}