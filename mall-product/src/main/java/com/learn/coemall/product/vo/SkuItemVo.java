package com.learn.coemall.product.vo;


import com.learn.coemall.product.entity.SkuImagesEntity;
import com.learn.coemall.product.entity.SkuInfoEntity;
import com.learn.coemall.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    //1、sku基本信息获取  pms_sku_info
    SkuInfoEntity info;

    boolean hasStock = true;

    //2、sku的图片信息  pms_sku_images
    List<SkuImagesEntity> images;

    //3、获取spu的销售属性组合。
    List<SkuItemSaleAttrVo> saleAttr;

    //4、获取spu的介绍
    SpuInfoDescEntity desp;

    //5、获取spu的规格参数信息。
    List<SpuItemAttrGroupVo> groupAttrs;

    SecKillInfoVo secKillInfo;//当前商品的秒杀优惠信息

}
