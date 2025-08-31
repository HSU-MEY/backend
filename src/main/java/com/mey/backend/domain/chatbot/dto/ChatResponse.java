package com.mey.backend.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "챗봇 응답 DTO")
@Getter
@Builder
@AllArgsConstructor
public class ChatResponse {
    
    @Schema(description = "응답 타입", example = "QUESTION")
    private ResponseType responseType;
    
    @Schema(description = "응답 메시지", example = "어느 지역의 루트를 찾고 계신가요?")
    private String message;
    
    @Schema(description = "루트 추천 정보 (ROUTE_RECOMMENDATION일 때만 포함)")
    private RouteRecommendation routeRecommendation;
    
    public enum ResponseType {
        QUESTION,           // 추가 정보가 필요한 경우
        ROUTE_RECOMMENDATION // 루트 추천 완료
    }
    
    @Schema(description = "루트 추천 정보")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class RouteRecommendation {
        
        @Schema(description = "루트 ID", example = "123")
        private Long routeId;
        
        @Schema(description = "루트 접근 엔드포인트", example = "/api/routes/123")
        private String endpoint;
        
        @Schema(description = "루트명", example = "서울 K-POP 투어")
        private String title;
        
        @Schema(description = "루트 설명", example = "서울의 주요 K-POP 명소들을 둘러보는 루트입니다.")
        private String description;
        
        @Schema(description = "예상 비용", example = "50000")
        private Integer estimatedCost;
        
        @Schema(description = "소요 시간 (분)", example = "240")
        private Integer durationMinutes;
    }
}