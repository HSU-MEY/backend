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
    
    @Schema(description = "루트 추천 정보 (ROUTE_RECOMMENDATION/EXISTING_ROUTES일 때만 포함)")
    private RouteRecommendation routeRecommendation;
    
    @Schema(description = "기존 루트 목록 (EXISTING_ROUTES일 때만 포함)")
    private java.util.List<ExistingRoute> existingRoutes;
    
    @Schema(description = "장소 정보 목록 (PLACE_INFO일 때만 포함)")
    private java.util.List<PlaceInfo> places;
    
    public enum ResponseType {
        QUESTION,            // 추가 정보가 필요한 경우
        ROUTE_RECOMMENDATION, // 새 루트 생성/추천 완료
        EXISTING_ROUTES,     // 기존 루트 목록 제공
        PLACE_INFO,          // 장소 정보 제공
        GENERAL_INFO         // 일반 정보 제공
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
    
    @Schema(description = "기존 루트 정보")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ExistingRoute {
        
        @Schema(description = "루트 ID", example = "456")
        private Long routeId;
        
        @Schema(description = "루트명", example = "부산 K-드라마 투어")
        private String title;
        
        @Schema(description = "루트 설명", example = "부산의 주요 드라마 촬영지들을 둘러보는 루트입니다.")
        private String description;
        
        @Schema(description = "예상 비용", example = "45000")
        private Integer estimatedCost;
        
        @Schema(description = "소요 시간 (분)", example = "300")
        private Integer durationMinutes;
        
        @Schema(description = "테마 목록")
        private java.util.List<String> themes;
    }
    
    @Schema(description = "장소 정보")
    @Getter
    @Builder
    @AllArgsConstructor
    public static class PlaceInfo {
        
        @Schema(description = "장소 ID", example = "789")
        private Long placeId;
        
        @Schema(description = "장소명", example = "JYP 사옥")
        private String name;
        
        @Schema(description = "설명", example = "JYP 엔터테인먼트의 본사 사옥입니다.")
        private String description;
        
        @Schema(description = "주소", example = "서울특별시 강동구 성내동")
        private String address;
        
        @Schema(description = "테마 목록")
        private java.util.List<String> themes;
        
        @Schema(description = "비용 정보", example = "무료 (외부 관람만 가능)")
        private String costInfo;
    }
}