package com.ziyang.aiagent.chatmemory;

import com.ziyang.aiagent.entity.po.ChatMessage;
import com.ziyang.aiagent.service.impl.ChatMessageServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@Component
public class MySQLChatMemory implements ChatMemory {
    @Resource
    private ChatMessageServiceImpl chatMessageService;
    @Override
    public void add(String conversationId, Message message) {
        ChatMessage chatMessage = ChatMessage.fromMessage(conversationId, message);
        chatMessageService.save(chatMessage);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        List<ChatMessage> chatMessages = messages.stream()
                .map(message -> ChatMessage.fromMessage(conversationId, message))
                .toList();
        chatMessages.forEach(chatMessageService::save);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<ChatMessage> chatMessages = chatMessageService.findLatestMessages(conversationId, lastN);
        return chatMessages.stream()
                .map(ChatMessage::toMessage)
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        chatMessageService.deleteByConversationId(conversationId);
    }
}
