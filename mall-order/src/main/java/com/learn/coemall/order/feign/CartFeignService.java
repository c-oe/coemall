package com.learn.coemall.order.feign;

import com.learn.coemall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("coemall-cart")
public interface CartFeignService {

    @GetMapping("/currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();
}
