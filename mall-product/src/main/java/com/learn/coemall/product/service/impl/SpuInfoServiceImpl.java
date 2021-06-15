package com.learn.coemall.product.service.impl;

import com.learn.coemall.product.entity.*;
import com.learn.coemall.product.feign.CouponFeignService;
import com.learn.coemall.product.feign.SearchFeignService;
import com.learn.coemall.product.feign.WareFeignService;
import com.learn.coemall.product.service.*;
import com.learn.coemall.product.vo.*;
import com.learn.common.constant.ProductConstant;
import com.learn.common.to.SkuHasStockTo;
import com.learn.common.to.SkuReductionTo;
import com.learn.common.to.SpuBoundTo;
import com.learn.common.to.es.SkuEsModel;
import com.learn.common.utils.R;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.learn.common.utils.PageUtils;
import com.learn.common.utils.Query;

import com.learn.coemall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private SkuInfoService skuInfoService;

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private SearchFeignService searchFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {

        //保存spu基本信息 pms_sku_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());

        baseMapper.insert(spuInfoEntity);

        //保存spu描述图片 pms_spu_info_desc
        List<String> desc = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuInfoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",",desc));

        spuInfoDescService.save(spuInfoDescEntity);

        //保存spu图片集  pms_spu_images
        List<String> images = vo.getImages();

        spuImagesService.saveImages(spuInfoEntity.getId(),images);

        //保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
            productAttrValueEntity.setAttrId(attr.getAttrId());
            productAttrValueEntity.setAttrName(attrService.getById(attr.getAttrId()).getAttrName());
            productAttrValueEntity.setAttrValue(attr.getAttrValues());
            productAttrValueEntity.setQuickShow(attr.getShowDesc());
            productAttrValueEntity.setSpuId(spuInfoEntity.getId());

            return productAttrValueEntity;
        }).collect(Collectors.toList());

        productAttrValueService.saveBatch(collect);

        //保存spu积分信息  mall_sms/sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(spuInfoEntity.getId());

        R r = couponFeignService.saveSpuBounds(spuBoundTo);

        if (r.getCode() == 0){
            log.error("远程保存spu积分信息失败");
        }


        //保存spu对应的sku信息
        List<Skus> skus = vo.getSkus();

        if (!CollectionUtils.isEmpty(skus)){
            skus.forEach(item -> {
                //保存sku基本信息  pms_sku_info
                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item,skuInfoEntity);
                skuInfoEntity.setBrandId(skuInfoEntity.getBrandId());
                skuInfoEntity.setCatalogId(spuInfoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(spuInfoEntity.getId());
                String defaultImg = "";
                for (Images image : item.getImages()) {
                    if (image.getDefaultImg() == 1){
                        defaultImg = image.getImgUrl();
                    }
                }
                skuInfoEntity.setSkuDefaultImg(defaultImg);

                skuInfoService.save(skuInfoEntity);

                //保存sku图片信息  pms_sku_images
                Long skuId = skuInfoEntity.getSkuId();
                List<SkuImagesEntity> skuImagesEntityList = item.getImages().stream().map(img -> {
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());

                    return skuImagesEntity;
                }).filter(entity -> {
                    //没有图片路径的无需保存
                    //返回false被过滤
                    return StringUtils.hasLength(entity.getImgUrl());
                }).collect(Collectors.toList());

                skuImagesService.saveBatch(skuImagesEntityList);

                //保存sku销售属性信息  pms_sku_sale_attr_value
                List<SkuSaleAttrValueEntity> saleAttrValueEntityList = item.getAttr().stream().map(attr -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr, skuSaleAttrValueEntity);
                    skuSaleAttrValueEntity.setSkuId(skuId);

                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());

                skuSaleAttrValueService.saveBatch(saleAttrValueEntityList);

                //保存sku优惠满减等信息  mall_sms/sms_sku_ladder、sms_sku_full_reduction、sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item,skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1){
                    R r1 = couponFeignService.saveSkuReduction(skuReductionTo);

                    if (r1.getCode() == 0){
                        log.error("远程保存spu优惠信息失败");
                    }
                }
            });
        }
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> queryWrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if (StringUtils.hasLength(key)){
            queryWrapper.and(wrapper ->{
                wrapper.eq("id",key).or().like("spu_name",key);
            });
        }

        String status = (String) params.get("status");
        if (StringUtils.hasLength(status)){
            queryWrapper.eq("publish_status",status);
        }

        String brandId = (String) params.get("brandId");
        if (StringUtils.hasLength(brandId) && "0".equalsIgnoreCase(brandId)){
            queryWrapper.eq("brand_id",brandId);
        }

        String catalogId = (String) params.get("catalogId");
        if (StringUtils.hasLength(catalogId) && "0".equalsIgnoreCase(catalogId)){
            queryWrapper.eq("catalogId",catalogId);
        }

        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params), queryWrapper);

        return new PageUtils(page);
    }

    @Override
    public void up(Long spuId) {

        //查出当前spuid对应的所有sku信息，品牌的名字
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);
        List<Long> idList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        //查询当前sku的所有可以被用来检索的规格属性
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrListForSpu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(ProductAttrValueEntity::getAttrId).collect(Collectors.toList());
        List<Long> searchAttrIds = attrService.selectSearchAttrs(attrIds);

        Set<Long> idSet = new HashSet<>(searchAttrIds);
        List<SkuEsModel.Attrs> collect = baseAttrs.stream().filter(attr -> {
            return idSet.contains(attr.getAttrId());
        }).map(attr -> {
            SkuEsModel.Attrs attrs = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(attr, attrs);

            return attrs;
        }).collect(Collectors.toList());
        //向库存系统发送远程调用，查询是否有库存
        Map<Long, Boolean> stockMap = null;
        try {
            List<SkuHasStockTo> hasStock = wareFeignService.getSkuHasStock(idList);
            stockMap = hasStock.stream().collect(Collectors.toMap(SkuHasStockTo::getSkuId, SkuHasStockTo::getHasStock));
        }catch (Exception e){
            log.error("库存服务查询异常，原因{}",e);
        }

        //封装信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            esModel.setSkuId(sku.getSkuId());
            esModel.setSpuId(sku.getSpuId());
            esModel.setSkuTitle(sku.getSkuTitle());
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());
            esModel.setSaleCount(sku.getSaleCount());
            //库存信息设置
            if (finalStockMap == null){
                esModel.setHasStock(true);
            }else {
                esModel.setHasStock(finalStockMap.get(sku.getSkuId()));
            }
            //热度评分
            esModel.setHotScore(0L);
            //品牌和分类的信息
            esModel.setBrandId(sku.getBrandId());
            BrandEntity brandEntity = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brandEntity.getName());
            esModel.setBrandImg(brandEntity.getLogo());
            esModel.setCatalogId(sku.getCatalogId());
            CategoryEntity categoryEntity = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(categoryEntity.getName());
            //设置检索属性
            esModel.setAttrs(collect);

            return esModel;
        }).collect(Collectors.toList());

        //组装数据，将数据发送给es进行保存，调用mall-search服务
        R r = searchFeignService.productStatusUp(upProducts);
        if (r.getCode() == 0){
            //成功,修改spu状态
            baseMapper.updateSpuStatus(spuId, ProductConstant.StatusEnum.SPU_UP.getCode());
        }else {
            //失败 TODO 重复调用  接口幂等性  重试机制
        }

    }

}