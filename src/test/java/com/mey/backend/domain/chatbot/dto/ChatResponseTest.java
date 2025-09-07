package com.mey.backend.domain.chatbot.dto;

import com.mey.backend.domain.route.entity.Theme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChatBot DTO 테스트")
class ChatResponseTest {

    @Test
    @DisplayName("ChatRequest DTO가 올바르게 생성된다")
    void createChatRequest() {
        // given
        ChatContext context = ChatContext.builder()
                .themes(Arrays.asList(Theme.KPOP))
                .region("서울")
                .budget(50000)
                .build();

        // when
        ChatRequest request = ChatRequest.builder()
                .query("k-pop 루트 추천해줘")
                .context(context)
                .build();

        // then
        assertThat(request.getQuery()).isEqualTo("k-pop 루트 추천해줘");
        assertThat(request.getContext()).isNotNull();
        assertThat(request.getContext().getThemes()).contains(Theme.KPOP);
        assertThat(request.getContext().getRegion()).isEqualTo("서울");
        assertThat(request.getContext().getBudget()).isEqualTo(50000);
    }

    @Test
    @DisplayName("ChatResponse 질문 응답이 올바르게 생성된다")
    void createQuestionResponse() {
        // when
        ChatResponse response = ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.QUESTION)
                .message("어떤 테마의 루트를 찾고 계신가요?")
                .build();

        // then
        assertThat(response.getResponseType()).isEqualTo(ChatResponse.ResponseType.QUESTION);
        assertThat(response.getMessage()).contains("테마");
        assertThat(response.getRouteRecommendation()).isNull();
    }

    @Test
    @DisplayName("ChatResponse 루트 추천 응답이 올바르게 생성된다")
    void createRouteRecommendationResponse() {
        // given
        ChatResponse.RouteRecommendation recommendation = ChatResponse.RouteRecommendation.builder()
                .routeId(1L)
                .endpoint("/api/routes/1")
                .title("서울 K-POP 투어")
                .description("서울의 주요 K-POP 명소들을 둘러보는 루트")
                .estimatedCost(50000)
                .durationMinutes(240)
                .build();

        // when
        ChatResponse response = ChatResponse.builder()
                .responseType(ChatResponse.ResponseType.ROUTE_RECOMMENDATION)
                .message("서울의 멋진 K-POP 투어 루트를 추천드립니다!")
                .routeRecommendation(recommendation)
                .build();

        // then
        assertThat(response.getResponseType()).isEqualTo(ChatResponse.ResponseType.ROUTE_RECOMMENDATION);
        assertThat(response.getMessage()).contains("추천");
        assertThat(response.getRouteRecommendation()).isNotNull();
        assertThat(response.getRouteRecommendation().getRouteId()).isEqualTo(1L);
        assertThat(response.getRouteRecommendation().getTitle()).isEqualTo("서울 K-POP 투어");
        assertThat(response.getRouteRecommendation().getEndpoint()).isEqualTo("/api/routes/1");
    }

    @Test
    @DisplayName("ChatContext가 올바르게 생성된다")
    void createChatContext() {
        // when
        ChatContext context = ChatContext.builder()
                .themes(Arrays.asList(Theme.KPOP, Theme.KFOOD))
                .region("부산")
                .budget(100000)
                .preferences("야외 활동 선호")
                .durationMinutes(300)
                .build();

        // then
        assertThat(context.getThemes()).hasSize(2);
        assertThat(context.getThemes()).contains(Theme.KPOP, Theme.KFOOD);
        assertThat(context.getRegion()).isEqualTo("부산");
        assertThat(context.getBudget()).isEqualTo(100000);
        assertThat(context.getPreferences()).isEqualTo("야외 활동 선호");
        assertThat(context.getDurationMinutes()).isEqualTo(300);
    }

    @Test
    @DisplayName("DocumentSearchResult가 올바르게 생성된다")
    void createDocumentSearchResult() {
        // when
        DocumentSearchResult result = new DocumentSearchResult(
                "doc1",
                "서울 K-POP 투어 루트 정보",
                java.util.Map.of("source", "database"),
                0.9
        );

        // then
        assertThat(result.getId()).isEqualTo("doc1");
        assertThat(result.getContent()).contains("K-POP 투어");
        assertThat(result.getMetadata()).containsEntry("source", "database");
        assertThat(result.getSimilarityScore()).isEqualTo(0.9);
    }
}