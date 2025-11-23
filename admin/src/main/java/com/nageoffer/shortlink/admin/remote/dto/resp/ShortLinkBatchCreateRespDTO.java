package com.nageoffer.shortlink.admin.remote.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量短链接创建响应对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortLinkBatchCreateRespDTO {

    /**
     * 批量创建短链接成功数量
     */
    private Integer total;

    /**
     * 批量创建返回参数
     */
    private List<ShortLinkBaseInfoRespDTO> baseLinkInfos;
}
