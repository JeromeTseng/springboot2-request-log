package io.github.jerometseng.requestlog.core


/**
 * controller层加了该注解 返回的结果不会被Result类包裹
 * @author 曾兴顺
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@MustBeDocumented
annotation class RawResponse
