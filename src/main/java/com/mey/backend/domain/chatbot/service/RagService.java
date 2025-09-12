package com.mey.backend.domain.chatbot.service;

import com.mey.backend.domain.chatbot.dto.DocumentSearchResult;
import com.mey.backend.domain.chatbot.exception.DocumentProcessingException;
import com.mey.backend.domain.chatbot.repository.InMemoryDocumentVectorStore;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {
    private final InMemoryDocumentVectorStore vectorStore;
    private final OpenAiApi openAiApi;

    /**
     * 질의와 관련된 문서를 검색합니다.
     *
     * @param question 사용자 질문
     * @param maxResults 최대 검색 결과 수
     * @return 유사도 순으로 정렬된 문서 목록
     */
    public List<DocumentSearchResult> retrieve(String question, int maxResults) {
        log.debug("검색 시작: '{}', 최대 결과 수: {}", question, maxResults);
        return vectorStore.similaritySearch(question, maxResults);
    }

    /**
     * 한류 루트 추천을 위한 답변을 생성합니다 (출처 정보 없음).
     *
     * @param question 사용자 질문
     * @param relevantDocs 이미 검색된 관련 문서
     * @return 출처 정보가 제거된 루트 추천 응답
     */
    public String generateRouteRecommendationAnswer(String question, List<DocumentSearchResult> relevantDocs) {
        return generateRouteRecommendationAnswer(question, relevantDocs, "ko"); // 기본 한국어
    }

    /**
     * 언어를 고려한 한류 루트 추천 답변을 생성합니다.
     */
    public String generateRouteRecommendationAnswer(String question, List<DocumentSearchResult> relevantDocs, String language) {
        log.debug("루트 추천 응답 생성 시작: '{}' (언어: {})", question, language);

        // 관련 문서 검색 또는 사용
        if (relevantDocs.isEmpty()) {
            log.info("관련 정보를 찾을 수 없음: '{}'", question);
            return getNoResultsMessage(language);
        }

        // 관련 문서의 내용을 컨텍스트로 결합 (번호 없이)
        String context = relevantDocs.stream()
                .map(DocumentSearchResult::getContent)
                .collect(java.util.stream.Collectors.joining("\n\n"));
        
        log.debug("컨텍스트 크기: {} 문자", context.length());

        // 언어별 루트 추천 시스템 프롬프트 생성
        String systemPromptText = String.format(getRouteRecommendationSystemPrompt(language), context);

        // LLM을 통한 응답 생성
        try {
            ChatResponse response = callOpenAi(question, systemPromptText);
            log.debug("AI 응답 생성: {}", response);
            String aiAnswer = (response != null && response.getResult() != null &&
                    response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getText()
                    : "응답을 생성할 수 없습니다.";

            return aiAnswer;
        } catch (Exception e) {
            log.error("AI 모델 호출 중 오류 발생: {}", e.getMessage(), e);
            return getSystemErrorMessage(language);
        }
    }

    /**
     * 질문에 대한 답변을 생성하며, 참고한 정보 출처도 함께 제공합니다.
     *
     * @param question 사용자 질문
     * @param relevantDocs 이미 검색된 관련 문서
     * @return 참고 출처가 포함된 응답
     */
    public String generateAnswerWithContexts(String question, List<DocumentSearchResult> relevantDocs) {
        log.debug("RAG 응답 생성 시작: '{}'", question);

        // 관련 문서 검색 또는 사용
        if (relevantDocs.isEmpty()) {
            log.info("관련 정보를 찾을 수 없음: '{}'", question);
            return "관련 정보를 찾을 수 없습니다. 다른 질문을 시도하거나 관련 문서를 업로드해 주세요.";
        }

        // 문서 번호 부여 (응답에서 출처 표시를 위해)
        List<String> numberedDocs = IntStream.range(0, relevantDocs.size())
                .mapToObj(index -> "[" + (index + 1) + "] " + relevantDocs.get(index).getContent())
                .toList();

        // 관련 문서의 내용을 컨텍스트로 결합
        String context = String.join("\n\n", numberedDocs);
        log.debug("컨텍스트 크기: {} 문자", context.length());

        // 컨텍스트를 포함하는 시스템 프롬프트 생성
        String systemPromptText = String.format("""
            당신은 지식 기반 Q&A 시스템입니다. 
            사용자의 질문에 대한 답변을 다음 정보를 바탕으로 생성해주세요.
            주어진 정보에 답이 없다면 모른다고 솔직히 말해주세요.
            답변 마지막에 사용한 정보의 출처 번호 [1], [2] 등을 반드시 포함해주세요.
            
            정보:
            %s
            """, context);

        // LLM을 통한 응답 생성
        try {
            ChatResponse response = callOpenAi(question, systemPromptText);
            log.debug("AI 응답 생성: {}", response);
            String aiAnswer = (response != null && response.getResult() != null &&
                    response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getText()
                    : "응답을 생성할 수 없습니다.";

            // 참고 문서 정보 추가
            StringBuilder sourceInfo = new StringBuilder();
            sourceInfo.append("\n\n참고 문서:");
            for (int i = 0; i < relevantDocs.size(); i++) {
                DocumentSearchResult doc = relevantDocs.get(i);
                String originalFilename = doc.getMetadata().getOrDefault("originalFilename", "Unknown file").toString();
                sourceInfo.append("\n[").append(i + 1).append("] ").append(originalFilename);
            }

            return aiAnswer + sourceInfo.toString();
        } catch (Exception e) {
            log.error("AI 모델 호출 중 오류 발생: {}", e.getMessage(), e);
            StringBuilder fallbackResponse = new StringBuilder();
            fallbackResponse.append("AI 모델 호출 중 오류가 발생했습니다. 검색 결과만 제공합니다:\n\n");

            for (DocumentSearchResult doc : relevantDocs) {
                fallbackResponse.append(doc.getContent()).append("\n\n");
            }

            return fallbackResponse.toString();
        }
    }

    private ChatResponse callOpenAi(String userInput, String systemMessage) {
        List<Message> messages = Arrays.asList(
                new SystemMessage(systemMessage),
                new UserMessage(userInput)
        );

        ChatOptions chatOptions = ChatOptions.builder()
                .model("gpt-4o-mini")
                .build();

        Prompt prompt = Prompt.builder()
                .messages(messages)
                .chatOptions(chatOptions)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .build();

        return chatModel.call(prompt);
    }
    
    /**
     * 벡터 스토어에 문서를 직접 추가합니다.
     *
     * @param id 문서 ID
     * @param content 문서 내용
     * @param metadata 메타데이터
     */
    public void addDocument(String id, String content, Map<String, Object> metadata) {
        vectorStore.addDocument(id, content, metadata);
    }
    
    /**
     * 장소 검색을 위한 특화된 검색 메서드
     *
     * @param searchQuery 검색 쿼리
     * @param maxResults 최대 결과 수
     * @return Place ID 리스트
     */
    public List<Long> searchPlaceIds(String searchQuery, int maxResults) {
        log.debug("장소 ID 검색 시작: '{}', 최대 결과: {}", searchQuery, maxResults);
        
        List<DocumentSearchResult> results = retrieve(searchQuery, maxResults);
        List<Long> placeIds = new ArrayList<>();
        
        for (DocumentSearchResult result : results) {
            if (placeIds.size() >= maxResults) {
                break;
            }
            
            Map<String, Object> metadata = result.getMetadata();
            if (metadata != null && metadata.containsKey("placeId")) {
                Object placeIdObj = metadata.get("placeId");
                if (placeIdObj instanceof Number) {
                    placeIds.add(((Number) placeIdObj).longValue());
                } else if (placeIdObj instanceof String) {
                    try {
                        placeIds.add(Long.parseLong((String) placeIdObj));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid placeId format in metadata: {}", placeIdObj);
                    }
                }
            }
        }
        
        log.info("장소 ID 검색 완료: {} 개 추출", placeIds.size());
        return placeIds;
    }

    /**
     * 실제 루트의 장소들을 기반으로 자연스러운 루트 추천 메시지를 생성합니다.
     *
     * @param question 사용자 질문
     * @param places 실제 루트에 포함된 장소들
     * @return 모든 장소를 반영한 루트 추천 응답
     */
    public String generateRouteRecommendationAnswerWithPlaces(String question, java.util.List<com.mey.backend.domain.place.entity.Place> places) {
        return generateRouteRecommendationAnswerWithPlaces(question, places, "ko"); // 기본 한국어
    }

    /**
     * 언어를 고려한 실제 루트의 장소들 기반 루트 추천 메시지를 생성합니다.
     */
    public String generateRouteRecommendationAnswerWithPlaces(String question, java.util.List<com.mey.backend.domain.place.entity.Place> places, String language) {
        log.debug("루트 추천 응답 생성 시작 (장소 기반): '{}', 장소 수: {}, 언어: {}", question, places.size(), language);

        if (places.isEmpty()) {
            log.info("장소 정보 없음: '{}'", question);
            return getNoResultsMessage(language);
        }

        // 장소 정보를 순서대로 정렬하고 언어별 컨텍스트 생성
        String context = places.stream()
                .map(place -> createPlaceContext(place, language))
                .collect(java.util.stream.Collectors.joining("\n"));
        
        log.debug("컨텍스트 크기: {} 문자, 장소 수: {}", context.length(), places.size());

        // 언어별 루트 추천 시스템 프롬프트 생성
        String systemPromptText = String.format(getPlaceBasedRouteRecommendationSystemPrompt(language), context);

        // LLM을 통한 응답 생성
        try {
            ChatResponse response = callOpenAi(question, systemPromptText);
            log.debug("AI 응답 생성: {}", response);
            String aiAnswer = (response != null && response.getResult() != null &&
                    response.getResult().getOutput() != null)
                    ? response.getResult().getOutput().getText()
                    : "루트 추천 정보를 생성할 수 없습니다.";

            return aiAnswer.trim();

        } catch (Exception e) {
            log.error("AI 응답 생성 중 오류 발생", e);
            return getSystemErrorMessage(language);
        }
    }
    
    /**
     * 언어별 장소 컨텍스트를 생성합니다.
     */
    private String createPlaceContext(com.mey.backend.domain.place.entity.Place place, String language) {
        // null 체크 및 기본값 설정
        if (language == null) {
            language = "ko";
        }
        
        return switch (language) {
            case "ko" -> String.format("""
                    장소명: %s
                    설명: %s
                    주소: %s
                    지역: %s
                    테마: %s
                    비용정보: %s
                    연락처: %s
                    """,
                    place.getNameKo(),
                    place.getDescriptionKo(),
                    place.getAddressKo(),
                    place.getRegion().getNameKo(),
                    String.join(", ", place.getThemes()),
                    place.getCostInfo(),
                    place.getContactInfo() != null ? place.getContactInfo() : "정보 없음"
            );
            case "en" -> String.format("""
                    Place Name: %s
                    Description: %s
                    Address: %s
                    Region: %s
                    Theme: %s
                    Cost Info: %s
                    Contact: %s
                    """,
                    place.getNameEn() != null ? place.getNameEn() : place.getNameKo(),
                    place.getDescriptionEn() != null ? place.getDescriptionEn() : place.getDescriptionKo(),
                    place.getAddressKo(), // Address is only available in Korean
                    place.getRegion().getNameKo(),
                    String.join(", ", place.getThemes()),
                    place.getCostInfo(),
                    place.getContactInfo() != null ? place.getContactInfo() : "No information"
            );
            case "ja" -> String.format("""
                    場所名: %s
                    説明: %s
                    住所: %s
                    地域: %s
                    テーマ: %s
                    費用情報: %s
                    連絡先: %s
                    """,
                    place.getNameEn() != null ? place.getNameEn() : place.getNameKo(), // Fallback to English
                    place.getDescriptionEn() != null ? place.getDescriptionEn() : place.getDescriptionKo(),
                    place.getAddressKo(),
                    place.getRegion().getNameKo(),
                    String.join(", ", place.getThemes()),
                    place.getCostInfo(),
                    place.getContactInfo() != null ? place.getContactInfo() : "情報なし"
            );
            case "zh" -> String.format("""
                    地点名称: %s
                    描述: %s
                    地址: %s
                    地区: %s
                    主题: %s
                    费用信息: %s
                    联系方式: %s
                    """,
                    place.getNameEn() != null ? place.getNameEn() : place.getNameKo(), // Fallback to English
                    place.getDescriptionEn() != null ? place.getDescriptionEn() : place.getDescriptionKo(),
                    place.getAddressKo(),
                    place.getRegion().getNameKo(),
                    String.join(", ", place.getThemes()),
                    place.getCostInfo(),
                    place.getContactInfo() != null ? place.getContactInfo() : "无信息"
            );
            default -> createPlaceContext(place, "ko"); // 기본값
        };
    }
    
    /**
     * 언어별 "결과 없음" 메시지를 반환합니다.
     */
    private String getNoResultsMessage(String language) {
        if (language == null) {
            language = "ko";
        }
        return switch (language) {
            case "ko" -> "죄송합니다. 요청하신 조건에 맞는 루트 정보를 찾을 수 없습니다. 다른 테마나 지역을 시도해보시겠어요?";
            case "en" -> "Sorry, I couldn't find route information matching your criteria. Would you like to try different themes or regions?";
            case "ja" -> "申し訳ございません。お客様の条件に合うルート情報が見つかりませんでした。他のテーマや地域をお試しいただけますか？";
            case "zh" -> "抱歉，找不到符合您条件的路线信息。您想尝试其他主题或地区吗？";
            default -> getNoResultsMessage("ko");
        };
    }
    
    /**
     * 언어별 시스템 오류 메시지를 반환합니다.
     */
    private String getSystemErrorMessage(String language) {
        if (language == null) {
            language = "ko";
        }
        return switch (language) {
            case "ko" -> "죄송합니다. 현재 추천 시스템에 문제가 있습니다. 잠시 후 다시 시도해주세요.";
            case "en" -> "Sorry, there's currently an issue with the recommendation system. Please try again later.";
            case "ja" -> "申し訳ございません。現在、推薦システムに問題があります。しばらくしてからもう一度お試しください。";
            case "zh" -> "抱歉，推荐系统目前有问题。请稍后再试。";
            default -> getSystemErrorMessage("ko");
        };
    }
    
    /**
     * 언어별 루트 추천 시스템 프롬프트를 반환합니다.
     */
    private String getRouteRecommendationSystemPrompt(String language) {
        if (language == null) {
            language = "ko";
        }
        return switch (language) {
            case "ko" -> """
                당신은 친근하고 전문적인 한류 여행 가이드입니다. 
                사용자의 질문에 대해 주어진 루트 정보를 바탕으로 자연스럽고 매력적인 추천을 해주세요.
                
                다음 원칙을 따라주세요:
                1. 정보 출처나 참고 번호는 절대 언급하지 마세요
                2. 자연스럽고 친근한 톤으로 응답하세요
                3. 루트의 특징과 매력을 강조하세요
                4. 구체적인 추천 이유를 포함하세요
                5. 2-3문장으로 간결하게 작성하세요
                
                루트 정보:
                %s
                """;
            case "en" -> """
                You are a friendly and professional Korean Wave travel guide.
                Please provide natural and attractive recommendations based on the given route information in response to user questions.
                
                Please follow these principles:
                1. Never mention information sources or reference numbers
                2. Respond in a natural and friendly tone
                3. Emphasize the features and attractions of the route
                4. Include specific reasons for recommendations
                5. Write concisely in 2-3 sentences
                
                Route Information:
                %s
                """;
            case "ja" -> """
                あなたは親しみやすく専門的な韓流旅行ガイドです。
                与えられたルート情報に基づいて、ユーザーの質問に対して自然で魅力的な推薦をしてください。
                
                以下の原則に従ってください：
                1. 情報源や参考番号は絶対に言及しないでください
                2. 自然で親しみやすいトーンで応答してください
                3. ルートの特徴と魅力を強調してください
                4. 具体的な推薦理由を含めてください
                5. 2-3文で簡潔に書いてください
                
                ルート情報：
                %s
                """;
            case "zh" -> """
                您是一位友善且专业的韩流旅行向导。
                请基于提供的路线信息，对用户的问题给出自然且有吸引力的推荐。
                
                请遵循以下原则：
                1. 绝不提及信息来源或参考编号
                2. 以自然友好的语调回应
                3. 强调路线的特色和魅力
                4. 包含具体的推荐理由
                5. 用2-3句话简洁地写出
                
                路线信息：
                %s
                """;
            default -> getRouteRecommendationSystemPrompt("ko");
        };
    }
    
    /**
     * 언어별 장소 기반 루트 추천 시스템 프롬프트를 반환합니다.
     */
    private String getPlaceBasedRouteRecommendationSystemPrompt(String language) {
        if (language == null) {
            language = "ko";
        }
        return switch (language) {
            case "ko" -> """
                당신은 친근하고 전문적인 한류 여행 가이드입니다.
                사용자의 질문에 대해 주어진 루트 정보를 바탕으로 자연스럽고 매력적인 추천을 해주세요.

                다음 원칙을 따라주세요:
                1. 장소들은 제공된 순서대로 방문하는 것이 최적화된 루트입니다
                2. 여행 일수에 맞게 일차별로 구성하되, 적절한 개수의 대표 장소들을 선별하여 소개해주세요
                3. 각 장소의 특징과 매력을 간략하게 설명해주세요
                4. 정보 출처나 참고 번호는 절대 언급하지 마세요
                5. 자연스럽고 친근한 톤으로 작성해주세요
                6. 2-3문단 정도의 적절한 길이로 작성해주세요

                루트 정보:
                %s
                """;
            case "en" -> """
                You are a friendly and professional Korean Wave travel guide.
                Please provide natural and attractive recommendations based on the given route information in response to user questions.

                Please follow these principles:
                1. The places should be visited in the provided order as it's an optimized route
                2. Organize by daily itinerary according to travel days, selecting appropriate representative places to introduce
                3. Briefly describe the features and attractions of each place
                4. Never mention information sources or reference numbers
                5. Write in a natural and friendly tone
                6. Write in an appropriate length of 2-3 paragraphs

                Route Information:
                %s
                """;
            case "ja" -> """
                あなたは親しみやすく専門的な韓流旅行ガイドです。
                与えられたルート情報に基づいて、ユーザーの質問に対して自然で魅力的な推薦をしてください。

                以下の原則に従ってください：
                1. 場所は提供された順序で訪問するのが最適化されたルートです
                2. 旅行日数に合わせて日別に構成し、適切な数の代表的な場所を選んで紹介してください
                3. 各場所の特徴と魅力を簡潔に説明してください
                4. 情報源や参考番号は絶対に言及しないでください
                5. 自然で親しみやすいトーンで書いてください
                6. 2-3段落程度の適切な長さで書いてください

                ルート情報：
                %s
                """;
            case "zh" -> """
                您是一位友善且专业的韩流旅行向导。
                请基于提供的路线信息，对用户的问题给出自然且有吸引力的推荐。

                请遵循以下原则：
                1. 地点应按提供的顺序参观，这是优化的路线
                2. 根据旅行天数按日安排，选择适当数量的代表性地点介绍
                3. 简要描述每个地点的特色和魅力
                4. 绝不提及信息来源或参考编号
                5. 以自然友好的语调书写
                6. 以2-3段的适当篇幅书写

                路线信息：
                %s
                """;
            default -> getPlaceBasedRouteRecommendationSystemPrompt("ko");
        };
    }

}
