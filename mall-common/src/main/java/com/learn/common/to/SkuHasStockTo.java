package com.learn.common.to;

import lombok.Data;

/**
 * @author coffee
 * @since 2021-06-08 19:14
 */
@Data
public class SkuHasStockTo {

    private Long skuId;
    private Boolean hasStock;
}
