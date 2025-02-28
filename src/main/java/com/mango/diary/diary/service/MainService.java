package com.mango.diary.diary.service;

import com.mango.diary.auth.domain.User;
import com.mango.diary.auth.exception.MAuthErrorCode;
import com.mango.diary.auth.repository.UserRepository;
import com.mango.diary.common.enums.Emotion;
import com.mango.diary.diary.domain.TodayComment;
import com.mango.diary.diary.dto.DiaryResponse;
import com.mango.diary.diary.dto.EmotionCountDTO;
import com.mango.diary.diary.dto.MainPageResponse;
import com.mango.diary.diary.exception.DiaryErrorCode;
import com.mango.diary.diary.exception.DiaryException;
import com.mango.diary.diary.repository.DiaryRepository;
import com.mango.diary.diary.repository.TodayCommentRepository;
import com.mango.diary.statistics.entity.EmotionStatistics;
import com.mango.diary.statistics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.mango.diary.auth.exception.MAuthException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MainService {

    private final DiaryRepository diaryRepository;
    private final TodayCommentRepository todayCommentRepository;
    private final UserRepository userRepository;
    private final StatisticsRepository statisticsRepository;

    public MainPageResponse getMainPage(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new MAuthException(MAuthErrorCode.USER_NOT_FOUND));

        TodayComment todayComment = getTodayComment(user);

        DiaryResponse diaryResponse = getLatestDiaryResponse(userId);

        List<EmotionCountDTO> topEmotions = getTopThreeEmotionsForCurrentMonth(userId);

        return new MainPageResponse(todayComment.getComment(), diaryResponse, topEmotions);

    }

    private List<EmotionCountDTO> getTopThreeEmotionsForCurrentMonth(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        return statisticsRepository.findByUserIdAndYearMonth(userId, currentMonth)
                .map(this::getTopThreeEmotions)
                .orElse(null);
    }

    private DiaryResponse getLatestDiaryResponse(Long userId) {
        return diaryRepository.findFirstByUserIdOrderByDateDesc(userId)
                .map(diary -> new DiaryResponse(diary.getId(), diary.getContent(), diary.getDate(), diary.getEmotion()))
                .orElse(null);
    }

    private TodayComment getTodayComment(User user) {
        TodayComment todayComment;
        if (!todayCommentRepository.existsByDate(LocalDate.now())) {
            todayComment = new TodayComment(user);
            todayCommentRepository.save(todayComment);
        } else {
            todayComment = todayCommentRepository.findByDate(LocalDate.now()).
                    orElseThrow(() -> new DiaryException(DiaryErrorCode.TODAY_COMMENT_NOT_FOUND));
        }
        return todayComment;
    }

    private List<EmotionCountDTO> getTopThreeEmotions(EmotionStatistics statistics) {
        Map<Emotion, Long> emotions = new HashMap<>();
        emotions.put(Emotion.기쁨, statistics.get기쁨());
        emotions.put(Emotion.신남, statistics.get신남());
        emotions.put(Emotion.행복, statistics.get행복());
        emotions.put(Emotion.평온, statistics.get평온());
        emotions.put(Emotion.우울, statistics.get우울());
        emotions.put(Emotion.불안, statistics.get불안());
        emotions.put(Emotion.슬픔, statistics.get슬픔());
        emotions.put(Emotion.분노, statistics.get분노());

        return emotions.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(3)
                .map(entry -> new EmotionCountDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
