package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkRecycleBinPageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;

/**
 * 回收站服务接口
 */
public interface RecycleBinService extends IService<ShortLinkDO> {

    /**
     * 保存回收站记录
     * @param requestParam
     */
    void saveRecycleBin(RecycleBinSaveReqDTO requestParam);

    /**
     * 回收站中的短链接分页
     * @param requestParam 短链接分页请求参数
     * @return 短链接分页返回信息
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecycleBinPageReqDTO requestParam);
}
