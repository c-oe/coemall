package com.learn.coemall.product.feign;

import com.learn.common.to.SkuHasStockTo;
import com.learn.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @author coffee
 * @since 2021-06-08 19:22
 */
@FeignClient("mall-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasstock")
    List<SkuHasStockTo> getSkuHasStock(@RequestBody List<Long> skuIds);
}
