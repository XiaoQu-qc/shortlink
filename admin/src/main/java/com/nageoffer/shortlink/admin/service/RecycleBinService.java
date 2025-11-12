package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

/**
 * url 回收站服务接口
 */
public interface RecycleBinService {

    /**
     * 分页查询回收站里的短链接
     * @return
     */
    Result<IPage<ShortLinkPageRespDTO>> pageRecycleBinShortLink(ShortLinkRecycleBinPageReqDTO requestParam);
}
