package com.learn.coemall.order.web;

import com.learn.coemall.order.service.OrderService;
import com.learn.coemall.order.vo.OrderConfirmVo;
import com.learn.coemall.order.vo.OrderSubmitVo;
import com.learn.coemall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.concurrent.ExecutionException;

/**
 * @author coffee
 * @date 2021-06-22 10:50
 */
@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData",confirmVo);
        return "confirm";
    }

    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo,Model model){
        SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
        if (responseVo.getCode() == 0){
            model.addAttribute("submitOrderResp",responseVo);
            return "pay";
        }else {
            return "redirect:http://order.coemall.com/toTrade";
        }
    }
}
