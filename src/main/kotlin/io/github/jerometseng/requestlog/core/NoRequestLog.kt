package io.github.jerometseng.requestlog.core

/**
 * controller层的方法加了该注解就不会打印日志
 * @author 曾兴顺  2023/8/2
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@MustBeDocumented
annotation class NoRequestLog
