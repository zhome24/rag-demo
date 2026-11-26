package com.example.rag.demo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。*
 * 核心原则：先给"可预期的框架异常"写精确的 handler，最后才用 Exception 兜底。
 *  * 因为 Spring 会挑"最具体"的异常类型匹配 —— 精确的在前，兜底只接真正没预料到的东西。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 日志对象：用法 log.info / log.error，比 System.out.println 强在能分级、能带堆栈
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ============ 一、业务异常 ============
    /**
     * 业务异常 → HTTP 400，把 message 原样告诉用户
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        //业务异常是“预期内”的，用warn不要用error，避免误报警
        log.warn("业务异常：code={},message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.fail(e.getCode(), e.getMessage()));
    }

    // ============ 二、请求本身有问题（4xx，属于调用方的错） ============
    /**
     * 路径不存在->404
     * Spring 6.1+ 的静态资源处理器找不到资源时抛这个（而不是sendError）
     * 不显示处理的话，会被下面的Exception兜底吃掉，变成500
     */
    @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<Result<Void>> handNoResource(NoResourceFoundException e){
        log.warn("路径不存在：{}",e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(404, "接口不存在：" + e.getResourcePath()));
    }

    /**有匹配的路径但没有对应的HTTP方法->405
     * 例：/api/chat只支持POST，用GET访问就就是它*/
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e){
        log.warn("请求方法不支持：{}",e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(405, "该接口不支持" + e.getMethod() + "方法"));
    }

    /**Content-Type不对->415
     * 例：POST /api/
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaType(HttpMediaTypeNotSupportedException e){
        log.warn("不支持的Content-Type:{}",e.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Result.fail(415, "请求头Content-Type需为application/json"));
    }

    /**请求体不是合法JSON->400
     * 例：{"message":}少个值、或多个逗号
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException e){
        log.warn("请求体无法解析：{}",e.getMessage());
        return ResponseEntity.badRequest()
                .body(Result.fail(400, "请求体不是合法的JSON"));
    }

    /**缺少必填的URL参数->400
     * 例：访问chat（老接口）却没带？message=
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e){
        log.warn("缺少请求参数：{}",e.getParameterName());
        return ResponseEntity.badRequest()
                .body(Result.fail(400, "缺少参数:" + e.getParameterName()));
    }


    // ============ 三、参数校验（@Valid 不通过） ============
    /**
     * 参数校验失败（@Valid不通过）->HTTP 400，把每个字段的错误并起来
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        //getBindingResult()里存着所有校验错误，这里只取“字段名+提示”
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ":" +defaultMessage(fe))
                .collect(Collectors.joining(";"));
        log.warn("参数校验失败：{}",detail);
        return ResponseEntity.badRequest().body(Result.fail(400,detail));
    }

    // ============ 四、兜底：真正的服务端错误（5xx） ============
    /**兜底：所有没被上面接住的异常->HTTP 500，只给用户一句安全的话*/
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e){
        //堆栈只进日志（你自己看），不返回给前端（避免泄露内部实现）
        log.error("未预期的异常",e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500,"服务器开小差了，请稍后再试"));
    }

    /**小工具：取错误提示，防止message为null时拼出"null"*/
    private String defaultMessage(FieldError fe){
        return fe.getDefaultMessage() == null ? "参数不合法" : fe.getDefaultMessage();
    }


}
