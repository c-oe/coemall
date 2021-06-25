package com.learn.coemall.order.web;

import com.alipay.api.AlipayApiException;
import com.learn.coemall.order.config.AlipayTemplate;
import com.learn.coemall.order.service.OrderService;
import com.learn.coemall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author coffee
 * @date 2021-06-24 10:14
 */
@Controller
public class PayWebController {

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    @ResponseBody
    @GetMapping(value = "/payOrder",produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {
        PayVo payVo = orderService.getOrderPay(orderSn);

        return alipayTemplate.pay(payVo);
    }
}
