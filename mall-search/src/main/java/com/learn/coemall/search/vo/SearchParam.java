package com.learn.coemall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 锋装页面所有可能传递来的查询条件
 * @author coffee
 * @since 2021-06-17 17:22
 */
@Data
public class SearchParam {

    private String keyword;//全文匹配关键字
    private Long catalog3Id;//三级分类id

    private String sort;//排序

    private Integer hasStock;//有货
    private String skuPrice;//价格区间
    private List<Long> brandId;//品牌ID
    private List<String> attrs;//属性

    private Integer pageNum = 1;//页码

    private String _queryString;//原生查询条件
}
