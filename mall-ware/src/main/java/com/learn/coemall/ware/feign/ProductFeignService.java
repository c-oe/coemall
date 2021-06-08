package com.learn.coemall.ware.feign;

import com.learn.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author coffee
 * @since 2021-06-07 15:17
 */
@FeignClient("mall-product")
//@FeignClient("mall-gateway")
public interface ProductFeignService {

    // /api/product/skuinfo/info/{skuId}
    @RequestMapping("/product/skuinfo/info/{skuId}")
    R info(@PathVariable("skuId") Long skuId);

}
