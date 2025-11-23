package com.nageoffer.shortlink.admin.toolKit;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import java.net.URLEncoder;
import java.util.List;

/**
 * EasyExcel Web 工具类
 *
 * @author nageoffer
 */
public class EasyExcelWebUtil {

    @SneakyThrows
    public static void write(HttpServletResponse response, String fileName, Class<?> clazz, List<?> dataList) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        fileName = URLEncoder
                .encode(fileName, java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), clazz).sheet("Sheet1").doWrite(dataList);
    }
}
