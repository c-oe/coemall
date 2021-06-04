package com.learn.coemall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * @author coffee
 * @since 2021-06-07 11:34
 */
@Data
public class MergeVo {

    private Long purchaseId;
    private List<Long> items;
}
