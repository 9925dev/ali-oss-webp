package com.aliyunosswebp.utils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * OSS 持久化转存参数专用 Base64 编码
 */
public final class OSSBase64Utils {

    private OSSBase64Utils() {
    }

    public static String encode(String value) {
        return Base64.getEncoder()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8))
                .replace('+', '-')
                .replace('/', '_')
                .replaceAll("=+$", "");
    }
}
