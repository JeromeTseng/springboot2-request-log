package per.jerome.requestlog.configure

import org.springframework.context.annotation.Import
import per.jerome.requestlog.core.ControllerLog
import per.jerome.requestlog.core.ResultHandler


@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Import(value = [ControllerLog::class, ResultHandler::class])
annotation class EnableRequestLog
