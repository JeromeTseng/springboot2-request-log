package per.jerome.requestlog.core

/**
 * 请求返回的统一格式
 * @author 曾兴顺
 */
data class Result<T>(
    var data: T?,
    var code: Int?,
    var success: Boolean,
    var message: String?
) {
    constructor(code: Int?, success: Boolean) : this(null, code, success, null)

    companion object {
        fun ok(): Result<Any?> {
            return Result(null, 200, true, "请求成功！")
        }

        fun <T> ok(data: T): Result<T> {
            return Result(data, 200, true, "请求成功！")
        }

        fun fail(): Result<Any?> {
            return Result(null, 500, false, "请求失败")
        }

        fun <T> fail(message: String?): Result<T> {
            return Result(null, 500, false, message)
        }
    }
}
