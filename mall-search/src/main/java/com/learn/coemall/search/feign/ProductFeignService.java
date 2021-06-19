package com.learn.coemall.search.feign;

import com.learn.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author coffee
 * @since 2021-06-18 16:39
 */
@FeignClient("mall-product")
public interface ProductFeignService {

    @GetMapping ("/product/attr/info/{attrId}")
    R attrInfo(@PathVariable("attrId") Long attrId);

    @GetMapping("/product/brand//infos")
    R brandInfo(@RequestParam("brandIds") List<Long> brandIds);
}
