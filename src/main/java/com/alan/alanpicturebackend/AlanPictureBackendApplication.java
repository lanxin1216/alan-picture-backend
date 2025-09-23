package com.alan.alanpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author alan
 */
@SpringBootApplication
@MapperScan("com.alan.alanpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
@Transactional
public class AlanPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlanPictureBackendApplication.class, args);
    }

}
