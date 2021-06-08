package com.learn.coemall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author coffee
 * @since 2021-06-08 12:31
 */
@Configuration
public class CoemallElasticSearchConfig {

    @Bean
    public RestHighLevelClient esRestClient(){
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("192.168.80.133",9200,"http")));
    }

}
