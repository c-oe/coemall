package com.learn.coemall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.learn.coemall.search.config.CoemallElasticSearchConfig;
import com.learn.coemall.search.constant.EsConstant;
import com.learn.coemall.search.service.ProductSaveService;
import com.learn.common.to.es.SkuEsModel;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author coffee
 * @since 2021-06-13 11:05
 */
@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    //保存到es
    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {

        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel model : skuEsModels) {
            //在es中建立索引，product，建立映射关系
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(model.getSpuId().toString());
            String s = JSON.toJSONString(model);
            indexRequest.source(s, XContentType.JSON);

            bulkRequest.add(indexRequest);
        }
        //给es中保存数据
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, CoemallElasticSearchConfig.COMMON_OPTIONS);


        List<String> collect = Arrays.stream(bulk.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList());
        log.error("商品上架：{},返回数据：{}",collect,bulk.toString());
        //如果有错误
        return !bulk.hasFailures();
    }
}
