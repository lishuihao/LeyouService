package com.leyou;

import com.leyou.item.pojo.Category;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class LyRegistery {
    public static void main(String[] args){
        SpringApplication.run(LyRegistery.class);

        Category category=new Category();
    }
}
