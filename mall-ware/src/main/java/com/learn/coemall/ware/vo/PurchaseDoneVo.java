package com.learn.coemall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author coffee
 * @since 2021-06-07 14:07
 */
@Data
public class PurchaseDoneVo {

    @NotNull
    private Long id;
    private List<PurchaseItemVo> items;

}
