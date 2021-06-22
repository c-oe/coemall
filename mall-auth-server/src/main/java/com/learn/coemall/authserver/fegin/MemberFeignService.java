package com.learn.coemall.authserver.fegin;

import com.learn.coemall.authserver.vo.SocialUser;
import com.learn.coemall.authserver.vo.UserLoginVo;
import com.learn.coemall.authserver.vo.UserRegistVo;
import com.learn.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author coffee
 * @since 2021-06-19 17:03
 */
@FeignClient("mall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
     R oauthLogin(@RequestBody SocialUser user) throws Exception;
}
