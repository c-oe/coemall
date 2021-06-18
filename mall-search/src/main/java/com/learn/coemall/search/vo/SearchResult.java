package com.learn.coemall.search.vo;

import com.learn.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

/**
 * 返回给页面的所有信息
 * @author coffee
 * @since 2021-06-17 17:38
 */
@Data
public class SearchResult {

    private List<SkuEsModel> products;//商品信息

    private Integer pageNum;//当前页码
    private Long total;//总记录数
    private Integer totalPages;//总页码

    private List<BrandVo> brands;//涉及品牌

    private List<AttrVo> attrs;//涉及属性

    private List<catalogVo> catalogs;//涉及分类



    @Data
    public static class BrandVo{
        private Long brandId;
        private String brandName;

        private String brandImg;
    }

    @Data
    public static class AttrVo{
        private Long attrId;
        private String attrName;

        private List<String> attrValue;
    }

    @Data
    public static class catalogVo{
        private Long catalogId;
        private String catalogName;
    }
}
