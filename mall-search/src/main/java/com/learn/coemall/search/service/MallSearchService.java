package com.learn.coemall.search.service;

import com.learn.coemall.search.vo.SearchParam;
import com.learn.coemall.search.vo.SearchResult;

/**
 * @author coffee
 * @since 2021-06-17 17:23
 */
public interface MallSearchService {

    /**
     *
     * @param searchParam 检索的所有参数
     * @return 检索结果
     */
    SearchResult search(SearchParam searchParam);
}
