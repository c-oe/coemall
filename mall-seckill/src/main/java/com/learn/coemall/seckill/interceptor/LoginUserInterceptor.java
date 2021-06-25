package com.learn.coemall.seckill.interceptor;

import com.learn.common.constant.AuthServerConstant;
import com.learn.common.vo.MemberRespVo;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author coffee
 * @date 2021-06-22 10:54
 */
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/kill/**", uri);
        if (match) {
            MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);
            if (attribute != null) {
                loginUser.set(attribute);
                return true;
            } else {
                request.getSession().setAttribute("msg", "请先进行登录");
                response.sendRedirect("http://auth.coemall.com/login.html");
                return false;
            }
        }
        return true;
    }
}
