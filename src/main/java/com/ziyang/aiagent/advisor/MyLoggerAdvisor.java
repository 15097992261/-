package com.ziyang.aiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 */
@Slf4j
public class MyLoggerAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        log.info("AI Request: {}", request.userText());
        return request;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        log.info("AI Response: {}", advisedResponse.response().getResult().getOutput().getText());
    }

    /**
     * 普通调用环绕处理方法
     * @param advisedRequest 用户请求
     * @param chain 调用链
     * @return AI响应
     */
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest);  //前置处理
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest); //执行调用链
        this.observeAfter(advisedResponse); //后置处理
        return advisedResponse;
    }
    /**
     * 流式调用环绕处理方法
     * @param advisedRequest 用户请求
     * @param chain 调用链
     * @return 响应流
     */
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest); //前置处理
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);//执行流式调用链
        // 使用消息聚合器处理流式响应，在完成后触发日志记录
        return (new MessageAggregator()).aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}
