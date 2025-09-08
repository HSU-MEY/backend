package com.mey.backend.domain.chatbot.controller;

import com.mey.backend.domain.chatbot.dto.ChatRequest;
import com.mey.backend.domain.chatbot.dto.ChatResponse;
import com.mey.backend.domain.chatbot.exception.LLMException;
import com.mey.backend.domain.chatbot.service.ChatService;
import com.mey.backend.global.payload.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "챗봇", description = "한류 루트 추천 챗봇 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * 한류 루트 추천 챗봇 API
     */
    @Operation(
            summary = "한류 루트 추천 챗봇",
            description = "사용자 질문을 분석하여 적절한 한류 루트를 추천하거나 추가 정보를 요청합니다."
    )
    @PostMapping("/query")
    public CommonResponse<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        log.info("chat request: {}", request.getQuery());

        try {
            ChatResponse response = chatService.processUserQuery(request);
            return CommonResponse.onSuccess(response);
        } catch (Exception e) {
            log.error("챗봇 처리 중 오류 발생", e);
            throw new LLMException();
        }
    }
}
