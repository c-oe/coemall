package com.learn.coemall.thirdparty.controller;

import com.learn.coemall.thirdparty.component.SMSComponent;
import com.learn.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author coffee
 * @since 2021-06-19 15:02
 */
@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @Autowired
    private SMSComponent smsComponent;

    @GetMapping("/sendcode")
    public R sendCode(@RequestParam("phone")String phone,@RequestParam("code")String code){
        smsComponent.sendSMSCode(phone, code);
        return R.ok();
    }
}
