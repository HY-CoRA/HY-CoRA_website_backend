# HY-CoRA 백엔드 프로젝트

## 기본 정보
- **레포**: https://github.com/HY-CoRA/HY-CoRA_websit_backend
- **스택**: Spring Boot 3.5 + MySQL 8.0 + Docker + Java 21 + Gradle
- **로컬 실행**: `./gradlew bootRun`
- **Swagger**: http://localhost:8080/swagger-ui/index.html

## 브랜치 구조 (계단식)
```
main
└── feat/SERVER-01-backend-setup
    └── feat/SERVER-02-db-schema
        └── feat/BE-01-admin-auth
```
- 브랜치 수정 시 하위 브랜치 rebase 필요 (force push 필요)
- 새 이슈 작업 시 현재 최하단 브랜치에서 새 브랜치 생성

## PR 현황
| PR | 브랜치 | 이슈 | 상태 |
|----|--------|------|------|
| #13 | feat/SERVER-01-backend-setup | #9 서버 환경 구성 | 리뷰 반영 완료 |
| #14 | feat/SERVER-02-db-schema | #10 DB 스키마 | 리뷰 반영 완료 |
| #15 | feat/BE-01-admin-auth | #1 관리자 인증 | 진행 중 |

## 구현된 API
- GET /api/health → {"status": "ok"}
- POST /api/auth/magic-link/request — 이메일로 1회용 링크 발송
- POST /api/auth/magic-link/verify — 토큰 검증 후 JWT 발급
- POST /api/auth/webauthn/login/options|verify — Passkey 로그인
- POST /api/auth/webauthn/register/options|verify — Passkey 등록
- GET /api/auth/me — 현재 관리자 정보 (JWT 필요)
- POST /api/auth/logout

## DB 테이블
admin, site_config, past_events, activities, announcements

## Docker
- MySQL 컨테이너: hycora-mysql (포트 3306)
- DB명: hycora / 계정: hycora / 비밀번호: hycora1234
- 실행: `docker compose up -d`

## 환경변수 (.env)
```
JWT_SECRET=
CORS_ORIGIN=https://hycora.co.kr
MAIL_HOST=smtp.gmail.com
MAIL_USERNAME=
MAIL_PASSWORD=
ADMIN_INITIAL_EMAIL=  ← 최초 관리자 이메일 (서버 첫 실행 시 자동 생성)
APP_BASE_URL=https://api.hycora.co.kr
```

## 인증 방식
- Magic Link (이메일 1회용 링크) + Passkey (WebAuthn)
- 비밀번호 없음
- JWT Stateless
- Rate Limiting: 5회/분 (IP 기준)

## 주요 규칙
- 커밋 메시지에 Co-Authored-By Claude 줄 붙이지 않기
- API 명세서 기준으로 구현 (대화에서 공유된 명세서 참고)
- 이슈 작업 시 브랜치명: feat/이슈번호-설명

## BE-01 남은 작업
- 메일 발송 실패 시 에러 반환 처리
- WebAuthn → 501 Not Implemented로 변경 검토

## 임시 데이터 (배포 전 삭제)
- test@hycora.co.kr — DB에 있는 테스트 계정
