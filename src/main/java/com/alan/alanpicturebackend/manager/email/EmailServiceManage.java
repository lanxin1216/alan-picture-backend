package com.alan.alanpicturebackend.manager.email;

import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.manager.email.store.InRedisVerificationCodeStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.security.SecureRandom;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alanK
 * @Description: 邮件服务管理类
 * @Date: 2025/9/29 17:22
 */
@Component
@Slf4j
public class EmailServiceManage {

    @Resource
    private InRedisVerificationCodeStore inRedisVerificationCodeStore;

    @Resource
    private JavaMailSender mailSender;

    @Resource
    private TemplateEngine templateEngine;

    // 发件人邮箱
    @Value("${spring.mail.username}")
    private String from;
    // 邮件主题
    private static final String SUBJECT = "【屿图】邮箱验证码";
    // 邮件模板名称(HTML:templates/email-verification.html)
    private static final String TEMPLATE_NAME = "email-template";
    // 邮件发件人名称
    private static final String FROM_NAME = "屿图";
    // 验证码字符集
    private static final String CODE_CHARACTERS = "0123456789";
    // 验证码长度
    private static final int CODE_LENGTH = 6;
    // 随机数生成器
    private static final SecureRandom random = new SecureRandom();

    /**
     * 发送邮箱验证码
     */
    public void sendEmailVerificationCode(String email) {
        // 检查发送频率
        if (inRedisVerificationCodeStore.isFrequent(email)) {
            Long expireTime = inRedisVerificationCodeStore.getSendIntervalExpire(email);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, String.format("操作过于频繁，请%d秒后再试", expireTime));
        }

        // 验证验证码是否已存在
        if (inRedisVerificationCodeStore.exists(email)) {
            inRedisVerificationCodeStore.delete(email);
        }

        // 生成验证码
        String code = generateEmailVerificationCode();

        // 发送HTML邮件
        boolean sendSuccess = sendVerificationCode(email, code);

        if (sendSuccess) {
            // 保存验证码到Redis
            inRedisVerificationCodeStore.save(email, code);
            log.info("验证码发送并保存成功，邮箱：{}", email);
        } else {
            log.error("验证码发送失败，邮箱：{}", email);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    /**
     * 发送验证码邮件（HTML格式）
     */
    public boolean sendVerificationCode(String email, String code) {
        try {
            // 创建MimeMessage用于发送HTML邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 设置邮件基本信息
            InternetAddress fromAddress = new InternetAddress(from, FROM_NAME);
            helper.setFrom(fromAddress);
            helper.setSubject(SUBJECT);
            helper.setTo(email);

            // 准备模板变量
            Map<String, Object> variables = new HashMap<>();
            variables.put("code", code);
            variables.put("year", Year.now().getValue());

            // 使用Thymeleaf模板生成HTML内容
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(TEMPLATE_NAME, context);

            // 设置HTML内容，第二个参数true表示启用HTML格式
            helper.setText(htmlContent, true);

            // 发送邮件
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("发送屿图HTML邮件失败，邮箱：{}，错误信息：", email, e);
            return false;
        }
    }

    /**
     * 验证邮箱验证码
     */
    public boolean validateEmailVerificationCode(String email, String code) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("邮箱为空");
            return false;
        }
        if (code == null || code.trim().isEmpty()) {
            log.warn("验证码为空，邮箱：{}", email);
            return false;
        }

        // 去除可能的空格
        code = code.trim();

        boolean isValid = inRedisVerificationCodeStore.validate(email, code);

        if (!isValid) {
            log.warn("验证码错误，邮箱：{}", email);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }

        return true;
    }

    /**
     * 生成邮箱验证码（6位数字）
     */
    public String generateEmailVerificationCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = random.nextInt(CODE_CHARACTERS.length());
            code.append(CODE_CHARACTERS.charAt(index));
        }
        return code.toString();
    }
}