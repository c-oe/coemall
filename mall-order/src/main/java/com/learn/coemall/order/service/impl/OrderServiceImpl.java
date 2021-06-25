package com.learn.coemall.order.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.learn.coemall.order.constant.OrderConstant;
import com.learn.coemall.order.entity.OrderItemEntity;
import com.learn.coemall.order.entity.PaymentInfoEntity;
import com.learn.coemall.order.enume.OrderStatusEnum;
import com.learn.coemall.order.feign.CartFeignService;
import com.learn.coemall.order.feign.MemberFeignService;
import com.learn.coemall.order.feign.ProductFeignService;
import com.learn.coemall.order.feign.WmsFeignService;
import com.learn.coemall.order.interceptor.LoginUserInterceptor;
import com.learn.coemall.order.service.OrderItemService;
import com.learn.coemall.order.service.PaymentInfoService;
import com.learn.coemall.order.to.OrderCreateTo;
import com.learn.coemall.order.vo.*;
import com.learn.common.exception.NoStockException;
import com.learn.common.to.mq.OrderTo;
import com.learn.common.to.mq.SeckillOrderTo;
import com.learn.common.utils.R;
import com.learn.common.vo.MemberRespVo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;

import com.learn.coemall.order.dao.OrderDao;
import com.learn.coemall.order.entity.OrderEntity;
import com.learn.coemall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private WmsFeignService wmsFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();//每一个线程共享数据

        //远程查询所有的收货地址列表
        CompletableFuture<Void> async = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);//每一个线程共享数据
            confirmVo.setAddress(memberFeignService.getAddress(memberRespVo.getId()));
        }, executor);

        //远程查询购物车所有选中的购物项
        CompletableFuture<Void> async1 = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);//每一个线程共享数据
            confirmVo.setItems(cartFeignService.getCurrentUserCartItems());
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R hasStock = wmsFeignService.getSkusHasStock(collect);
            List<SkuStockVo> data = (List<SkuStockVo>) hasStock.get("data");
            if (data != null){
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        });

        //查询用户积分
        confirmVo.setIntegration(memberRespVo.getIntegration());

        //防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(),token,30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(async,async1).get();

        return confirmVo;
    }

