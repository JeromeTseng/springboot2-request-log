package per.jerome.requestlog.core

import cn.hutool.core.util.IdUtil
import cn.hutool.json.JSONUtil
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Parameter
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

/**
 * 对请求的控制类做日志记录
 * @author 曾兴顺
 */
@Aspect
@Component
class ControllerLog{
    @Resource
    lateinit var request: HttpServletRequest

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
        logInfo.append("\n\t\t请求ID：[ $reqId ] ")
        logInfo.append("\n\t\t请求IP：[ ${getRemoteIp()} ] ")
        logInfo.append("\n\t\t请求方式及URI：[ ${request.method} - ${request.requestURI} ] ")
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
        logInfo.append("\n\t\t请求ID：[ ${requestId.get()} ] ")
        // 打印请求的接口中文名
        logInfo.append("\n\t\t请求IP：[ ${getRemoteIp()} ] ")
        logInfo.append("\n\t\t请求方式及URI：[ ${request.method} - ${request.requestURI} ] ")
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
    private fun getRemoteIp():String{
        var ipAddress = request.getHeader("X-Forwarded-For")
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equals(ipAddress,true)) {
            ipAddress = request.getHeader("Proxy-Client-IP")
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equals(ipAddress,true)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equals(ipAddress,true)) {
            ipAddress = request.remoteAddr
        }
        return ipAddress.split(",")[0]
    }

}