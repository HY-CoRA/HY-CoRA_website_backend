package com.hycora.backend.domain.activity.scheduler;

import com.hycora.backend.domain.activity.entity.Activity;
import com.hycora.backend.domain.activity.entity.StatusLabel;
import com.hycora.backend.domain.activity.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusScheduler {

    private final ActivityRepository activityRepository;

    /**
     * 스케줄러 1: recruiting → ongoing
     * 매일 00:10 KST 실행
     * 조건: status = 'recruiting' AND recruit_end < 오늘 KST (recruitEnd가 null이면 제외)
     */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void transitionRecruitingToOngoing() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<Activity> targets = activityRepository.findRecruitingExpired(today);

        if (targets.isEmpty()) {
            log.info("[스케줄러] recruiting→ongoing: 전환 대상 없음 (기준일: {})", today);
            return;
        }

        for (Activity activity : targets) {
            activity.updateStatus("ongoing", StatusLabel.from("ongoing"));
        }

        log.info("[스케줄러] recruiting→ongoing: {}건 전환 완료 (기준일: {})", targets.size(), today);
        targets.forEach(a -> log.info("  - [{}] {}", a.getId(), a.getTitle()));
    }

    /**
     * 스케줄러 2: ongoing → completed
     * 매년 1월 1일, 7월 1일 00:10 KST 실행
     * 조건: status = 'ongoing' 전체
     */
    @Scheduled(cron = "0 10 0 1 1,7 *", zone = "Asia/Seoul")
    @Transactional
    public void transitionOngoingToCompleted() {
        List<Activity> targets = activityRepository.findAllByStatus("ongoing");

        if (targets.isEmpty()) {
            log.info("[스케줄러] ongoing→completed: 전환 대상 없음");
            return;
        }

        for (Activity activity : targets) {
            activity.updateStatus("completed", StatusLabel.from("completed"));
        }

        log.info("[스케줄러] ongoing→completed: {}건 전환 완료", targets.size());
        targets.forEach(a -> log.info("  - [{}] {}", a.getId(), a.getTitle()));
    }

    /**
     * 두 스케줄러 모두 즉시 실행 (수동 트리거용)
     */
    @Transactional
    public void runAll() {
        transitionRecruitingToOngoing();
        transitionOngoingToCompleted();
    }
}
