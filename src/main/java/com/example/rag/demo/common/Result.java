package com.example.rag.demo.common;
/**
 * 统一响应体：所有接口都返回这个结构。
 * 好处：前端只需写一次解析逻辑，不用为每个接口猜返回值形状。
 *
 * 约定：
 *   code = 0   成功
 *   code != 0  失败（400 参数错 / 500 服务端错 / 502 调外部服务失败 …）
 */
public class Result<T> {
    /**业务状态码，0表示成功*/
    private int code;
    /**给人看的提示，前端可直接弹出来*/
    private String message;
    /**真正的业务数据；失败时为null*/
    private T data;

    /** 无参构造：Jackson 反序列化 / 序列化时需要 */
    public Result() {
    }
    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**成功的快捷构造：Result.ok("答案")*/
    public static <T> Result<T> ok(T data){
        return new Result<>(0, "ok", data);
    }

    /**失败的快捷构造：Result.fail(400,:message不能为空")*/
    public static <T> Result<T> fail(int code,String message){
        return new Result<>(code,message,null);
    }

    //getter/setter是给Jackson用的（它靠getter把字段写成JSON）
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
