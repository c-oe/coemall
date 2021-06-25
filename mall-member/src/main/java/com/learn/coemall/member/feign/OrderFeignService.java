package com.learn.coemall.member.feign;

import com.learn.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @author coffee
 * @date 2021-06-24 10:58
 */
@FeignClient("mall-order")
public interface OrderFeignService {

    @PostMapping ("/order/order/listWithItem")
    R listWithItem(@RequestBody Map<String, Object> params);
}
