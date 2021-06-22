package com.learn.coemall.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author coffee
 * @date 2021-06-22 14:42
 */
@Configuration
public class CoeFrignConfig {

    /**
     * 远程调用会丢失请求头，此方法将同步原请求的cookie
     */
    @Bean
    public RequestInterceptor requestInterceptor(){
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(attributes != null){
                    HttpServletRequest request = attributes.getRequest();
                    if (request != null){
                        template.header("Cookie",request.getHeader("Cookie"));
                    }
                }
            }
        };
    }
}
