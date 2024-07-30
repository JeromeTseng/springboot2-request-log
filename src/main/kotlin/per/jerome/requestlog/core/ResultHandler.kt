package per.jerome.requestlog.core

import cn.hutool.core.util.StrUtil
import cn.hutool.json.JSONUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import kotlin.Exception

@RestControllerAdvice
class ResultHandler : ResponseBodyAdvice<Any> {
    val logger: Logger = LoggerFactory.getLogger(ResultHandler::class.java)

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return returnType.parameterType != Result::class.java && // 本身返回的不是Result
                !returnType.hasMethodAnnotation(RawResponse::class.java) && // 方法上没加@RawResponse注解
                !returnType.method!!.declaringClass.isAnnotationPresent(RawResponse::class.java) // 类上没加@RawResponse注解
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        response.headers.contentType = MediaType.APPLICATION_JSON
        // 不对swagger资源作包裹
        if (StrUtil.containsAny(request.uri.path, "swagger-resources", "v2/api-docs", "webjars", "swagger-ui.html")) {
            return body
        }
        // 消息体的类型为String类型会报错 将消息体包装后手动转成JSON
        if (body is String) {
            return JSONUtil.toJsonStr(Result.ok(body))
        }
        return Result.ok(body)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseBody
    fun validatedExceptionHandler(ex: MethodArgumentNotValidException): Result<String> {
        logger.error(ex.stackTraceToString())
        val allErrors = ex.allErrors
        return if (allErrors.isEmpty()) {
            Result.fail("参数校验错误！")
        } else {
            allErrors[0].run {
                Result.fail("参数 < ${ex.fieldError?.field} > 传入错误，${defaultMessage}")
            }
        }

    }

    @ExceptionHandler(Exception::class)
    @ResponseBody
    fun allExceptionHandler(ex: Exception): Result<String> {
        logger.error(ex.stackTraceToString())
        return Result.fail(ex.message)
    }
}