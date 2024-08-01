package io.github.jerometseng.requestlog.core

import cn.hutool.core.util.IdUtil
import cn.hutool.json.JSONUtil
import io.github.jerometseng.requestlog.common.Constants.isSwaggerResource
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.tags.Tags
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import org.springframework.stereotype.Component
import java.lang.reflect.Parameter
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*
import javax.annotation.PostConstruct
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

/**
 * 对请求的控制类做日志记录
 * @author 曾兴顺
 */
@Aspect
@Component
class ControllerLog : InitializingBean {
    @Resource
    lateinit var request: HttpServletRequest

    @Resource
    lateinit var environment: Environment

    // 日志记录器
    val log: Logger = LoggerFactory.getLogger(ControllerLog::class.java)

    // 静态类
    companion object {
        // 记录 [ 请求线程ID ] 的线程隔离环境
        var requestId = ThreadLocal<String>()

        // 切点定义
        private const val REQUEST = "@annotation(org.springframework.web.bind.annotation.RequestMapping)"
        private const val POST = "@annotation(org.springframework.web.bind.annotation.PostMapping)"
        private const val PUT = "@annotation(org.springframework.web.bind.annotation.PutMapping)"
        private const val DELETE = "@annotation(org.springframework.web.bind.annotation.DeleteMapping)"
        private const val GET = "@annotation(org.springframework.web.bind.annotation.GetMapping)"
        const val POINTCUT = "$REQUEST || $POST || $PUT || $DELETE || $GET"
    }

    /**
     * controller 层执行前打印的日志信息
     * @author 曾兴顺  2023/7/7
     */
    @Before(POINTCUT)
    fun beforeEnterController(joinPoint: JoinPoint) {
        if (isSwaggerResource(request.requestURL.toString())) {
            return
        }
        // 生成请求ID
        val reqId = IdUtil.simpleUUID()
        // 设置请求ID
        requestId.set(reqId)
        // 拿到方法信息
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        // 如果有不记录日志的注解 则直接返回
        if (method.isAnnotationPresent(NoLog::class.java))
            return
        // 控制层方法全路径名
        val serviceFullName = "${joinPoint.target.javaClass.name}.${method.name}"
        // 日志拼接
        val logInfo = StringBuilder()
        logFormatBefore(logInfo)
        logInfo.append("服务执行操作 [ 开始 ]： -------START------- ")
        logInfo.append("\n\t\t请求IP-ID：[ ${getRemoteIp()} ] - [ $reqId ] ")
        // 公共日志处理
        commonLogHandle(logInfo, joinPoint)
        logInfo.append("\n\t\t执行服务：[ $serviceFullName ] ")
        // 请求参数拼接
        logRequestParameter(logInfo, joinPoint)
        logFormatAfter(logInfo)
        // 打印日志
        log.info(logInfo.toString())
    }


    /**
     * controller 层执行后打印的日志信息
     * @author 曾兴顺  2023/7/7
     */
    @AfterReturning(POINTCUT)
    fun afterFinishController(joinPoint: JoinPoint) {
        if (isSwaggerResource(request.requestURL.toString())) {
            return
        }
        // 拿到方法信息
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        // 如果有不记录日志的注解 则直接返回
        if (method.isAnnotationPresent(NoLog::class.java))
            return
        // 拼接日志
        val logInfo = StringBuilder()
        logFormatBefore(logInfo)
        logInfo.append("服务执行操作 [ 结束 ]： -------END------- ")
        logInfo.append("\n\t\t请求IP-ID：[ ${getRemoteIp()} ] - [ ${requestId.get()} ] ")
        // 公共日志处理
        commonLogHandle(logInfo, joinPoint)
        // 控制层方法全路径名
        val serviceFullName = "${joinPoint.target.javaClass.name}.${method.name}"
        logInfo.append("\n\t\t执行服务：[ ").append(serviceFullName).append(" ] ")
        logFormatAfter(logInfo)
        log.info(logInfo.toString())
        requestId.remove()
    }


