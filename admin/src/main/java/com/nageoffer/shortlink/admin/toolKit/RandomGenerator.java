package com.nageoffer.shortlink.admin.toolKit;

import java.security.SecureRandom;

/**
 * 分组id随机生成器
 */
public final class RandomGenerator {

    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 生成默认6位的随机分组ID
     * @return 分组ID
     */
    public static String generateRandomString() {
        return generateRandomString(6);
    }

    /**
     * 生成随机分组ID
     * @param length 生成多少位
     * @return 分组ID
     */
    public static String generateRandomString(int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

}
