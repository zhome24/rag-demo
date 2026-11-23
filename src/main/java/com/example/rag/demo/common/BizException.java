package com.example.rag.demo.common;
/**
 * 业务异常：表示"可以预料、需要告诉用户原因"的错误。
 * 比如参数不对、模型返回空、知识库没初始化。
 *
 * 继承 RuntimeException（非受检异常）：
 *   方法签名不用写 throws，代码更干净；
 *   由全局异常处理器统一捕获，不会让异常堆栈直接暴露给前端。
 */
public class BizException extends RuntimeException{

    /**业务错误码，默认400（参数/调用问题）*/
    private final int code;

    public BizException(String message) {
        this(400,message);
    }
    public BizException(int code,String message) {
        super(message);
        this.code = code;
    }
    public int getCode(){
        return code;
    }
}
