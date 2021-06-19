package com.learn.coemall.authserver.controller;

import com.learn.coemall.authserver.fegin.MemberFeignService;
import com.learn.coemall.authserver.fegin.ThirdPartFeignService;
import com.learn.coemall.authserver.vo.UserLoginVo;
import com.learn.coemall.authserver.vo.UserRegistVo;
import com.learn.common.constant.AuthServerConstant;
import com.learn.common.exception.BizCodeEnum;
import com.learn.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author coffee
 * @since 2021-06-19 15:05
 */
@Controller
public class LoginController {

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R sendCode(@RequestParam("phone")String phone){

        //接口防刷
        String redisCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (StringUtils.hasLength(redisCode)){
            long l = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000){
                return R.error(BizCodeEnum.VALID_SMS_CODE_EXCEPTION.getCode(),BizCodeEnum.VALID_SMS_CODE_EXCEPTION.getMsg());
            }
        }

        String code = UUID.randomUUID().toString().substring(0, 5);
        String s = code + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone,s,5, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone,code);
        return R.ok();
    }


    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes){

        if (result.hasErrors()){
            Map<String, String> errors = result.getFieldErrors().stream()
                    .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));

            redirectAttributes.addFlashAttribute("errors",errors);
            //校验出错,转到注册页
            return "redirect:/http://auth.coemall.com/reg.html";
        }
        //注册
            //校验验证码
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (StringUtils.hasLength(s)){
            if (code.equals(s.split("_")[0])){
                //删除验证码
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                //调用远程服务注册进行注册
                R r = memberFeignService.regist(vo);
                if (r.getCode() == 0){
                    return "redirect:/http://auth.coemall.com/login.html";
                }else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", String.valueOf(r.get("msg")));
                    redirectAttributes.addFlashAttribute("errors",errors);
                    return "redirect:/http://auth.coemall.com/reg.html";
                }
            }else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                redirectAttributes.addFlashAttribute("errors",errors);
                //校验出错,转到注册页
                return "redirect:/http://auth.coemall.com/reg.html";
            }
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码错误");
            redirectAttributes.addFlashAttribute("errors",errors);
            //校验出错,转到注册页
            return "redirect:/http://auth.coemall.com/reg.html";
        }
    }

    @PostMapping("/login")
    public String login(UserLoginVo vo,RedirectAttributes redirectAttributes){
        //远程登录
        R login = memberFeignService.login(vo);
        if (login.getCode() == 0){
            return "redirect:/http://coemall.com";
        }else {
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", String.valueOf(login.get("msg")));
            redirectAttributes.addFlashAttribute("errors",errors);
            return "redirect:/http://auth.coemall.com/login.html";
        }
    }

}
