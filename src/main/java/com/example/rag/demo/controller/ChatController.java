package com.example.rag.demo.controller;

import com.example.rag.demo.common.Result;
import com.example.rag.demo.dto.ChatRequest;
import com.example.rag.demo.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天接口。
 * &#064;RestController  = @Controller + @ResponseBody：方法返回值直接序列化成 JSON。
 */
@RestController
public class ChatController {
    private final ChatService chatService;

    /**
     * 构造器注入：只依赖Service,不依赖任何AI细节
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 新接口：POST /api/chat,请求体{"message":"..."}
     *
     * @RequestBody:从 HTTP body里读JSON（而不是URL参数）
     * @Valid：触发ChatRequest里的@NotBlank/@Size校验，失败会抛给全局处理器 用POST的原因：长文本不受URL长度限制，也不会出现访问日志/浏览器历史里
     */
    @PostMapping("/api/chat")
    public Result<String> chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(chatService.chat(request.message()));
    }

    /**兼容旧接口：GET/chat?message=xxx（现在用的index.html在调它）
     * 第二步改成Vue后可以删掉
     * @Deprecated是“给人看”的标记，IDE会划线提醒别在新代码里用它
     */
    @Deprecated
    @GetMapping("/chat")
    public String legacyChat(@RequestParam String message){
        return chatService.chat(message);
    }
}
