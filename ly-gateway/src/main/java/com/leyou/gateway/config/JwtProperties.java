package com.leyou.gateway.config;

import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Data
@ConfigurationProperties("ly.jwt")
public class JwtProperties {

    private String pubKeyPath;
    private String cookieName;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    //对象一旦实例化后，就应该读取公钥与私钥
    @PostConstruct
    public void init() throws Exception {
        //读取公钥与私钥
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
    }

}
