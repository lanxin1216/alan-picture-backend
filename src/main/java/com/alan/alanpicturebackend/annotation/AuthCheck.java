package com.alan.alanpicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author alan
 * @Description: 权限校验自定义注解
 * @Date: 2025/1/7 16:45
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须需要某个角色
     */
    String mustRole() default "";
}
