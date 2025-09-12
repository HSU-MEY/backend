package com.mey.backend.domain.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 다국어 지원을 위한 언어 처리 서비스
 * 
 * 주요 책임:
 * - 지원 언어 검증 및 fallback 처리
 * - 언어별 데이터 소스 매핑
 * - 언어 설정 관리
 */
@Slf4j
@Service
public class LanguageService {
    
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("ko", "en", "ja", "zh");
    private static final String DEFAULT_FALLBACK_LANGUAGE = "en";
    
    @Value("${chatbot.language.fallback:en}")
    private String fallbackLanguage;
    
    /**
     * 언어 코드를 검증하고 지원되지 않는 경우 fallback 언어를 반환합니다.
     */
    public String validateAndGetLanguage(String requestedLanguage) {
        if (requestedLanguage == null || requestedLanguage.trim().isEmpty()) {
            return "ko"; // 기본 언어
        }
        
        String normalizedLanguage = requestedLanguage.toLowerCase().trim();
        
        if (SUPPORTED_LANGUAGES.contains(normalizedLanguage)) {
            return normalizedLanguage;
        }
        
        log.info("지원되지 않는 언어 '{}' 요청, fallback 언어 '{}'로 처리", requestedLanguage, fallbackLanguage);
        return fallbackLanguage;
    }
    
    /**
     * Place 데이터에서 해당 언어의 이름을 가져옵니다.
     */
    public String getPlaceName(com.mey.backend.domain.place.entity.Place place, String language) {
        return switch (language) {
            case "ko" -> place.getNameKo();
            case "en" -> place.getNameEn() != null ? place.getNameEn() : place.getNameKo();
            case "ja", "zh" -> {
                // 일본어/중국어는 아직 지원되지 않으므로 영어로 fallback
                String englishName = place.getNameEn();
                yield englishName != null ? englishName : place.getNameKo();
            }
            default -> place.getNameKo();
        };
    }
    
    /**
     * Place 데이터에서 해당 언어의 설명을 가져옵니다.
     */
    public String getPlaceDescription(com.mey.backend.domain.place.entity.Place place, String language) {
        return switch (language) {
            case "ko" -> place.getDescriptionKo();
            case "en" -> place.getDescriptionEn() != null ? place.getDescriptionEn() : place.getDescriptionKo();
            case "ja", "zh" -> {
                // 일본어/중국어는 아직 지원되지 않으므로 영어로 fallback
                String englishDesc = place.getDescriptionEn();
                yield englishDesc != null ? englishDesc : place.getDescriptionKo();
            }
            default -> place.getDescriptionKo();
        };
    }
    
    /**
     * Place 데이터에서 해당 언어의 주소를 가져옵니다.
     * 현재는 한국어 주소만 지원하므로 모든 언어에 대해 한국어 주소를 반환
     */
    public String getPlaceAddress(com.mey.backend.domain.place.entity.Place place, String language) {
        return place.getAddressKo(); // 현재는 한국어 주소만 지원
    }
    
    /**
     * Route 데이터에서 해당 언어의 제목을 가져옵니다.
     */
    public String getRouteTitle(com.mey.backend.domain.route.entity.Route route, String language) {
        return switch (language) {
            case "ko" -> route.getTitleKo();
            case "en" -> route.getTitleEn() != null ? route.getTitleEn() : route.getTitleKo();
            case "ja", "zh" -> {
                // 일본어/중국어는 아직 지원되지 않으므로 영어로 fallback
                String englishTitle = route.getTitleEn();
                yield englishTitle != null ? englishTitle : route.getTitleKo();
            }
            default -> route.getTitleKo();
        };
    }
    
    /**
     * Route 데이터에서 해당 언어의 설명을 가져옵니다.
     */
    public String getRouteDescription(com.mey.backend.domain.route.entity.Route route, String language) {
        return switch (language) {
            case "ko" -> route.getDescriptionKo();
            case "en" -> route.getDescriptionEn() != null ? route.getDescriptionEn() : route.getDescriptionKo();
            case "ja", "zh" -> {
                // 일본어/중국어는 아직 지원되지 않으므로 영어로 fallback
                String englishDesc = route.getDescriptionEn();
                yield englishDesc != null ? englishDesc : route.getDescriptionKo();
            }
            default -> route.getDescriptionKo();
        };
    }
    
    /**
     * 지원되는 언어인지 확인합니다.
     */
    public boolean isLanguageSupported(String language) {
        return SUPPORTED_LANGUAGES.contains(language);
    }
    
    /**
     * 데이터베이스에서 완전히 지원되는 언어인지 확인합니다.
     * (현재는 한국어와 영어만 완전 지원)
     */
    public boolean isLanguageFullySupported(String language) {
        return "ko".equals(language) || "en".equals(language);
    }
}