//    @GlobalTransactional
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        response.setCode(0);
        //验证令牌
        String orderToken = vo.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if (result == 0L){
            return response;
        }else {
            //验证令牌成功
            OrderCreateTo order = createOrder();
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01){
                saveOrder(order);
                //库存锁定，只要有异常就回滚
                WareSkuLockVo wareSkuLockVo = new WareSkuLockVo();
                wareSkuLockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                wareSkuLockVo.setLocks(orderItemVos);
                //远程所库存
                R r = wmsFeignService.orderLockStock(wareSkuLockVo);
                if (r.getCode() == 0){
                    response.setOrder(order.getOrder());
                    //创建成功给MQ发送消息
                    rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",order.getOrder());

                    return response;
                }else {
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            }else {
                response.setCode(2);
                return response;
            }
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        //查询当前订单最新状态
        OrderEntity orderEntity = getById(entity.getId());
        if (orderEntity.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())){
            //关单
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity,orderTo);
            try {
                rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderTo);
            }catch (Exception e){

            }

        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();

        OrderEntity order = getOrderByOrderSn(orderSn);
        BigDecimal payAmount = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        OrderItemEntity itemEntity = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn)).get(0);

        payVo.setTotal_amount(payAmount.toString());
        payVo.setOut_trade_no(orderSn);
        payVo.setSubject(itemEntity.getSkuName());
        payVo.setBody(itemEntity.getSkuAttrsVals());

        return payVo;
    }

    @Override
    public PageUtils queryListWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",memberRespVo.getId()).orderByDesc("id")
        );

        List<OrderEntity> collect = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> orderSn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(orderSn);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(collect);

        return new PageUtils(page);
    }

    /**
     * 处理支付宝的支付结果
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());

        paymentInfoService.save(infoEntity);

        //修改订单状态信息
        if (vo.getTrade_status().equals("TREAD_SUCCESS") || vo.getTrade_status().equals("TREAD_")){
            String outTradeNo = vo.getOut_trade_no();
            baseMapper.updateOrderStatus(outTradeNo,OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrder.getOrderSn());
        orderEntity.setMemberId(seckillOrder.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal multiply = seckillOrder.getSeckillPrice().multiply(new BigDecimal("" + seckillOrder.getNum()));
        orderEntity.setPayAmount(multiply);

        save(orderEntity);

        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrder.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        orderItemEntity.setSkuQuantity(seckillOrder.getNum());

        orderItemService.save(orderItemEntity);
    }

    /**
     * 保存订单数据
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        save(orderEntity);

        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * 创建订单
     */
    private OrderCreateTo createOrder(){
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        String orderSn = IdWorker.getTimeId();

        //订单号
        OrderEntity orderEntity = buildOrder(orderSn);

        //订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);

        //验价
        computePrice(orderEntity,orderItemEntities);
        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(orderItemEntities);

        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        for (OrderItemEntity entity : orderItemEntities) {
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(entity.getRealAmount());
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }
        //订单价格相关
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));

        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setCouponAmount(coupon);

        //积分等
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());

        //删除状态
        orderEntity.setDeleteStatus(0);

    }

    private OrderEntity buildOrder(String orderSn) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        //生成保存订单号
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(memberRespVo.getId());

        //获取收货地址
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        R r = wmsFeignService.getFare(submitVo.getAddrId());
        FareVo fareVo = (FareVo) r.get("data");

        //设置运费，收获人等信息
        entity.setFreightAmount(fareVo.getFare());
        entity.setReceiverCity(fareVo.getAddress().getCity());
        entity.setReceiverDetailAddress(fareVo.getAddress().getDetailAddress());
        entity.setReceiverName(fareVo.getAddress().getName());
        entity.setReceiverPhone(fareVo.getAddress().getPhone());
        entity.setReceiverPostCode(fareVo.getAddress().getPostCode());
        entity.setReceiverProvince(fareVo.getAddress().getProvince());
        entity.setReceiverRegion(fareVo.getAddress().getRegion());

        //设置订单状态
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);

        return entity;
    }

    /**
     * 构建所有订单项数据
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (!CollectionUtils.isEmpty(currentUserCartItems)){
            return currentUserCartItems.stream().map(item -> {
                OrderItemEntity orderItem = buildOrderItem(item);
                orderItem.setOrderSn(orderSn);
                return orderItem;
            }).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 构建某一个订单项
     */
    private OrderItemEntity buildOrderItem(OrderItemVo item) {
        OrderItemEntity entity = new OrderItemEntity();
        //spu信息
        R r = productFeignService.getSpuInfoBySkuId(item.getSkuId());
        SpuInfoVo data = (SpuInfoVo) r.get("data");
        entity.setSpuId(data.getId());
        entity.setSpuBrand(data.getBrandId().toString());
        entity.setSpuName(data.getSpuName());
        entity.setCategoryId(data.getCatalogId());

        //sku信息
        entity.setSkuId(item.getSkuId());
        entity.setSkuName(item.getTitle());
        entity.setSkuPrice(item.getPrice());
        entity.setSkuPic(item.getImage());
        String skuAttr = StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";");
        entity.setSkuAttrsVals(skuAttr);
        entity.setSkuQuantity(item.getCount());

        //积分信息
        entity.setGiftGrowth(item.getPrice().multiply(new BigDecimal(item.getCount().toString())).intValue());
        entity.setGiftIntegration(item.getPrice().multiply(new BigDecimal(item.getCount().toString())).intValue());

        //订单项的价格信息
        entity.setPromotionAmount(new BigDecimal("0"));
        entity.setCouponAmount(new BigDecimal("0"));
        entity.setIntegrationAmount(new BigDecimal("0"));
        //当前订单项实际金额
        entity.setRealAmount(entity.getSkuPrice().multiply(new BigDecimal(entity.getSkuQuantity().toString()))
                .subtract(entity.getCouponAmount())
                .subtract(entity.getPromotionAmount())
                .subtract(entity.getIntegrationAmount()));

        return entity;
    }
}