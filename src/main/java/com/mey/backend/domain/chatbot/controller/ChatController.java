package com.mey.backend.domain.chatbot.controller;

import com.mey.backend.domain.chatbot.dto.ChatRequest;
import com.mey.backend.domain.chatbot.exception.LLMException;
import com.mey.backend.domain.chatbot.service.ChatService;
import com.mey.backend.global.payload.CommonResponse;
import com.mey.backend.global.payload.status.ErrorStatus;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "챗봇", description = "챗봇 관련 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * 사용자 질문에 대한 LLM 답변을 생성하는 API
     */
    @PostMapping("/query")
    public CommonResponse<String> sendMessage(@RequestBody ChatRequest request) {
        log.info("chat request: {}", request);

        String systemMessage = "You are a helpful assistant.";

        String response = chatService.openAiChat(request.getQuery(), systemMessage).getResult().getOutput().getText();

        if (response == null) {
            log.error("LLM 응답 실패");
            throw new LLMException(ErrorStatus.LLM_API_ERROR);
        }

        return CommonResponse.onSuccess(response);
    }
}
