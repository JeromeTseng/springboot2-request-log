### 使用方式
* 在配置类型加上注解 `@EnableRequestLog` 即可开启请求日志打印
* 在controller层方法上加上 `@NoLog`注解则关闭该请求方法的日志打印
* 在controller层方法上加上 `@RawResponse`注解，响应结果则不会被per.jerome.requestlog.model.Result 类包裹

### 请求日志格式如下
```text
2024-07-31 14:47:43.971  INFO 24376 --- [io-17812-exec-8] p.jerome.requestlog.core.ControllerLog   : 
<==============================================================================================================>
	服务执行操作 [ 开始 ]： -------START------- 
		请求IP-ID：[ 192.168.6.116 ] - [ cc71f2fb3fae4c368a49a69a4ed834f9 ] 
		接口名称：[ 测试模块2 ### 描述2 ] 
		请求方式及URI：[ POST - /test2/description ] 
		执行服务：[ com.xiaominfo.knife4j.demo.web.Test2Controller.description ] 
		访问参数列表：[ listBo = {"userDataBos":[{"age":0}]} ] 
<==============================================================================================================>

2024-07-31 14:47:43.974  INFO 24376 --- [io-17812-exec-8] p.jerome.requestlog.core.ControllerLog   : 
<==============================================================================================================>
	服务执行操作 [ 结束 ]： -------END------- 
		请求IP-ID：[ 192.168.6.116 ] - [ cc71f2fb3fae4c368a49a69a4ed834f9 ] 
		接口名称：[ 测试模块2 ### 描述2 ] 
		请求方式及URI：[ POST - /test2/description ] 
		执行服务：[ com.xiaominfo.knife4j.demo.web.Test2Controller.description ] 
<==============================================================================================================>
```
<mark>如果要求某个接口不打印日志，可以在接口上加 **@NoLog**  注解<mark>，如下：

```java
import org.springframework.web.bind.annotation.GetMapping;
import per.jerome.requestlog.core.NoLog;

@GetMapping("/test")
@NoLog
public String test() {
    return "我是返回结果";
}
```

### [该框架可与Knife4j所有版本兼容](https://doc.xiaominfo.com/docs/quick-start)
* [SpringBoot2 openApi2规范](https://doc.xiaominfo.com/docs/quick-start#openapi2)
* [SpringBoot2 openApi3规范](https://doc.xiaominfo.com/docs/quick-start#openapi3)
* [SpringBoot3](https://doc.xiaominfo.com/docs/quick-start#spring-boot-3)

<mark>注：如果接口上加了Knife4j相关注解，打印日志中会有[接口名称]信息，以三个#号分开，如下：<mark>
```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "测试模块2")
@RestController
@RequestMapping("/test")
public class Test2Controller {

    @Operation(summary = "描述2")
    @GetMapping("/description")
    public String description() {
        return "我是返回结果";
    }

}
```
<mark>调用如上接口时，打印日志中会有如下信息：<mark>
```text
接口名称：[ 测试模块2 ### 描述2 ] 
```


### 响应内容会被包裹
```json
{
  "data": "我是返回结果",
  "code": 200,
  "success": true,
  "message": "请求成功！"
}
```
<mark>如果想响应内容不被包裹，请使用 **@RawResponse** 注解<mark>，如下：

```java
import org.springframework.web.bind.annotation.GetMapping;
import per.jerome.requestlog.core.RawResponse;

@GetMapping("/test")
@RawResponse
public String test() {
    return "我是返回结果";
}
```