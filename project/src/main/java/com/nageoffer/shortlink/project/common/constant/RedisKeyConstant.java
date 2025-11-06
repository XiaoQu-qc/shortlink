package com.nageoffer.shortlink.project.common.constant;

/**
 * redis key 常量类
 */
public class RedisKeyConstant {

    /**
     * 短链接跳转key
     */
    public static final String GOTO_SHORT_LINK_KEY = "short-link_goto_%s";

    /**
     * 短链接空值跳转key 后面拼接fullShortLink,如果在缓存中命中该key，说明该短链接对应的原始链接不存在于数据库
     */
    public static final String GOTO_IS_NULL_SHORT_LINK_KEY = "short-link_is-null_goto_%s";

    /**
     * 短链接跳转锁前缀key
     */
    public static final String LOCK_GOTO_SHORT_LINK_KEY = "short-link_lock_goto_%s";
}
