package io.github.jerometseng.requestlog.common;

import cn.hutool.core.util.StrUtil;

/**
 * 常量类
 */
public abstract class Constants {

    static final String[] SWAGGER_RESOURCE = {"swagger-resources", "v2/api-docs","v3/api-docs", "webjars", "swagger-ui.html"};

    public static Boolean isSwaggerResource(String contentPath){
        return StrUtil.containsAnyIgnoreCase(contentPath, SWAGGER_RESOURCE);
    }

}
