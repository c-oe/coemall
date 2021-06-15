package com.learn.coemall.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sun.tools.jstat.Token;

/**
 * @author coffee
 * @since 2021-06-08 12:31
 */
@Configuration
public class CoemallElasticSearchConfig {

    public static final RequestOptions COMMON_OPTIONS;
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
//        builder.addHeader("","", + TOKEN);
//        builder.setHttpAsyncResponseConsumerFactory(
//                new HttpAsyncResponseConsumerFactory.
//                        HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 *1024));
        COMMON_OPTIONS = builder.build();
    }


    @Bean
    public RestHighLevelClient esRestClient(){
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("192.168.80.133",9200,"http")));
    }

}
