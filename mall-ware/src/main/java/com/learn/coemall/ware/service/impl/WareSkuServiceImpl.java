package com.learn.coemall.ware.service.impl;

import com.learn.coemall.ware.feign.ProductFeignService;
import com.learn.coemall.ware.vo.OrderItemVo;
import com.learn.coemall.ware.vo.SkuWareHasStock;
import com.learn.coemall.ware.vo.WareSkuLockVo;
import com.learn.common.exception.NoStockException;
import com.learn.common.to.SkuHasStockTo;
import com.learn.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private ProductFeignService productFeignService;

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

        //找到每个商品在哪个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            stock.setSkuId(item.getSkuId());
            List<Long> wareIds = baseMapper.listWareIdHasSkuStock(item.getSkuId());
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

}