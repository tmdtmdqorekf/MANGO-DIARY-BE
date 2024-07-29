package com.mango.diary.diary.service;

import com.mango.diary.common.enums.Emotion;
import com.mango.diary.diary.dto.*;
import com.mango.diary.diary.exception.DiaryErrorCode;
import com.mango.diary.diary.exception.DiaryException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class GeminiService {

    private final RestTemplate restTemplate;

    @Value("${gemini.api.key}")
    private String GEMINI_API_KEY;

    @Value("${gemini.api.url}")
    private String GEMINI_API_URL;

    @Value("${gemini.prompt.emotion}")
    private String GEMINI_API_EMOTION_TEMPLATE;

    @Value("${gemini.prompt.advice}")
    private String GEMINI_API_ADVICE_TEMPLATE;

    public AiEmotionResponse analyzeEmotion(AiEmotionRequest aiEmotionRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String prompt = GEMINI_API_EMOTION_TEMPLATE + "\n" + "\"" + aiEmotionRequest.diaryContent() + "\"";

        GeminiRequest request = new GeminiRequest(prompt);

        String emotionsText = getGeminiResponseResponseEntity(request, headers)
                .getBody()
                .candidates().get(0)
                .content().parts()
                .get(0).text().trim();

        List<String> emotionsList = Arrays.asList(emotionsText.split(",\\s*"));

        List<Emotion> topEmotions = emotionsList.stream()
                .map(this::parseEmotion)
                .collect(Collectors.toList());

        return new AiEmotionResponse(topEmotions);
    }

    private Emotion parseEmotion(String emotionStr) {
        try {
            return Emotion.valueOf(emotionStr);
        } catch (IllegalArgumentException e) {
            throw new DiaryException(DiaryErrorCode.DIARY_ANALYSIS_FAILED);
        }
    }


    private ResponseEntity<GeminiResponse> getGeminiResponseResponseEntity(GeminiRequest request, HttpHeaders headers) {
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);

        String url = UriComponentsBuilder.fromHttpUrl(GEMINI_API_URL)
                .queryParam("key", GEMINI_API_KEY)
                .toUriString();

        ResponseEntity<GeminiResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                GeminiResponse.class);
        return response;
    }

    @Transactional
    public AiCommentResponse getAiComment(AiCommentRequest aiCommentRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        String prompt = GEMINI_API_ADVICE_TEMPLATE + "\n" +
                "일기내용 :" + aiCommentRequest.diaryContent()  +
                "감정 : " + aiCommentRequest.emotion();

        GeminiRequest request = new GeminiRequest(prompt);

        String aiComment = getGeminiResponseResponseEntity(request, headers)
                .getBody()
                .candidates().get(0)
                .content().parts()
                .get(0).text();

        return new AiCommentResponse(aiComment);
    }
}
