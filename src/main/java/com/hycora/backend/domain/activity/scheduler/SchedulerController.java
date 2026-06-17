package com.hycora.backend.domain.activity.scheduler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "Scheduler", description = "스케줄러 수동 트리거 API")
@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final ActivityStatusScheduler activityStatusScheduler;

    @Operation(
        summary = "활동 상태 자동 전환 수동 실행",
        description = "recruiting→ongoing, ongoing→completed 전환을 즉시 실행합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/run-status-transition")
    public ResponseEntity<Map<String, Boolean>> runStatusTransition() {
        activityStatusScheduler.runAll();
        return ResponseEntity.ok(Map.of("success", true));
    }
}
