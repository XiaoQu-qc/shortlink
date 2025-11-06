package com.nageoffer.shortlink.project.toolKit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.Optional;

import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

public class LinkUtil {

    /**
     * 获取短链接缓存有效期
     * @param validDate
     * @return
     */
    public static long getLinkCacheValidTime(Date validDate) {
        return Optional.of(validDate)
                .map(each -> DateUtil.between(new Date(), each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }
}
