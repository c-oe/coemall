package com.learn.coemall.seckill.controller;

import com.learn.coemall.seckill.interceptor.LoginUserInterceptor;
import com.learn.coemall.seckill.service.SeckillService;
import com.learn.coemall.seckill.to.SecKillSkuRedisTo;
import com.learn.common.utils.R;
import com.learn.common.vo.MemberRespVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author coffee
 * @date 2021-06-24 16:42
 */
@Controller
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @ResponseBody
    @GetMapping("/currentSeckillSkus")
    public R getCurrentSeckillSkus(){
        List<SecKillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().put("data",vos);
    }

    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId){
        SecKillSkuRedisTo to = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().put("data",to);
    }

    @GetMapping("/kill")
    public String seckill(@RequestParam("killId") String killId, @RequestParam("key") String key, @RequestParam("num") Integer num, Model model){
        String orderSn = seckillService.kill(killId, key, num);
        model.addAttribute("orderSn",orderSn);
        return "success";
    }
}
