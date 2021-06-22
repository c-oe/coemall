package com.learn.coemall.ware.service.impl;

import com.learn.coemall.ware.feign.MemberFeignService;
import com.learn.coemall.ware.vo.FareVo;
import com.learn.coemall.ware.vo.MemberAddressVo;
import com.learn.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;

import com.learn.coemall.ware.dao.WareInfoDao;
import com.learn.coemall.ware.entity.WareInfoEntity;
import com.learn.coemall.ware.service.WareInfoService;
import org.springframework.util.StringUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (StringUtils.hasLength(key)){
            queryWrapper.eq("id",key)
                    .or().like("name",key)
                    .or().like("address",key)
                    .or().like("areacode",key);
        }

        IPage<WareInfoEntity> page = this.page(new Query<WareInfoEntity>().getPage(params), queryWrapper);

        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long addrId) {

        FareVo fareVo = new FareVo();
        R r = memberFeignService.addrInfo(addrId);
        MemberAddressVo data = (MemberAddressVo) r.get("memberReceiveAddress");
        if (data != null){
            String phone = data.getPhone();
            String substring = phone.substring(phone.length() - 1, phone.length());
            BigDecimal bigDecimal = new BigDecimal(substring);
            fareVo.setAddress(data);
            fareVo.setFare(bigDecimal);
            return fareVo;
        }
        return null;
    }

}