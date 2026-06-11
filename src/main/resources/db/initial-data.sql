-- HY-CoRA 초기 데이터 마이그레이션 스크립트
-- 실행 방법: docker exec -i hycora-mysql mysql -u hycora -phycora1234 hycora < src/main/resources/db/initial-data.sql

-- 관리자 계정 초기 데이터
INSERT INTO admin (email, created_at, updated_at)
VALUES ('admin@hycora.co.kr', CURDATE(), CURDATE())
ON DUPLICATE KEY UPDATE updated_at = CURDATE();

-- 사이트 설정 초기 데이터
SET @admin_id = (SELECT admin_id FROM admin WHERE email = 'admin@hycora.co.kr');

INSERT INTO site_config (`key`, value, created_at, updated_at, admin_id)
VALUES
  ('main-banner', '{"imageUrl": "", "altText": "HY-CoRA 메인 배너"}', CURDATE(), CURDATE(), @admin_id),
  ('about-banner', '{"imageUrl": "", "altText": "HY-CoRA 소개 배너"}', CURDATE(), CURDATE(), @admin_id),
  ('apply-links', '{"newMember": {"url": "", "label": "신규 지원"}, "returning": {"url": "", "label": "재가입"}}', CURDATE(), CURDATE(), @admin_id)
ON DUPLICATE KEY UPDATE updated_at = CURDATE();
