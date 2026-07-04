-- ============================================================
-- V12: 테스트 중 생성된 더미(한글 깨짐 '???') 과정 정리 + 실제 과정명/레슨 시드
-- ============================================================
-- FORCE RLS가 켜져 있어 소유자(lms_owner)인 이 마이그레이션도 RLS 대상이다.
-- 각 테넌트 행을 다루기 전에 SET LOCAL로 세션 테넌트를 지정한다(V3와 동일 패턴).
-- 깨끗한 새 DB에서는 더미가 없어 DELETE가 no-op, 시드만 적용된다.

-- 샘플 영상은 Google 공개 버킷(gtv-videos-bucket/sample)을 사용한다.

-- ============================================================
-- 테넌트 A (acme)
-- ============================================================
SET LOCAL app.current_tenant = '11111111-1111-1111-1111-111111111111';

-- 1) 깨진 더미 과정 제거 (CASCADE로 레슨/수강/퀴즈/진도/수료증 함께 삭제)
DELETE FROM course WHERE title LIKE '%?%' OR title = 'enrtest';

-- 2) 기존 실제 과정 분류/설명 보정
UPDATE course SET category_code='BACKEND', level=0,
    description='Spring Boot 3로 배우는 백엔드 첫걸음'              WHERE title='Spring 입문';
UPDATE course SET category_code='BACKEND', level=1,
    description='Spring Data JPA와 Hibernate로 데이터 다루기'       WHERE title='JPA 기초';
UPDATE course SET category_code='DATA', level=2,
    description='Claude로 학습 추천·콘텐츠 분석을 붙이는 실습'      WHERE title='AI 에이전트를 활용한 LMS 구축';

-- 3) 신규 실제 과정 추가
INSERT INTO course (tenant_id, title, description, category_code, level) VALUES
('11111111-1111-1111-1111-111111111111', 'React로 시작하는 프론트엔드', '컴포넌트·상태·훅으로 만드는 모던 웹 UI', 'FRONTEND', 0),
('11111111-1111-1111-1111-111111111111', '실전 SQL과 데이터 모델링', '조인·인덱스·정규화로 탄탄한 스키마 설계', 'DATA', 1),
('11111111-1111-1111-1111-111111111111', 'Docker로 배우는 컨테이너 기초', '이미지·컨테이너·컴포즈로 개발 환경 표준화', 'DEVOPS', 1);

