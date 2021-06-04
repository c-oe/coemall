package com.learn.common.to;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author coffee
 * @since 2021-06-06 17:40
 */
@Data
public class SpuBoundTo {

    private Long spuId;
    private BigDecimal buyBounds;
    private BigDecimal growBounds;

}
