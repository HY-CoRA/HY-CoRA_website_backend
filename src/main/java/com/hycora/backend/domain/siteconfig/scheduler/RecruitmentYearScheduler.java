package com.hycora.backend.domain.siteconfig.scheduler;

import com.hycora.backend.domain.siteconfig.service.SiteConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitmentYearScheduler {

    private final SiteConfigService siteConfigService;

    /**
     * 매년 2월 1일 00:05 KST 실행
     * recruitment-year +1 갱신, 정규 모집 날짜(regularStart/regularEnd) null 초기화
     * 이미 해당 연도로 설정되어 있으면 중복 실행 방지
     */
    @Scheduled(cron = "0 5 0 1 2 *", zone = "Asia/Seoul")
    public void updateRecruitmentYear() {
        int currentYear = Year.now(ZoneId.of("Asia/Seoul")).getValue();
        int nextYear = currentYear + 1;

        int savedYear = siteConfigService.getCurrentRecruitmentYear();
        if (savedYear >= nextYear) {
            log.info("[스케줄러] 모집 연도 갱신 스킵: 이미 {}년으로 설정되어 있음", savedYear);
            return;
        }

        siteConfigService.updateRecruitmentYear(nextYear);
        log.info("[스케줄러] 모집 연도 갱신 완료: {} → {} (regularStart/regularEnd 초기화)", savedYear, nextYear);
    }
}