    /**
     * controller 层执行出错后打印日志信息
     * @author 曾兴顺  2023/7/7
     */
    @AfterThrowing(value = POINTCUT, throwing = "ex")
    fun logAfterError(joinPoint: JoinPoint, ex: Exception) {
        if (isSwaggerResource(request.requestURL.toString())) {
            return
        }
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        // 如果有不记录日志的注解 则直接返回
        if (method.isAnnotationPresent(NoLog::class.java))
            return
        // 日志拼接
        val logInfo = StringBuilder()
        logFormatBefore(logInfo)
        logInfo.append("服务执行操作 [ 出错 ]： -------ERROR------- ")
        logInfo.append("\n\t\t请求ID：[ ${requestId.get()} ] ")
        logInfo.append("\n\t\t请求IP：[ ${getRemoteIp()} ] ")
        logInfo.append("\n\t\t出错的服务接口：[ ${method.declaringClass.name}.${method.name} ] ")
        // 拼装请求参数
        logRequestParameter(logInfo, joinPoint)
        logFormatAfter(logInfo)
        log.error(logInfo.toString())
        requestId.remove()
    }


    /**
     * 打印拼装请求参数信息
     * @author 曾兴顺  2023/7/7
     */
    private fun logRequestParameter(logInfo: StringBuilder, joinPoint: JoinPoint) {
        // 拿到方法信息
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        // 如果有不记录日志的注解 则直接返回
        if (method.isAnnotationPresent(NoLog::class.java))
            return
        logInfo.append("\n\t\t访问参数列表：")
        // 请求参数拼接
        val parameters: Array<Parameter> = method.parameters
        val args = joinPoint.args
        assert(parameters.size == args.size)
        if (parameters.isNotEmpty()) {
            for (i in parameters.indices) {
                if (i == 0) {
                    logInfo.append("[ ")
                }
                logInfo.append(parameters[i].name).append(" = ").append(JSONUtil.toJsonStr(args[i])).append("")
                if (i == parameters.size - 1) {
                    logInfo.append(" ] ")
                }
            }
        } else {
            logInfo.append("[ 无参数 ]")
        }
    }

    /**
     * 获取到 controller 层配置的 Swagger 接口中文名
     *
     * 能一眼看出调用的什么接口
     * @author 曾兴顺  2023/7/7
     */
    fun getApiName(joinPoint: JoinPoint): String? {
        return joinPoint.run {
            val javaClass = target.javaClass
            val apiParentName = getApiParentName(javaClass)
            val apiChildName = getApiChildName(joinPoint)
            if (apiParentName == null && apiChildName == null) {
                null
            } else {
                "${if (apiParentName.isNullOrBlank()) "未配置接口名称" else apiParentName} ### ${if (apiChildName.isNullOrBlank()) "未配置方法名称" else apiChildName}"
            }
        }
    }

    /**
     * 获取到 controller 层配置的 Swagger 接口父级名称
     */
    fun getApiParentName(javaClass: Class<Any>): String? {

        return try {
            if (javaClass.isAnnotationPresent(Api::class.java)) {
                val tags = javaClass.getAnnotation(Api::class.java).tags
                if (tags.isNotEmpty()) {
                    tags.joinToString("-")
                } else {
                    javaClass.getAnnotation(Api::class.java).value
                }
            } else {
                null
            }
        } catch (ex: Throwable) {
            null
        }
            ?: (try {
                if (javaClass.isAnnotationPresent(Tag::class.java)) {
                    javaClass.getAnnotation(Tag::class.java).name
                } else {
                    null
                }
            } catch (ex: Throwable) {
                null
            } ?: try {
                if (javaClass.isAnnotationPresent(Tags::class.java)) {
                    javaClass.getAnnotation(Tags::class.java).value.joinToString("-")
                } else {
                    null
                }
            } catch (ex: Throwable) {
                null
            })

    }

    /**
     * 获取到 controller 层配置的 Swagger 接口子级名称
     */
    fun getApiChildName(joinPoint: JoinPoint): String? {
        val method = (joinPoint.signature as MethodSignature).method
        return try {
            if (method.isAnnotationPresent(ApiOperation::class.java)) {
                method.getAnnotation(ApiOperation::class.java).value
            } else {
                null
            }
        } catch (ex: Throwable) {
            null
        }
            ?: try {
                if (method.isAnnotationPresent(Operation::class.java)) {
                    method.getAnnotation(Operation::class.java).summary
                } else {
                    null
                }
            } catch (ex: Throwable) {
                null
            }
    }


