package com.learn.coemall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.learn.coemall.search.config.CoemallElasticSearchConfig;
import com.learn.coemall.search.constant.EsConstant;
import com.learn.coemall.search.feign.ProductFeignService;
import com.learn.coemall.search.service.MallSearchService;
import com.learn.coemall.search.vo.AttrResponseVo;
import com.learn.coemall.search.vo.BrandVo;
import com.learn.coemall.search.vo.SearchParam;
import com.learn.coemall.search.vo.SearchResult;
import com.learn.common.to.es.SkuEsModel;
import com.learn.common.utils.R;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author coffee
 * @since 2021-06-17 17:23
 */
@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Qualifier("elasticsearchRestHighLevelClient")
    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductFeignService productFeignService;

    @Override
    public SearchResult search(SearchParam searchParam) {
        //动态构建出查询需要的DSL语句
        SearchResult result = null;

        //准备检索请求
        SearchRequest searchRequest = buildSearchRequest(searchParam);

        try {
            //执行检索请求
            SearchResponse searchResponse = client.search(searchRequest, CoemallElasticSearchConfig.COMMON_OPTIONS);
            //分析响应数据并封装成所需格式
            result = buildSearchResult(searchResponse,searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 分析响应数据并封装成所需格式
     */
    private SearchResult buildSearchResult(SearchResponse searchResponse,SearchParam searchParam) {

        SearchResult result = new SearchResult();
        SearchHits hits = searchResponse.getHits();

        //返回的所有查询到的商品
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0){
            for (SearchHit hit : hits.getHits()) {
                String source = hit.getSourceAsString();
                SkuEsModel esModel = JSON.parseObject(source, SkuEsModel.class);
                if (StringUtils.hasLength(searchParam.getKeyword())){
                    esModel.setSkuTitle(hit.getHighlightFields().get("skuTitle").getFragments()[0].string());
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);

        //当前商品的所有属性信息
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedNested attrAgg = searchResponse.getAggregations().get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id_agg");
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            attrVo.setAttrId(bucket.getKeyAsNumber().longValue());
            attrVo.setAttrName(((ParsedStringTerms)bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString());
            attrVo.setAttrValue(((ParsedStringTerms)bucket.getAggregations().get("")).getBuckets()
                    .stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList()));

            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);

        //当前商品的所有品牌信息
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        ParsedLongTerms brandAgg = searchResponse.getAggregations().get("brand_agg");
        for (Terms.Bucket bucket : brandAgg.getBuckets()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            brandVo.setBrandId(bucket.getKeyAsNumber().longValue());
            brandVo.setBrandImg(((ParsedStringTerms)bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString());
            brandVo.setBrandName(((ParsedStringTerms)bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString());

            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);

        //当前商品的所有分类信息
        ParsedLongTerms catalogAgg = searchResponse.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();

        for (Terms.Bucket bucket : catalogAgg.getBuckets()) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            catalogVo.setCatalogId(bucket.getKeyAsNumber().longValue());
            ParsedStringTerms catalogNameAgg = bucket.getAggregations().get("catalog_name_agg");
            catalogVo.setCatalogName(catalogNameAgg.getBuckets().get(0).getKeyAsString());

            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //页码
        result.setPageNum(searchParam.getPageNum());

        //总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);

        //总页码
        int totalPage = (int) total%EsConstant.PRODUCT_PAGESIZE == 0 ? (int)total%EsConstant.PRODUCT_PAGESIZE : ((int)total%EsConstant.PRODUCT_PAGESIZE + 1);
        result.setTotalPages(totalPage);

        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 0; i < totalPage; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);


        //属性面包屑导航
        if (!CollectionUtils.isEmpty(searchParam.getAttrs())){
            List<SearchResult.NavVo> collect = searchParam.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = attr.split("_");
                navVo.setNavValue(s[1]);
                R r = productFeignService.attrInfo(Long.parseLong(s[0]));
                result.getAttrIds().add(Long.parseLong(s[0]));
                if (r.getCode() == 0){
                    AttrResponseVo data = (AttrResponseVo) r.get("attr");
                    navVo.setNavName(data.getAttrName());
                }else {
                    navVo.setNavName(s[0]);
                }

                //取消面包屑后跳转
                String replace = getReplace(searchParam, attr,"attrs");
                navVo.setLink("http://search.coemall.com/list.html?" + replace);

                return navVo;
            }).collect(Collectors.toList());




            result.setNavs(collect);
        }

        //品牌面包屑导航
        if (!CollectionUtils.isEmpty(searchParam.getBrandId())){
            List<SearchResult.NavVo> navs = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();

            navVo.setNavName("品牌");
            R r = productFeignService.brandInfo(searchParam.getBrandId());
            if (r.getCode() == 0){
                List<BrandVo> brand = (List<BrandVo>) r.get("brand");
                StringBuilder builder = new StringBuilder();
                String replace = null;
                for (BrandVo brandVo : brand) {
                    builder.append(brandVo.getBrandName() + ";");
                    replace = getReplace(searchParam, brandVo.getBrandId() + "","brandId");
                }
                navVo.setNavValue(builder.toString());
                navVo.setLink("http://search.coemall.com/list.html?" + replace);
            }
            navs.add(navVo);
            result.setNavs(navs);
        }
        //分类面包屑导航

        return result;
    }

    private String getReplace(SearchParam searchParam, String value,String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(value, "UTF-8");
            encode = encode.replace("+","%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = searchParam.get_queryString().replace("&" + key + "=" + encode, "");
        return replace;
    }

    /**
     * 准备检索请求
     */
    private SearchRequest buildSearchRequest(SearchParam searchParam) {

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //查询：模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存）
            //bool - query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                //must-模糊匹配
        if (StringUtils.hasLength(searchParam.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("skuTitle",searchParam.getKeyword()));
        }
            //bool - filter
                //三级分类id
        if (searchParam.getCatalog3Id() != null){
            boolQuery.filter(QueryBuilders.termQuery("catalogId",searchParam.getCatalog3Id()));
        }
                //品牌id
        if (!CollectionUtils.isEmpty(searchParam.getBrandId())){
            boolQuery.filter(QueryBuilders.termsQuery("brandId",searchParam.getBrandId()));
        }
                //属性
        if (!CollectionUtils.isEmpty(searchParam.getAttrs())){
            for (String attr : searchParam.getAttrs()) {
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                String[] s = attr.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",attrId));
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",attrValues));
                boolQuery.filter(nestedBoolQuery);
            }
        }
                //库存
        boolQuery.filter(QueryBuilders.termQuery("hasStock",searchParam.getHasStock()));
                //价格区间
        if (StringUtils.hasLength(searchParam.getSkuPrice())){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = searchParam.getSkuPrice().split("-");
            if (s.length == 2){
                rangeQuery.gte(s[0]).lte(s[1]);
            }else{
                if (searchParam.getSkuPrice().startsWith("_")){
                    rangeQuery.lte(s[0]);
                }
                if (searchParam.getSkuPrice().endsWith("_")){
                    rangeQuery.gte(s[0]);
                }
            }
        }

        sourceBuilder.query(boolQuery);
        //排序，分页，高亮
            //排序"sort": [{"skuPrice": {"order": "desc"}}],
        if (StringUtils.hasLength(searchParam.getSort())){
            String[] s = searchParam.getSort().split("_");
            SortOrder sortOrder = SortOrder.fromString(s[1]);
            sourceBuilder.sort(s[0],sortOrder);
        }
            //分页
        sourceBuilder.from((searchParam.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

            //高亮 "highlight": {"fields": {"skuTitle": {}},"pre_tags": "<b style='color:red'>","post_tags": "</b>"},
        if (StringUtils.hasLength(searchParam.getKeyword())){
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            sourceBuilder.highlighter(builder);
        }
        //聚合
            //品牌聚合
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
                //子聚合
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));

        sourceBuilder.aggregation(brandAgg);
            //分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(20);
                //子聚合
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));

        sourceBuilder.aggregation(catalogAgg);
            //属性聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
                //子聚合
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
                    //子聚合
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attrIdAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));

        attrAgg.subAggregation(attrIdAgg);

        sourceBuilder.aggregation(attrAgg);
        return new SearchRequest(new String[]{(EsConstant.PRODUCT_INDEX)},sourceBuilder);
    }
}
