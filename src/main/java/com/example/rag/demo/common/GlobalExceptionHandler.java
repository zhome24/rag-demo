package com.example.rag.demo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。*
 *  @ RestControllerAdvice  = 拦截所有 Controller 抛出的异常，转成统一 JSON。
 * <p>
 * 之前你的问题是：catch 了异常却 return 一个字符串，HTTP 状态码仍是 200，
 * 前端和监控都分不清"成功"还是"失败"。现在不同错误对应不同状态码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 日志对象：用法 log.info / log.error，比 System.out.println 强在能分级、能带堆栈
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
