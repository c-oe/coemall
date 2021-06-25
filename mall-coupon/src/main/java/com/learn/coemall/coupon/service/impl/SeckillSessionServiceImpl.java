package com.learn.coemall.coupon.service.impl;

import com.learn.coemall.coupon.entity.SeckillSkuRelationEntity;
import com.learn.coemall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;

import com.learn.coemall.coupon.dao.SeckillSessionDao;
import com.learn.coemall.coupon.entity.SeckillSessionEntity;
import com.learn.coemall.coupon.service.SeckillSessionService;
import org.springframework.util.CollectionUtils;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLatest3DaySession() {
        //计算最近三天
        String startTime = startTime();
        String endTime = endTime();

        List<SeckillSessionEntity> list = list(new QueryWrapper<SeckillSessionEntity>().between("start_time", startTime, endTime));
        if (!CollectionUtils.isEmpty(list)){
            return list.stream().map(session -> {
                Long sessionId = session.getId();
                List<SeckillSkuRelationEntity> relationEntities = seckillSkuRelationService.list(new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", sessionId));
                session.setRelationSkus(relationEntities);
                return session;
            }).collect(Collectors.toList());
        }
        return null;
    }

    private String startTime(){
        LocalDate now = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(now, LocalTime.MIN);

        return start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String endTime(){
        LocalDate plus = LocalDate.now().plus(Duration.ofDays(2));
        LocalDateTime end = LocalDateTime.of(plus, LocalTime.MAX);

        return end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

}