package com.nageoffer.shortlink.admin.remote.dto.req;

import lombok.Data;

/**
 * 回收站移除请求参数
 */
@Data
public class RecycleBinRemoveReqDTO {

    /**
     * 完整短链接
     */
    private String fullShortUrl;

    /**
     * 分组标识
     */
    private String gid;
}
