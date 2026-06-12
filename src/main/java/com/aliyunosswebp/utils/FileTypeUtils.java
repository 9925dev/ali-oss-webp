package com.aliyunosswebp.utils;

import org.apache.commons.lang3.StringUtils;

public final class FileTypeUtils {

    private FileTypeUtils() {
    }

    public static String getMineType(byte[] data) {
        return getMineType(data, null);
    }

    public static String getMineType(byte[] data, String filename) {
        if (data == null || data.length < 4) {
            return "application/octet-stream";
        }
        if (data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
            return "image/jpeg";
        }
        if (data[0] == (byte) 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47) {
            return "image/png";
        }
        if (data[0] == 0x47 && data[1] == 0x49 && data[2] == 0x46) {
            return "image/gif";
        }
        if (data[0] == 0x42 && data[1] == 0x4D) {
            return "image/bmp";
        }
        if (data.length >= 12
                && data[0] == 0x52 && data[1] == 0x49 && data[2] == 0x46 && data[3] == 0x46
                && data[8] == 0x57 && data[9] == 0x45 && data[10] == 0x42 && data[11] == 0x50) {
            return "image/webp";
        }
        if (StringUtils.isNotBlank(filename)) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                return "image/jpeg";
            }
            if (lower.endsWith(".png")) {
                return "image/png";
            }
            if (lower.endsWith(".gif")) {
                return "image/gif";
            }
            if (lower.endsWith(".bmp")) {
                return "image/bmp";
            }
            if (lower.endsWith(".webp")) {
                return "image/webp";
            }
        }
        return "application/octet-stream";
    }
}
