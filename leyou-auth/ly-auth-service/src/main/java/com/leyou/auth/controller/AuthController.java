package com.leyou.auth.controller;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.CookieUtils;
import com.sun.awt.AWTUtilities;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Value("${ly.jwt.cookieName}")
    private String cookieName;
    @Autowired
    AuthService authService;
    @Autowired
    JwtProperties properties;

    /**
     * 登录授权功能
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestParam("username") String username,
                                      @RequestParam("password") String password,
                                      HttpServletResponse response,
                                      HttpServletRequest request
                                    ){
        //登录
        String token = authService.login(username, password);
        //写入cookie
        CookieUtils.setCookie(request,response,cookieName,token);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     *验证用户登录状态
     * @return
     */
    @GetMapping("/verify")
    public ResponseEntity<UserInfo> verify(@CookieValue("LY_TOKEN") String token,
        HttpServletRequest request,HttpServletResponse response
    ){
        if (StringUtils.isBlank(token)) {
            //如果没有token，说明未登录
            throw new LyException(ExceptionEnums.UNAUTHORIZED);
        }
        try {
            //解析token
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, properties.getPublicKey());
            //刷新token,重新生成
            String newToken = JwtUtils.generateToken(userInfo, properties.getPrivateKey(), properties.getExpire());
            //写回cookie中
            CookieUtils.setCookie(request,response,cookieName,newToken);
            //说明认证通过，返回user信息
            return ResponseEntity.ok(userInfo);
        }catch (Exception e){
            //1.token已过期(30分钟)
            //2.token无效，被篡改过
            throw new LyException(ExceptionEnums.UNAUTHORIZED);
        }
    }
}
