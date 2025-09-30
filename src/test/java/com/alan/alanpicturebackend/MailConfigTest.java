package com.alan.alanpicturebackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
public class MailConfigTest {

    @Autowired
    private JavaMailSender mailSender;

    @Test
    public void testSendSimpleMail() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("alank_code@163.com"); // 发件人
            message.setTo("2779821074@qq.com");    // 收件人
            message.setSubject("测试邮件");
            message.setText("这是一封测试邮件，用于验证 Spring Boot 邮件配置是否正确。");

            mailSender.send(message);
            System.out.println("✅ 测试邮件发送成功，请检查收件邮箱。");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ 测试邮件发送失败：" + e.getMessage());
        }
    }
}