package com.nageoffer.shortlink.admin.remote.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 回收站分页查询请求参数
 */
@Data
public class ShortLinkRecycleBinPageReqDTO extends Page {

    /**
     * 短链接分组标识列表
     */
    private List<String> gidList;
}
