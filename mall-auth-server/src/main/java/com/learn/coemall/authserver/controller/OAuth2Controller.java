package com.learn.coemall.authserver.controller;

import com.alibaba.fastjson.JSON;
import com.learn.coemall.authserver.fegin.MemberFeignService;
import com.learn.coemall.authserver.vo.SocialUser;
import com.learn.common.constant.AuthServerConstant;
import com.learn.common.utils.HttpUtils;
import com.learn.common.utils.R;
import com.learn.common.vo.MemberRespVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @author coffee
 * @since 2021-06-20 10:51
 */
@Controller
public class OAuth2Controller {


    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code, HttpSession session) throws Exception {

        Map<String,String> map = new HashMap<>();
        map.put("client_id","2636917288");
        map.put("client_secret","6a263e9284c6c1a74a62eadacc11b6e2");
        map.put("grant_type","authorization_code");
        map.put("redirect_uri","http://auth.coemall.com/oauth2.0/weibo/success");
        map.put("code",code);
        //根据code换取accessToken
        HttpResponse response = HttpUtils.doPost("http://api.weibo.com", "/oauth2.0/access_token", "post", null, null, map);

        //登录成功跳回首页
        if (response.getStatusLine().getStatusCode() == 200){
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);

            //调用远程社交登录接口
            R r = memberFeignService.oauthLogin(socialUser);
            if (r.getCode() == 0){
                MemberRespVo data = (MemberRespVo) r.get("data");
                session.setAttribute(AuthServerConstant.LOGIN_USER,data);

                return "redirect:http://coemall.com";
            }else {
                return "redirect:http://auth.coemall.com/login.html";
            }
        }else {
            return "redirect:http://auth.coemall.com/login.html";
        }
    }
}
