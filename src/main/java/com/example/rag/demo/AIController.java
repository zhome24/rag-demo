package com.example.rag.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
public class AIController {
    private final ChatClient chatClient;
    private final SimpleVectorStore vectorStore;
    private final ChatMemoryRepository chatMemoryRepository=new InMemoryChatMemoryRepository();

    public AIController(ChatModel chatModel, EmbeddingModel embeddingModel) {
        vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        File storeFile = new File("rag-verctor-store.json");
        if (storeFile.exists()) {
            vectorStore.load(storeFile);
            System.out.println(">>>从本地文件恢复向量库，跳过embedding:" + storeFile.getAbsolutePath());

        } else {
            try {
                ClassPathResource resource = new ClassPathResource("knowledge.txt");
                InputStream inputStream = resource.getInputStream();
                String fileContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Document doc = new Document(fileContent);
                TokenTextSplitter splitter = new TokenTextSplitter();
                List<Document> chunks = splitter.apply(List.of(doc));
                vectorStore.add(chunks);

                vectorStore.save(storeFile);
                System.out.println(">>>已生成并保存向量库：" + storeFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        chatClient = ChatClient.builder(chatModel).defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build()).build();


    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        try {
            String answer = chatClient.prompt().user(message).call().content();
            return answer;
        } catch (Exception e) {
            e.printStackTrace();
            return "【报错】: " + e.getMessage();
        }
    }
}