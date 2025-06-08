package com.ziyang.aiagent.advisor;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 违禁词校验Advisor
 */
@Slf4j
public class ContentFilterAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    // 违禁词列表（实际项目中可以从数据库或配置中心加载）
    private final Set<String> bannedWords = Set.of(
        "暴力", "色情", "赌博", "毒品", "诈骗", 
        "政治敏感词1", "政治敏感词2" // 替换为实际需要过滤的词汇
    );

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // 设置较高的优先级，确保在日志记录等Advisor之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }

    /**
     * 检查文本是否包含违禁词
     */
    private boolean containsBannedWords(String text) {
        if (StringUtils.isEmpty(text)) {
            return false;
        }
        return bannedWords.stream().anyMatch(text::contains);
    }

    /**
     * 处理违禁词违规情况
     */
    private AdvisedResponse handleViolation(String message) {
        log.warn("检测到违禁内容: {}", message);
        // 创建一个 ChatResponse 对象
        ChatResponse chatResponse = new ChatResponse(
                List.of( new Generation(new AssistantMessage("您的输入或AI响应中包含违禁词，请修改后重试。"))),
                new ChatResponseMetadata()
        );
        // 返回包含 ChatResponse 的 AdvisedResponse
        // 创建 AdvisedResponse 对象，需要传入上下文
        Map<String, Object> context = new HashMap<>();
        return new AdvisedResponse(chatResponse,context);
    }
    /**
     * 普通调用环绕处理方法
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        // 检查用户输入
        if (containsBannedWords(request.userText())) {
            return handleViolation("用户输入包含违禁词: " + request.userText());
        }

        AdvisedResponse response = chain.nextAroundCall(request);

        // 检查AI响应
            String outputText = response.response().getResult().getOutput().getText();
            if (containsBannedWords(outputText)) {
                return handleViolation("AI响应包含违禁词: " + outputText);
            }
        return response;
    }

    /**
     * 流式调用环绕处理方法
     */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        // 检查用户输入
        if (containsBannedWords(request.userText())) {
            return Flux.just(handleViolation("用户输入包含违禁词: " + request.userText()));
        }

        return chain.nextAroundStream(request)
            .map(response -> {
                // 检查AI响应
                if (response != null && response.response() != null && response.response().getResult() != null) {
                    String outputText = response.response().getResult().getOutput().getText();
                    if (containsBannedWords(outputText)) {
                        return handleViolation("AI响应包含违禁词: " + outputText);
                    }
                }
                return response;
            });
    }
}