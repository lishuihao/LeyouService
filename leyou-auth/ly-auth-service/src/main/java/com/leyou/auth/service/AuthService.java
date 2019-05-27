package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.auth.utils.RsaUtils;
import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import com.leyou.user.pojo.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(JwtProperties.class)
@Slf4j
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    JwtProperties properties;

    public String login(String username, String password) {
        try {
            //校验用户名和密码
            User user = userClient.queryUserByUsernameAndPassword(username, password);
            if (user == null) {
                throw new LyException(ExceptionEnums.INVALID_USERANDPASSWORD);
            }
            //生成tooken
            String token = JwtUtils.generateToken(new UserInfo(user.getId(), username), properties.getPrivateKey(), properties.getExpire());
            return token;
        }catch (Exception e){
            log.error("[授权中心] 用户名或密码有误,用户名称:{}",username);
            throw new LyException(ExceptionEnums.CREATE_TOKEN_ERROR);
        }
    }
}
