package com.learn.coemall.product.service.impl;

import com.learn.coemall.product.feign.SeckillFeignService;
import com.learn.coemall.product.service.*;
import com.learn.coemall.product.vo.SecKillInfoVo;
import com.learn.coemall.product.vo.SkuItemVo;
import com.learn.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;

import com.learn.coemall.product.dao.SkuInfoDao;
import com.learn.coemall.product.entity.SkuInfoEntity;
import org.springframework.util.StringUtils;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    private SkuImagesService imagesService;

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private SeckillFeignService seckillFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (StringUtils.hasLength(key)){
            queryWrapper.and(wrapper -> {
                wrapper.eq("sku_id",key).or().like("spu_name",key);
            });
        }

        String catalogId = (String) params.get("catalogId");
        if (StringUtils.hasLength(catalogId) && "0".equalsIgnoreCase(catalogId)){
            queryWrapper.eq("catalog_id",catalogId);
        }

        String brandId = (String) params.get("brandId");
        if (StringUtils.hasLength(brandId) && "0".equalsIgnoreCase(brandId)){
            queryWrapper.eq("brand_id",brandId);
        }

        String min = (String) params.get("min");
        if (StringUtils.hasLength(min)){
            queryWrapper.ge("price",min);
        }

        String max = (String) params.get("max");
        if (StringUtils.hasLength(max) && new BigDecimal(max).compareTo(BigDecimal.ZERO) > 0){
            queryWrapper.le("price",max);
        }


        IPage<SkuInfoEntity> page = this.page(new Query<SkuInfoEntity>().getPage(params),queryWrapper);

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {

        return list(new QueryWrapper<SkuInfoEntity>().eq("spu_id",spuId));

    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo vo = new SkuItemVo();

        //sku基本信息获取
        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfoEntity info = getById(skuId);
            vo.setInfo(info);
            return info;
        }, executor);

        //获取spu销售属性组合
        CompletableFuture<Void> saleAttrFuture = infoFuture.thenAcceptAsync(res -> {
            vo.setSaleAttr(skuSaleAttrValueService.getSaleAttrsBySpuId(res.getSpuId()));
        }, executor);

        //获取spu介绍
        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync(res -> {
            vo.setDesp(spuInfoDescService.getById(res.getSpuId()));
        }, executor);

        //获取spu规格参数信息
        CompletableFuture<Void> baseAttrFuture = infoFuture.thenAcceptAsync(res -> {
            vo.setGroupAttrs(attrGroupService.getAttrGroupWithAttrsBySpuId(res.getSpuId(), res.getCatalogId()));
        }, executor);

        //sku图片信息
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            vo.setImages(imagesService.getImagesBySkuId(skuId));
        }, executor);

        //是否参与秒杀优惠
        CompletableFuture<Void> secKillFuture = CompletableFuture.runAsync(() -> {
            R r = seckillFeignService.getSkuSeckillInfo(skuId);
            if (r.getCode() == 0) {
                SecKillInfoVo data = (SecKillInfoVo) r.get("data");
                vo.setSecKillInfo(data);
            }
        }, executor);

        //等待所有任务都完成
        CompletableFuture.allOf(saleAttrFuture,descFuture,baseAttrFuture,imageFuture,secKillFuture).get();

        return vo;
    }

}