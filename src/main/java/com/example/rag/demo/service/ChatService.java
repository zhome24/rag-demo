package com.example.rag.demo.service;

import com.example.rag.demo.common.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 聊天服务：只负责"把问题发给模型、拿回答案、处理 AI 层的失败"。
 * <p>
 * 为什么不写在 Controller 里：
 * 1) 以后要加"多轮记忆""流式输出"，都改这里，Controller 不用动
 * 2) 可以被其他地方复用（比如定时任务、另一个接口）
 * 3) 单元测试时可以单独测这个类
 */
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 单论回答
     *
     * @param userMessage 用户问题
     * @return 模型回答
     * @throws BizException 模型调用失败或返回空内容时抛出
     */
    public String chat(String userMessage) {

        try {
            //prompt()开一次对话；user()传用户消息；call()同步请求；content()取问本回答
            String answer = chatClient.prompt().user(userMessage).call().content();

            if (answer == null || answer.isBlank()) {
                //模型返回空是“预期内可能发生”的异常->业务异常，HTTP 400/502
                throw new BizException(502, "模型没有返回内容，请换个问法再试");
            }
            return answer;
        } catch (BizException e) {
            throw e;//自己抛的业务异常，原样往外丢，不要被下面的catch吞掉
        } catch (Exception e) {
            //网络超时、key无效（401）、额度不足（429）等都会走到这里
            //完整原因只写日志（自己看），返回给前端一句模糊但安全的话
            log.error("调用大模型失败，userMessage 长度={}", userMessage == null ? 0 :
                    userMessage.length(), e);
            throw new BizException(502, "调用大模型失败，请稍后重试");
        }
    }


}