    /**
     * 打印前的分隔符
     * @author 曾兴顺  2023/7/7
     */
    private fun logFormatBefore(logInfo: StringBuilder) {
        logInfo.append("\n")
        logInfo.append("<${"=".repeat(110)}>")
        logInfo.append("\n\t")
    }


    /**
     * 打印后的分隔符
     * @author 曾兴顺  2023/7/7
     */
    private fun logFormatAfter(logInfo: StringBuilder) {
        logInfo.append("\n")
        logInfo.append("<${"=".repeat(110)}>")
        logInfo.append("\n")
    }


    /**
     * 获取请求的IP地址
     */
    private fun getRemoteIp(): String {
        try {
            var ipAddress = request.getHeader("X-Forwarded-For")
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equals(ipAddress, true)) {
                ipAddress = request.getHeader("Proxy-Client-IP")
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equals(ipAddress, true)) {
                ipAddress = request.getHeader("WL-Proxy-Client-IP")
            }
            if (ipAddress == null || ipAddress.isEmpty() || "unknown".equals(ipAddress, true)) {
                ipAddress = request.remoteAddr
            }
            return ipAddress.split(",")[0]
        } catch (ex: Exception) {
            return ex.message ?: "未知IP"
        }
    }

    /**
     * 共同日志打印
     */
    private fun commonLogHandle(logInfo: StringBuilder, joinPoint: JoinPoint) {
        // 打印请求的接口中文名
        try {
            val apiName = getApiName(joinPoint)
            if (apiName != null) {
                logInfo.append("\n\t\t接口名称：[ $apiName ] ")
            }
        } finally {
            logInfo.append("\n\t\t请求方式及URI：[ ${request.method} - ${request.requestURI} ] ")
        }
    }

    override fun afterPropertiesSet() {
        val result = try {
            // 尝试加载类
            Class.forName("com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
        if (result) {
            val port = environment["server.port"] ?: "8080"
            var contextPath = environment["server.servlet.context-path"] ?: ""
            contextPath =
                if (contextPath.last() == '/') contextPath.substring(0, contextPath.length - 1) else contextPath
            val logInfo = StringBuilder()
            logFormatBefore(logInfo)
            logInfo.append("\t\tKnife4J 文档地址：http://${getHostAddresses()}:${port}${contextPath}/doc.html")
            logFormatAfter(logInfo)
            log.info(logInfo.toString())
        }
    }

    fun getHostAddresses(): String {
        val addresses = mutableListOf<String>()
        try {
            // 获取本机所有网络接口
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (netInterface in networkInterfaces) {
                // 排除虚拟网络接口（如环回接口）
                if (!netInterface.isLoopback && netInterface.isUp) {
                    val inetAddresses = Collections.list(netInterface.inetAddresses)

                    for (inetAddress in inetAddresses) {
                        // 只添加IPv4地址
                        if (inetAddress is Inet4Address) {
                            addresses.add(inetAddress.getHostAddress())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.error(e.message)
        }
        return if (addresses.isEmpty()) "127.0.0.1" else addresses[0]
    }

    @PostConstruct
    fun printBanner(){
        // 生成地址
        //https://patorjk.com/software/taag/?spm=a2c6h.12873639.article-detail.7.7acc2c9aiecGQi#p=display&f=Calvin%20S&t=REQUEST-LOG
        // 生成类型：Calvin S
        println("""${"\r\n"}
            ╦═╗╔═╗╔═╗ ╦ ╦╔═╗╔═╗╔╦╗  ╦  ╔═╗╔═╗
            ╠╦╝║╣ ║═╬╗║ ║║╣ ╚═╗ ║───║  ║ ║║ ╦
            ╩╚═╚═╝╚═╝╚╚═╝╚═╝╚═╝ ╩   ╩═╝╚═╝╚═╝   - developed by JeromeTseng.
            https://gitee.com/zengxingshun/springboot-request-log${"\r\n"}
        """.trimIndent())
    }

}