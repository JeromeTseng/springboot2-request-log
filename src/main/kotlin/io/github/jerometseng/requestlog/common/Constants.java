package io.github.jerometseng.requestlog.common;

import cn.hutool.core.util.StrUtil;

/**
 * 常量类
 */
public abstract class Constants {

    // swagger资源
    static final String[] SWAGGER_RESOURCE = {"swagger-resources", "v2/api-docs","v3/api-docs", "webjars", "swagger-ui.html"};

    /**
     * 是否是swagger资源
     * @param contentPath 路径字符串
     */
    public static Boolean isSwaggerResource(String contentPath){
        return StrUtil.containsAnyIgnoreCase(contentPath, SWAGGER_RESOURCE);
    }

}
