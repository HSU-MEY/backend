package com.mey.backend.domain.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mey.backend.domain.chatbot.exception.DocumentProcessingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

/**
 * 다양한 형식의 문서에서 텍스트를 추출하는 서비스입니다.
 * 현재는 json 파일 지원, 향후 다른 형식도 추가 가능합니다.
 */
@Service
@Slf4j
public class DocumentProcessingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JSON 파일을 단순 텍스트로 변환합니다.
     * JSON의 모든 문자열 값들을 공백으로 구분하여 하나의 텍스트로 반환합니다.
     *
     * @param jsonFile JSON 파일 객체
     * @return 추출된 텍스트
     */
    public String extractTextFromJson(File jsonFile) {
        log.debug("JSON 텍스트 추출 시작: {}", jsonFile.getName());

        try {
            JsonNode rootNode = objectMapper.readTree(jsonFile);
            log.debug("JSON 파일 파싱 성공");

            List<String> textValues = new ArrayList<>();
            extractAllTextValues(rootNode, textValues);

            String extractedText = String.join(" ", textValues);
            log.debug("JSON 텍스트 추출 완료: {} 문자, {} 개의 텍스트 값",
                    extractedText.length(), textValues.size());

            return extractedText;

        } catch (IOException e) {
            log.error("JSON 텍스트 추출 실패: {}", jsonFile.getName(), e);
            throw new DocumentProcessingException();
        }
    }

    /**
     * JsonNode에서 재귀적으로 모든 키와 값을 key: value 형태로 추출합니다.
     *
     * @param node JSON 노드
     * @param textValues 추출된 텍스트를 저장할 리스트
     */
    private void extractAllTextValues(JsonNode node, List<String> textValues) {
        if (node.isTextual()) {
            // 문자열 값 추가
            String text = node.asText().trim();
            if (!text.isEmpty()) {
                textValues.add(text);
            }
        } else if (node.isArray()) {
            // 배열인 경우 각 요소 처리
            for (JsonNode arrayElement : node) {
                extractAllTextValues(arrayElement, textValues);
            }
        } else if (node.isObject()) {
            // 객체인 경우 key: value 형태로 처리
            Iterator<Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey().trim();
                JsonNode value = field.getValue();

                if (!key.isEmpty()) {
                    if (value.isTextual()) {
                        // 문자열 값인 경우: key: value 형태
                        String textValue = value.asText().trim();
                        if (!textValue.isEmpty()) {
                            textValues.add(key + ": " + textValue);
                        }
                    } else if (value.isObject()) {
                        // 객체인 경우: key: { 로 시작
                        textValues.add(key + ": {");
                        extractAllTextValues(value, textValues);
                        textValues.add("}");
                    } else if (value.isArray()) {
                        // 배열인 경우: key: [ 로 시작
                        textValues.add(key + ": [");
                        extractAllTextValues(value, textValues);
                        textValues.add("]");
                    } else {
                        // 숫자, 불린 등: key: value 형태
                        textValues.add(key + ": " + value.asText());
                    }
                }
            }
        }
    }
}
