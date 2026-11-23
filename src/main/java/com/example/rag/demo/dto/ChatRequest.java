package com.example.rag.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 聊天请求体，对应 JSON：{"message": "住宿标准是多少"}
 * <p>
 * 用 record（Java 17）：编译器自动生成 构造器 / 访问器 message() / equals / hashCode / toString。
 * 比手写 20 行 getter/setter 清爽，而且天然不可变——到手之后谁都改不了。
 * <p>
 * 注意：注解写在"组件"上，Spring 校验时会作用到对应字段。
 */
public record ChatRequest (
        //@NotBlank:不能为null、不能是空串、不能只有空格
        //message属性就是报错时返回给前端的文案
        @NotBlank(message = "message不能为空")
        //@Size:限制长度，防止有人传10万字把模型上下文撑爆、也防止刷费用
        @Size(max = 2000, message = "message长度不能超过2000字")
        String message
){
}
