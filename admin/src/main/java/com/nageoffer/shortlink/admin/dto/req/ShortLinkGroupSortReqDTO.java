package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

/**
 * 分组排序接口请求参数
 */
@Data
public class ShortLinkGroupSortReqDTO {

    /**
     *分组id
     */
    private String gid;

    /**
     * 排序
     */
    private Integer sortOrder;
}
