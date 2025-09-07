package com.mey.backend.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 대화 상태를 정의하는 열거형
 * 
 * 상태별 의미:
 * - INITIAL: 새로운 대화 시작 상태
 * - AWAITING_THEME: 테마 정보 입력 대기 중
 * - AWAITING_REGION: 지역 정보 입력 대기 중  
 * - AWAITING_DAYS: 여행 일수 입력 대기 중
 * - READY_FOR_ROUTE: 모든 정보 수집 완료, 루트 생성 준비
 * 
 * 이 열거형은 단계적 정보 수집을 위한 상태 머신의 핵심이며,
 * 의도 분류 오류를 방지하고 정확한 대화 흐름을 보장합니다.
 */
@Getter
@AllArgsConstructor
public enum ConversationState {
    INITIAL("INITIAL"),
    AWAITING_THEME("AWAITING_THEME"),
    AWAITING_REGION("AWAITING_REGION"),
    AWAITING_DAYS("AWAITING_DAYS"),
    READY_FOR_ROUTE("READY_FOR_ROUTE");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ConversationState fromValue(String value) {
        for (ConversationState state : ConversationState.values()) {
            if (state.value.equals(value) || state.name().equals(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid conversation state value: " + value);
    }
}