-- 4) 과정별 레슨 시드 (제목/내용/영상)
INSERT INTO lesson (tenant_id, course_id, title, content, video_url, order_no)
SELECT '11111111-1111-1111-1111-111111111111', c.id, v.title, v.content, v.url, v.ord
FROM course c, (VALUES
    (1, '1강. 오리엔테이션과 개발 환경', 'JDK·IDE 설정과 첫 프로젝트 생성', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4'),
    (2, '2강. 스프링 부트 시작하기', '자동 구성과 내장 톰캣으로 띄우는 첫 서버', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4'),
    (3, '3강. REST 컨트롤러 만들기', 'GET/POST 매핑과 JSON 응답', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4'),
    (4, '4강. 의존성 주입과 빈', '@Component·@Service와 생성자 주입', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4')
) AS v(ord, title, content, url)
WHERE c.title = 'Spring 입문';

INSERT INTO lesson (tenant_id, course_id, title, content, video_url, order_no)
SELECT '11111111-1111-1111-1111-111111111111', c.id, v.title, v.content, v.url, v.ord
FROM course c, (VALUES
    (1, '1강. 엔티티와 매핑', '@Entity·@Id와 기본 컬럼 매핑', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
    (2, '2강. 연관관계 매핑', '일대다·다대일과 지연 로딩', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
    (3, '3강. 리포지토리와 쿼리 메서드', 'JpaRepository와 파생 쿼리', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4')
) AS v(ord, title, content, url)
WHERE c.title = 'JPA 기초';

INSERT INTO lesson (tenant_id, course_id, title, content, video_url, order_no)
SELECT '11111111-1111-1111-1111-111111111111', c.id, v.title, v.content, v.url, v.ord
FROM course c, (VALUES
    (1, '1강. LLM과 에이전트 개요', '프롬프트·툴 호출·컨텍스트 이해', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
    (2, '2강. 콘텐츠 분석 파이프라인', '태그·난이도·요약 추출 설계', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
    (3, '3강. 맞춤 추천 붙이기', '관심분야·행동신호 기반 추천', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4')
) AS v(ord, title, content, url)
WHERE c.title = 'AI 에이전트를 활용한 LMS 구축';

INSERT INTO lesson (tenant_id, course_id, title, content, video_url, order_no)
SELECT '11111111-1111-1111-1111-111111111111', c.id, v.title, v.content, v.url, v.ord
FROM course c, (VALUES
    (1, '1강. React 첫 화면', 'JSX와 컴포넌트 기본', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4'),
    (2, '2강. 상태와 이벤트', 'useState로 다루는 UI 상태', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4'),
    (3, '3강. 효과와 데이터 패칭', 'useEffect로 API 연동', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4')
) AS v(ord, title, content, url)
WHERE c.title = 'React로 시작하는 프론트엔드';

INSERT INTO lesson (tenant_id, course_id, title, content, video_url, order_no)
SELECT '11111111-1111-1111-1111-111111111111', c.id, v.title, v.content, v.url, v.ord
FROM course c, (VALUES
    (1, '1강. SELECT와 필터링', 'WHERE·ORDER BY·집계 함수', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
    (2, '2강. 조인 마스터하기', 'INNER/LEFT 조인과 서브쿼리', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
    (3, '3강. 인덱스와 성능', '인덱스 설계와 실행계획 읽기', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4'),
    (4, '4강. 정규화와 모델링', '1~3정규형과 실전 트레이드오프', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4')
) AS v(ord, title, content, url)
WHERE c.title = '실전 SQL과 데이터 모델링';

INSERT INTO lesson (tenant_id, course_id, title, content, video_url, order_no)
SELECT '11111111-1111-1111-1111-111111111111', c.id, v.title, v.content, v.url, v.ord
FROM course c, (VALUES
    (1, '1강. 컨테이너란?', '가상머신과의 차이와 이미지 개념', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4'),
    (2, '2강. 이미지 빌드와 실행', 'Dockerfile과 docker run', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4'),
    (3, '3강. 컴포즈로 멀티 컨테이너', 'docker compose로 DB까지 한 번에', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4')
) AS v(ord, title, content, url)
WHERE c.title = 'Docker로 배우는 컨테이너 기초';

-- ============================================================
-- 테넌트 B (globex)
-- ============================================================
SET LOCAL app.current_tenant = '22222222-2222-2222-2222-222222222222';

UPDATE course SET category_code='BACKEND', level=0,
    description='기초 문법부터 함수·자료구조까지'                  WHERE title='Python 입문';

INSERT INTO course (tenant_id, title, description, category_code, level) VALUES
('22222222-2222-2222-2222-222222222222', 'Pandas 데이터 분석 입문', '데이터프레임으로 정제·집계·시각화', 'DATA', 1);

INSERT INTO lesson (tenant_id, course_id, title, content, video_url, order_no)
SELECT '22222222-2222-2222-2222-222222222222', c.id, v.title, v.content, v.url, v.ord
FROM course c, (VALUES
    (1, '1강. 파이썬 설치와 첫 코드', 'print와 변수, 자료형', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4'),
    (2, '2강. 조건문과 반복문', 'if/for/while로 흐름 제어', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4'),
    (3, '3강. 함수와 모듈', '함수 정의와 표준 라이브러리', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4')
) AS v(ord, title, content, url)
WHERE c.title = 'Python 입문';

INSERT INTO lesson (tenant_id, course_id, title, content, video_url, order_no)
SELECT '22222222-2222-2222-2222-222222222222', c.id, v.title, v.content, v.url, v.ord
FROM course c, (VALUES
    (1, '1강. 데이터프레임 기초', 'Series·DataFrame 생성과 인덱싱', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4'),
    (2, '2강. 정제와 결측치 처리', 'dropna·fillna와 타입 변환', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4'),
    (3, '3강. 그룹 집계와 피벗', 'groupby·pivot_table로 요약', 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4')
) AS v(ord, title, content, url)
WHERE c.title = 'Pandas 데이터 분석 입문';
