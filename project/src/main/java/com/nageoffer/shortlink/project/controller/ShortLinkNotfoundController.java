package com.nageoffer.shortlink.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 短链接未找到处理controller
 */
@Controller
public class ShortLinkNotfoundController {

    /**
     * 短链接未找到页面
     * @return
     */
    @RequestMapping("/page/notfound")
    public String notFound() {
        return "notfound";
    }
}
