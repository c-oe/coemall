package com.learn.coemall.authserver.fegin;

import com.learn.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author coffee
 * @since 2021-06-19 15:07
 */
@FeignClient("mall-third-party")
public interface ThirdPartFeignService {

    @GetMapping("/sms/sendcode")
    R sendCode(@RequestParam("phone")String phone, @RequestParam("code")String code);
}
