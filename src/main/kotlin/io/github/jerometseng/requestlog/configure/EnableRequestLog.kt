package io.github.jerometseng.requestlog.configure

import org.springframework.context.annotation.Import
import io.github.jerometseng.requestlog.core.ControllerLog
import io.github.jerometseng.requestlog.core.ResultHandler


@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Import(value = [ControllerLog::class, ResultHandler::class])
annotation class EnableRequestLog
