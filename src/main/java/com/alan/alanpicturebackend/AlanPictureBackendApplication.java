package com.alan.alanpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author alan
 */
@SpringBootApplication
@MapperScan("com.alan.alanpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class AlanPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlanPictureBackendApplication.class, args);
    }

}
