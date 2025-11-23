package com.nageoffer.shortlink.admin.remote.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 短链接基础信息响应对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkBaseInfoRespDTO {

    /**
     * 短链接
     */
    private String fullShortUrl;

    /**
     * 原始链接
     */
    private String originUrl;

    /**
     * 描述信息
     */
    private String describe;
}
