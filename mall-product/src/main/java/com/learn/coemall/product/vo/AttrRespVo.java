package com.learn.coemall.product.vo;

import lombok.Data;

/**
 * @author coffee
 * @since 2021-06-04 18:05
 */
@Data
public class AttrRespVo extends AttrVo {

    private String catelogName;

    private String groupName;

    private Long[] catelogPath;
}
