package com.learn.coemall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.learn.coemall.seckill.feign.CouponFeignService;
import com.learn.coemall.seckill.feign.ProductFeignService;
import com.learn.coemall.seckill.interceptor.LoginUserInterceptor;
import com.learn.coemall.seckill.service.SeckillService;
import com.learn.coemall.seckill.to.SecKillSkuRedisTo;
import com.learn.coemall.seckill.vo.SeckillSesssionsWithSkus;
import com.learn.coemall.seckill.vo.SkuInfoVo;
import com.learn.common.to.mq.SeckillOrderTo;
import com.learn.common.utils.R;
import com.learn.common.vo.MemberRespVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author coffee
 * @date 2021-06-24 15:10
 */
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";//+商品随机码

    @Override
    public void uploadSeckillSkuLatest3Days() {
        //扫描最近三天需要参与秒杀的活动
        R r = couponFeignService.getLatest3DaySession();
        if (r.getCode() == 0) {
            List<SeckillSesssionsWithSkus> sessions = (List<SeckillSesssionsWithSkus>) r.get("data");
            if (sessions != null) {
                //缓存到redis
                //活动信息
                saveSessionInfos(sessions);
                //活动关联的商品信息
                saveSessionSkuInfos(sessions);
            }
        }
    }

    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        //确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();

        Set<String> keys = redisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        if (keys != null) {
            for (String key : keys) {
                String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
                String[] s = replace.split("_");
                long start = Long.parseLong(s[0]);
                long end = Long.parseLong(s[1]);
                if (time >= start && time <= end) {
                    //获取当前场次所需商品信息
                    List<String> range = redisTemplate.opsForList().range(key, -100, 100);
                    BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                    List<String> list = ops.multiGet(range);
                    if (list != null) {
                        return list.stream().map(item -> {
                            return JSON.parseObject((String) item, SecKillSkuRedisTo.class);
                        }).collect(Collectors.toList());
                    }
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        //找到所有需要参与秒杀的商品的key
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = ops.keys();
        if (!CollectionUtils.isEmpty(keys)) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String s = ops.get(key);
                    SecKillSkuRedisTo redisTo = JSON.parseObject(s, SecKillSkuRedisTo.class);
                    long time = new Date().getTime();
                    if (!(time >= redisTo.getStartTime() && time <= redisTo.getEndTime())) {
                        redisTo.setRandomCode(null);
                    }
                    return redisTo;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        //获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String json = ops.get(killId);
        if (StringUtils.hasLength(json)) {
            SecKillSkuRedisTo redisTo = JSON.parseObject(json, SecKillSkuRedisTo.class);
            //校验合法性
            long time = new Date().getTime();
            long ttl = redisTo.getEndTime() - time;
            //时间
            if (time >= redisTo.getStartTime() && time <= redisTo.getEndTime()) {
                String randomCode = redisTo.getRandomCode();
                String skuId = redisTo.getPromotionSessionId() + "_" + redisTo.getSkuId();
                //随机码，商品id
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    //数量
                    if (num <= redisTo.getSeckillLimit()) {
                        //重复购买，幂等性
                        String redisKey = memberRespVo.getId() + "_" + skuId;
                        Boolean aBoolean = redisTemplate.opsForValue().setIfAbsent(redisKey, num.toString());
                        if (aBoolean) {
                            //分布式信号量 --
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE);
                            boolean b = semaphore.tryAcquire(num);
                            if (b) {
                                //秒杀成功，快速下单，向MQ发送消息
                                String timeId = IdWorker.getTimeId();
                                SeckillOrderTo to = new SeckillOrderTo();
                                to.setOrderSn(timeId);
                                to.setMemberId(memberRespVo.getId());
                                to.setNum(num);
                                to.setPromotionSessionId(redisTo.getPromotionSessionId());
                                to.setSkuId(redisTo.getSkuId());
                                to.setSeckillPrice(redisTo.getSeckillPrice());

                                rabbitTemplate.convertAndSend("order-event", "order.seckill.order", to);
                                return timeId;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSesssionsWithSkus> sessions) {
        sessions.forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;

            if (!redisTemplate.hasKey(key)) {
                List<String> collect = session.getRelationSkus().stream()
                        .map(item -> item.getPromotionSessionId().toString() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
                redisTemplate.opsForList().leftPushAll(key, collect);
            }

        });
    }

    private void saveSessionSkuInfos(List<SeckillSesssionsWithSkus> sessions) {
        sessions.forEach(session -> {
            //准备hash操作
            BoundHashOperations<String, Object, Object> ops = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            session.getRelationSkus().forEach(seckillSkuVo -> {
                if (!ops.hasKey(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString())) {
                    //sku基本信息
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = (SkuInfoVo) r.get("skuInfo");
                        redisTo.setSkuInfo(skuInfo);
                    }

                    //sku秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);
                    String s = JSON.toJSONString(redisTo);

                    //时间
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    //随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTo.setRandomCode(token);

                    //存入缓存
                    ops.put(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString(), s);

                    //分布式信号量,商品秒杀件数作为信号量(限流)
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            });
        });
    }
}
