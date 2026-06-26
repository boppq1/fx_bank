-- =====================================================================
--  product_join_progress : IN_PROGRESS 행을 (user_no, product_no)당 1건으로 강제
--  대상 DB   : Oracle
--  적용 주체 : 클라이언트 DBA (애플리케이션은 이 스크립트를 실행하지 않음)
--  관련      : 상품 가입 임시저장 / 이어서 가입 기능
-- =====================================================================
--
-- [목적]
--   한 사용자(user_no)가 한 상품(product_no)에 대해 "진행 중(IN_PROGRESS)"인
--   임시저장 행을 동시에 2건 이상 갖지 못하게 막는다.
--   (더블클릭 / 멀티탭에서 동시 INSERT 경합을 DB 레벨에서 100% 차단)
--
-- [COMPLETED / EXPIRED 행은 중복 허용되는가? → 예]
--   함수 기반 "부분(partial) 유니크 인덱스"다.
--   progress_status 가 'IN_PROGRESS' 가 아니면 두 인덱스 식이 모두 NULL 이 된다.
--   Oracle B-Tree 인덱스는 "모든 인덱스 키 컬럼이 NULL 인 행"을 인덱스에 저장하지
--   않으므로, COMPLETED / EXPIRED 행은 유니크 검사 대상에서 빠진다.
--   => 같은 사용자가 같은 상품을 여러 번 가입완료(COMPLETED 다건)하거나
--      여러 번 만료(EXPIRED 다건)해도 ORA-00001 이 발생하지 않는다.
--   => 오직 IN_PROGRESS 행에 대해서만 (user_no, product_no) 유일성이 강제된다.
--
-- [적용 전/후 동작 차이 — 한 줄]
--   적용 전: 동시 요청이 각자 INSERT 해 IN_PROGRESS 가 일시적으로 2건 생길 수 있고(앱이
--            재개 조회 때 최신만 쓰고 나머지를 EXPIRED 로 정리해 방어),
--   적용 후: 동시 INSERT 중 1건만 성공·나머지는 ORA-00001 → 앱이 즉시 UPDATE 로 폴백해
--            항상 IN_PROGRESS 1건이 유지된다.
--
-- =====================================================================

-- ---------------------------------------------------------------------
-- [1단계] 선행 정리 (필수)
--   CREATE UNIQUE INDEX 는 기존에 중복 IN_PROGRESS 행이 있으면 ORA-01452 로 실패한다.
--   같은 (user_no, product_no) 의 IN_PROGRESS 중 최신(join_progress_no 최대) 1건만 남기고
--   나머지를 EXPIRED 로 정리한다. (행 삭제하지 않음 — 감사 보존)
-- ---------------------------------------------------------------------
UPDATE product_join_progress
   SET progress_status = 'EXPIRED',
       updated_dt      = SYSDATE
 WHERE progress_status = 'IN_PROGRESS'
   AND join_progress_no NOT IN (
        SELECT MAX(join_progress_no)
          FROM product_join_progress
         WHERE progress_status = 'IN_PROGRESS'
         GROUP BY user_no, product_no
   );
COMMIT;

-- ---------------------------------------------------------------------
-- [2단계] 부분 유니크 인덱스 생성
-- ---------------------------------------------------------------------
CREATE UNIQUE INDEX uix_pjp_inprogress
    ON product_join_progress (
        CASE WHEN progress_status = 'IN_PROGRESS' THEN user_no    END,
        CASE WHEN progress_status = 'IN_PROGRESS' THEN product_no END
    );

-- ---------------------------------------------------------------------
-- [검증] (선택 실행)
--   ① 적용 후 IN_PROGRESS 중복이 0건이어야 한다:
--      SELECT user_no, product_no, COUNT(*)
--        FROM product_join_progress
--       WHERE progress_status = 'IN_PROGRESS'
--       GROUP BY user_no, product_no
--      HAVING COUNT(*) > 1;
--
--   ② COMPLETED/EXPIRED 는 중복이 허용되는지(같은 user+product 다건 존재 가능) 확인.
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- [롤백]
--   DROP INDEX uix_pjp_inprogress;
-- ---------------------------------------------------------------------
