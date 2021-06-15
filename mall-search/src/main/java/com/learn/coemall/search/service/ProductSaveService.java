package com.learn.coemall.search.service;

import com.learn.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 * @author coffee
 * @since 2021-06-13 11:05
 */
public interface ProductSaveService {
    boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException;
}
