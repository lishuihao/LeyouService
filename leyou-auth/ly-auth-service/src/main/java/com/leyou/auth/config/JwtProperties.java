package com.leyou.auth.config;

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

    private String secret;
    private String pubKeyPath;
    private String priKeyPath;
    private int expire;
    private String cookieName;


    private PublicKey publicKey;
    private PrivateKey privateKey;

    //对象一旦实例化后，就应该读取公钥与私钥
    @PostConstruct
    public void init() throws Exception {
        //公钥私钥不存在,先生成
        File pubfile = new File(pubKeyPath);
        File prifile = new File(priKeyPath);
        if(!pubfile.exists() || !prifile.exists()){
            //生成公钥，私钥
            RsaUtils.generateKey(pubKeyPath,priKeyPath,secret);
        }

        //读取公钥与私钥
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey=RsaUtils.getPrivateKey(priKeyPath);
    }

}
