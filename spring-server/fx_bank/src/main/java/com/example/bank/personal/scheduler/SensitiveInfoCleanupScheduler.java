package com.example.bank.personal.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.bank.personal.dao.IUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [개인정보 보관 만료 청소 배치]
 * 레이지 만료 체크(로그인 시점)는 "접속한" 사용자만 정리하므로,
 * 로그인하지 않고 방치된 계정의 만료 민감정보는 이 배치가 주기적으로 일괄 삭제한다.
 *
 * @EnableScheduling 은 {@code com.example.bank.analysis.SchedulerConfig} 에 이미 선언되어 있다.
 *
 * 멀티 인스턴스 환경에서 중복 실행이 우려되면 ShedLock(@SchedulerLock) 적용을 권장한다.
 *   예) implementation 'net.javacrumbs.shedlock:shedlock-spring' + Redis/JDBC LockProvider
 *       @SchedulerLock(name = "cleanupExpiredSensitiveInfos")
 *   우선은 기본 @Scheduled 로 구현한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveInfoCleanupScheduler {

    private final IUser iUser;

    /**
     * 보관기한(retention_until_dt) 지난 민감정보 일괄 삭제.
     * 주기는 application.properties 의 app.sensitive.cleanup-cron 으로 조정 (기본: 매시간 정각).
     */
    @Scheduled(cron = "${app.sensitive.cleanup-cron:0 0 * * * *}")
    public void cleanupExpiredSensitiveInfos() {
        int deleted = iUser.deleteExpiredSensitiveInfos();
        if (deleted > 0) {
            log.info("[민감정보 청소] 보관기한 만료 {}건 삭제 완료", deleted);
        }
    }
}
