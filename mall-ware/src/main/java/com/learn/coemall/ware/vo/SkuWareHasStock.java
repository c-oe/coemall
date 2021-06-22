package com.learn.coemall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @author coffee
 * @date 2021-06-22 18:34
 */
@Data
public class SkuWareHasStock {

    private Long skuId;
    private Integer num;
    private List<Long> wareId;
}
