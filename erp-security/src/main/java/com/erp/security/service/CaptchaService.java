package com.erp.security.service;

import com.erp.security.captcha.CaptchaProperties;
import com.erp.security.dto.CaptchaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码生成与校验（B1.4 Phase 2）。
 *
 * <p>用纯 java.awt 画 4 位字母数字（排除易混字符 0/O/1/I/L），
 * Redis key {@code auth:captcha:{uuid}}，TTL 由 {@link CaptchaProperties#getTtlMinutes()} 控制（默认 5 分钟）。
 * 校验大小写不敏感、一次性：成功 / 失败都会 DEL key，防重放。
 */
@Slf4j
@Service
public class CaptchaService {

    /** Redis key 前缀：erp:{module}:{entity}。 */
    public static final String KEY_PREFIX = "erp:security:captcha:";

    /** 字符集：去掉容易混淆的 0/O/1/I/L。 */
    private static final char[] CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final CaptchaProperties properties;

    public CaptchaService(StringRedisTemplate redisTemplate, CaptchaProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 生成验证码：返回 uuid + PNG base64（带 data URL 前缀）。
     * 答案写入 Redis，TTL = ttlMinutes。
     */
    public CaptchaResponse generate() {
        String code = randomCode(properties.getLength());
        String uuid = UUID.randomUUID().toString();
        String image = drawPng(code);

        redisTemplate.opsForValue().set(KEY_PREFIX + uuid, code,
                properties.getTtlMinutes(), TimeUnit.MINUTES);

        return CaptchaResponse.builder()
                .uuid(uuid)
                .imageBase64("data:image/png;base64," + image)
                .build();
    }

    /**
     * 校验验证码。无论成功失败，命中 key 即 DEL（一次性）。
     *
     * @return true 通过；false 不通过（key 不存在 / 已过期 / 答案不匹配）
     */
    public boolean verify(String uuid, String code) {
        if (!StringUtils.hasText(uuid) || !StringUtils.hasText(code)) {
            return false;
        }
        String key = KEY_PREFIX + uuid;
        String expected = redisTemplate.opsForValue().get(key);
        // 一次性：无论 expected 是否存在或匹配，都 DEL（避免暴力轮询）
        redisTemplate.delete(key);
        return expected != null && expected.equalsIgnoreCase(code);
    }

    /** 生成随机字符串。 */
    static String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    /** 画 PNG 并 base64 编码（不带 data URL 前缀）。 */
    private static String drawPng(String code) {
        int width = 120;
        int height = 40;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 背景
            g.setColor(new Color(240, 240, 245));
            g.fillRect(0, 0, width, height);
            // 干扰线
            for (int i = 0; i < 6; i++) {
                g.setColor(randomLightColor());
                g.drawLine(RANDOM.nextInt(width), RANDOM.nextInt(height),
                        RANDOM.nextInt(width), RANDOM.nextInt(height));
            }
            // 字符
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 26);
            g.setFont(font);
            int x = 18;
            for (char ch : code.toCharArray()) {
                AffineTransform original = g.getTransform();
                double angle = (RANDOM.nextDouble() - 0.5) * 0.5;
                g.rotate(angle, x, height / 2.0);
                g.setColor(randomDarkColor());
                g.drawString(String.valueOf(ch), x, 30);
                g.setTransform(original);
                x += 22;
            }
            // 干扰点
            for (int i = 0; i < 80; i++) {
                g.setColor(randomLightColor());
                g.fillRect(RANDOM.nextInt(width), RANDOM.nextInt(height), 1, 1);
            }
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            throw new IllegalStateException("captcha PNG encode failed", e);
        }
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static Color randomDarkColor() {
        return new Color(RANDOM.nextInt(120), RANDOM.nextInt(120), RANDOM.nextInt(120));
    }

    private static Color randomLightColor() {
        return new Color(150 + RANDOM.nextInt(80), 150 + RANDOM.nextInt(80), 150 + RANDOM.nextInt(80));
    }
}
