package com.example.rag.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 相关对象的装配中心。
 *
 * @Configuration = 这个类里 @Bean 方法的返回值会交给 Spring 容器管理（单例）。
 * 为什么要单独一个类：ChatClient / VectorStore 是"基础设施"，不该由 Controller 主动 new。
 * 让容器管理后，任何地方需要，构造器里声明一下就能拿到（依赖注入）。
 */
@Configuration
public class AiConfig {
    /**向量库：先给空壳，真正的数据由knowledgeCaseService在启动时灌进去
     * 为什么先建空壳：ChatClient构造时需要它(QuestionAnswerAdvisor依赖它)
     * 但加载数据可能很慢（要调embedding接口），不能堵在这里
     */
    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel){
        //builder里传入EmbeddingModel:向量库靠它把文本转成向量
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**聊天客户端：整个项目“调大模型”的唯一入口
     * 参数由Spring自动注入，不需要自己new
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel,SimpleVectorStore vectorStore){
        return ChatClient.builder(chatModel)
                //系统提示词：每次对话都会带上，用来约束模型的身份和回答边界
                //"资料里没有就说没有"是RAG防幻觉的核心一句
                .defaultSystem(""" 
                        你是阿巴科技公司的内部知识库助手。
                        请严格根据检索到的资料回答；资料中没有的内容，明确回答“资料里没有相关信息”，不要编造。
                        回答使用简体中文，条理清晰。
                        """)
                //QuestionAnswerAdvisor:每次提问前自动做“检索->拼进prompt”
                //这是RAG的“R”（Retrieval），你在Controller里手写的那套逻辑就是他的手工版
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .build();
    }


}
