package com.learn.coemall.ware.service.impl;

import com.learn.coemall.ware.entity.WareOrderTaskDetailEntity;
import com.learn.coemall.ware.entity.WareOrderTaskEntity;
import com.learn.coemall.ware.feign.OrderFeignService;
import com.learn.coemall.ware.feign.ProductFeignService;
import com.learn.coemall.ware.service.WareOrderTaskDetailService;
import com.learn.coemall.ware.service.WareOrderTaskService;
import com.learn.coemall.ware.vo.OrderItemVo;
import com.learn.coemall.ware.vo.OrderVo;
import com.learn.coemall.ware.vo.SkuWareHasStock;
import com.learn.coemall.ware.vo.WareSkuLockVo;
import com.learn.common.exception.NoStockException;
import com.learn.common.to.SkuHasStockTo;
import com.learn.common.to.mq.OrderTo;
import com.learn.common.to.mq.StockDetailTo;
import com.learn.common.to.mq.StockLockedTo;
import com.learn.common.utils.R;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;

import com.learn.coemall.ware.dao.WareSkuDao;
import com.learn.coemall.ware.entity.WareSkuEntity;
import com.learn.coemall.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@RabbitListener(queues = "stock.release.stock.queue")
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderFeignService orderFeignService;



    @Override
    public void handleStockLockedRelease(StockLockedTo to){
        StockDetailTo detail = to.getDetail();
        Long detailId = detail.getId();

        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        if (byId != null){
            Long id = to.getId();
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            //远程查询订单状态
            R r = orderFeignService.getOrderStatus(orderSn);
            if(r.getCode() == 0){
                OrderVo data = (OrderVo) r.get("data");
                if (data == null || data.getStatus() == 4){
                    //订单不存在
                    //订单状态：已取消
                    //解锁
                    if (byId.getLockStatus() == 1){
                        unLockStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum(),detailId);
                    }
                }
            }else {
                //消息拒绝后重新放到队列里面，让别人继续消费解锁
                throw new RuntimeException("远程服务失败");
            }
        }else {
            //无需解锁
        }
    }

    private void unLockStock(Long skuId, Long wareId, Integer skuNum, Long detailId) {
        baseMapper.unLockStock(skuId,wareId,skuNum);
        WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity();
        detailEntity.setId(detailId);
        detailEntity.setLockStatus(2);

        wareOrderTaskDetailService.updateById(detailEntity);
    }


    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if (StringUtils.hasLength(skuId)){
            queryWrapper.eq("sku_id",skuId);
        }

        String wareId = (String) params.get("wareId");
        if (StringUtils.hasLength(wareId)){
            queryWrapper.eq("ware_id",wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                queryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {

        List<WareSkuEntity> skuEntities = baseMapper.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id", wareId));
        //判断是否有库存记录
        if (CollectionUtils.isEmpty(skuEntities)){
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            try {
                R info = productFeignService.info(skuId);
                Map<String,Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0){
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            }catch (Exception e){ }

            baseMapper.insert(wareSkuEntity);
        }else {
            baseMapper.addStock(skuId,wareId,skuNum);
        }


    }

    @Override
    public List<SkuHasStockTo> getSkuHasStock(List<Long> skuIds) {

        return skuIds.stream().map(skuId -> {
            SkuHasStockTo stockTo = new SkuHasStockTo();
            Long count = baseMapper.getSkuStock(skuId);
            stockTo.setSkuId(skuId);
            stockTo.setHasStock(count > 0);

            return stockTo;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        //保存库存工作单详情
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(taskEntity);

        //找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            stock.setSkuId(item.getSkuId());
            List<Long> wareIds = baseMapper.listWareIdHasSkuStock(item.getSkuId());
            stock.setWareId(wareIds);
            return stock;
        }).collect(Collectors.toList());

        for (SkuWareHasStock stock : collect) {
            boolean skuStocked = false;
            Long skuId = stock.getSkuId();
            List<Long> wareIds = stock.getWareId();
            if (CollectionUtils.isEmpty(wareIds)){
                throw new NoStockException(skuId);
            }
            for (Long wareId : wareIds) {
                Long count = baseMapper.lockSkuStock(skuId,wareId,stock.getNum());
                if(count == 1){
                    skuStocked = true;
                    //告诉MQ库存锁定成功
                    WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity(null,
                            skuId,"",stock.getNum(),taskEntity.getId(),wareId,1);
                    wareOrderTaskDetailService.save(detailEntity);
                    StockLockedTo lockedTo = new StockLockedTo();
                    StockDetailTo stockDetailTo = new StockDetailTo();

                    BeanUtils.copyProperties(detailEntity,stockDetailTo);

                    lockedTo.setId(taskEntity.getId());
                    lockedTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange","stock.locked","");
                    break;
                }else {

                }
            }
            if (!skuStocked){
                throw new NoStockException(skuId);
            }
        }
        return true;
    }

    /**
     * 防止订单服务卡顿
     */
    @Transactional
    @Override
    public void handleOrderCloseRelease(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //查询最新库存的状态，防止重复解锁库存
        WareOrderTaskEntity task = wareOrderTaskService.getOne(new QueryWrapper<WareOrderTaskEntity>().eq("order_sn",orderSn));
        Long id = task.getId();
        //按照工作单找到所有没有解锁的库存进行解锁
        List<WareOrderTaskDetailEntity> entities = wareOrderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id).eq("lock_status", 1));
        for (WareOrderTaskDetailEntity entity : entities) {
            unLockStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum(),entity.getId());
        }
    }

}