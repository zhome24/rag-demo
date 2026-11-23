package com.example.rag.demo.service;

import jakarta.annotation.PostConstruct;
import org.antlr.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识库服务：负责"启动时把 knowledge.txt 变成向量库"。
 *
 * @Service = 业务层组件，交给 Spring 管理（等价于 @Component，语义更明确）。
 * 和 Controller 的区别：这里不关心 HTTP，只关心数据。
 */
@Service
public class KnowledgeBaseService {
    private static final Logger log=LoggerFactory.getLogger(KnowledgeBaseService.class);
    /**
     * 相对“工作目录”的持久化文件；拼写沿用历史文件，改名会触发一次用心embeddding
     */
    private static final String STORE_FILE_NAME = "rag-verctor-store.json";
    private final SimpleVectorStore vectorStore;

    /**构造器注入：Spring会把AiConfig里造好的那个vectorStore传进来*/
    public KnowledgeBaseService(SimpleVectorStore vectorStore){
        this.vectorStore=vectorStore;
    }

    /**@PostConstruct:这个bean创建完成、依赖都注入之后，自动执行一次
     * 用它代替“在Controller构造器里干重活”
     */
    @PostConstruct
    public void init(){
        File storeFile=new File(STORE_FILE_NAME);
        try{
            //情况一：本地已有向量库文件->直接读，省掉一次embedding调用的钱和时间
            if(storeFile.exists()){
                vectorStore.load(storeFile);
                log.info("从本地文件恢复向量库，跳过embedding:{}",storeFile.getAbsolutePath());
                return;
            }
            //情况二：没有->读Knowledge.txt->切块->向量化->存盘
            //ClassPathResource表示“从classpath（编译后的resource目录）取文件”
            ClassPathResource resource = new ClassPathResource("knowledge.txt");
            String content;
            //try-with-resource:代码块结束自动关闭流，不用手写finally;
            try(InputStream in=resource.getInputStream()){
                //明确用UTF-8读，否则中文在Windows默认编码下会乱码
                content=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            }

            //Document是Spring AI的“文档”抽象：一段文本+元数据
            Document doc = new Document(content);

            //TokenTextSplitter:按token数量切块
            //为什么要切：模型上下文有限，且一次检索只会带回最相关的几块
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.apply(List.of(doc));

            //add:对每个分块调embedding接口生成向量（会产生网络请求和费用）
            vectorStore.add(chunks);

            //save:把“向量+原文”序列化成JSON落盘，下次启动直接读
            vectorStore.save(storeFile);
            log.info("已生成并把保存向量库:{},分块数：{}", storeFile.getAbsolutePath(), chunks.size());
        }catch (Exception e){
            //启动阶段失败必须炸出来
            //如果吞掉异常，应用会带着空知识库正常启动，用户提问时答非所问，更难排查
            throw new IllegalStateException("知识库初始化失败，请检查knowledge.txt与模型配置", e);

        }
    }

}
