package io.github.jerometseng.requestlog.common

import cn.hutool.core.util.StrUtil

/**
 * 常量类
 */
abstract class Constants {
    companion object{
        // Swagger资源路径
        val SWAGGER_RESOURCE = listOf("swagger-resources", "v2/api-docs","v3/api-docs", "webjars", "swagger-ui.html")

        /**
         * 是否为swagger资源
         */
        fun isSwaggerResource(content:String):Boolean{
            return StrUtil.containsAny(content, *SWAGGER_RESOURCE.toTypedArray())
        }
    }
}