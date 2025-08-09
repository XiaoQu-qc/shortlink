package com.nageoffer.shortlink.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nageoffer.shortlink.admin.serialize.PhoneDesensitizationSerializer;
import lombok.Data;

/**
 * 用户返回参数实体
 */
@Data
public class UserRespDTO {

    private Long id;

    private String username;

    private String realName;

    private String mail;

    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;
}
