package com.learn.coemall.order.feign;

import com.learn.coemall.order.vo.MemberAddressVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient("coemall-member")
public interface MemberFeignService {


    @GetMapping("/member/memberreceiveaddress/{memeberId}/addresses")
    List<MemberAddressVo> getAddress(@PathVariable("memeberId") Long memberId);

}
