package com.ziyang.aiagent.app;

import com.google.errorprone.annotations.Var;
import com.ziyang.aiagent.advisor.ContentFilterAdvisor;
import com.ziyang.aiagent.advisor.MyLoggerAdvisor;
import com.ziyang.aiagent.chatmemory.FileBasedChatMemory;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;

import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.aop.Advisor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.function.Consumer;


import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    @Resource
    private VectorStore  loveAppVectorStore;

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";
//    @Value("classpath:/prompts/system-message.st")
//    private Resource systemResource;
//    SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
//    Message systemMessage = systemPromptTemplate.createMessage(Map.of());

    record LoveReport(String title, List<String> suggestions) {
    }


    public LoveApp(ChatModel dashscopeChatModel) {
        //初始化基于文件的对话记忆
        String fileDir = System.getProperty("user.dir") + "/chat-memory";
        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
//        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new ContentFilterAdvisor()
//                        //自定义日志advisor
//                        new MyLoggerAdvisor()
                )
                .build();
    }
    public String doChat(String message, String chatId) {
 //       Message userMessage = new UserMessage(message);
//        Prompt prompt = new Prompt(List.of(userMessage, systemMessage));
        ChatResponse response = chatClient
                .prompt()
                .user(message)  //传入用户prompt
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new MyLoggerAdvisor())
                //应用知识问答
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .call()
                .chatResponse();
//                .entity(LoveReport.class);
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用增强检索服务（云知识库服务）
//                .advisors(loveAppRagCloudAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }


}
