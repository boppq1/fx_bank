package com.example.bank.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.bank.product.dao.IProductJoinDao;
import com.example.bank.product.dao.IProductJoinProgressDao;
import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductJoinProgressDto;
import com.example.bank.product.dto.ProductJoinResumeDto;
import com.example.bank.product.dto.ProductJoinTermsRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;

/**
 * 상품 가입 임시저장(product_join_progress) 저장·복원·중복방지 단위 테스트.
 * DB/스프링 컨텍스트 없이 Mockito 로 DAO/세션을 모킹한다.
 */
class ProductJoinProgressTest {

    private IProductJoinDao productJoinDao;
    private IProductJoinProgressDao progressDao;
    private ObjectMapper objectMapper;
    private ProductJoinServiceImpl service;

    private static final Long USER_NO = 100L;
    private static final Long PRODUCT_NO = 1L;

    @BeforeEach
    void setUp() {
        productJoinDao = mock(IProductJoinDao.class);
        progressDao = mock(IProductJoinProgressDao.class);
        objectMapper = new ObjectMapper();
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        // 생성자 순서 = @RequiredArgsConstructor 필드 선언 순서
        service = new ProductJoinServiceImpl(productJoinDao, progressDao, objectMapper, passwordEncoder);
    }

    /** 백킹 맵 기반 HttpSession 모킹 */
    private HttpSession fakeSession() {
        Map<String, Object> store = new HashMap<>();
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(anyString())).thenAnswer(i -> store.get(i.getArgument(0)));
        doAnswer(i -> { store.put(i.getArgument(0), i.getArgument(1)); return null; })
                .when(session).setAttribute(anyString(), any());
        doAnswer(i -> { store.remove(i.getArgument(0)); return null; })
                .when(session).removeAttribute(anyString());
        return session;
    }

    private ProductDetailDto demandDepositProduct() {
        ProductDetailDto p = new ProductDetailDto();
        p.setProductNo(PRODUCT_NO);
        p.setProductName("외화보통예금");
        p.setProductType("외화보통예금"); // '예금' 포함 + 정기/적금 아님 → 입출금식(OCR 필수)
        return p;
    }

    // ── 1) 약관 저장 시 TERMS 체크포인트가 upsert 된다 ─────────────────────
    @Test
    void saveTerms_writesTermsCheckpoint() {
        when(productJoinDao.selectProductForJoin(PRODUCT_NO)).thenReturn(demandDepositProduct());
        when(productJoinDao.countRequiredTerms(PRODUCT_NO)).thenReturn(1);
        when(productJoinDao.countMatchedRequiredTerms(eq(PRODUCT_NO), anyList())).thenReturn(1);

        ProductJoinTermsRequestDto dto = new ProductJoinTermsRequestDto();
        dto.setProductNo(PRODUCT_NO);
        dto.setRequiredTermsCodes(List.of("10"));
        dto.setOptionalTermsCodes(List.of("20"));

        HttpSession session = fakeSession();
        service.saveTermsToSession(dto, USER_NO, session);

        ArgumentCaptor<ProductJoinProgressDto> captor = ArgumentCaptor.forClass(ProductJoinProgressDto.class);
        verify(progressDao, times(1)).upsertProgress(captor.capture());
        ProductJoinProgressDto saved = captor.getValue();

        assertEquals(USER_NO, saved.getUserNo());
        assertEquals(PRODUCT_NO, saved.getProductNo());
        assertEquals("TERMS", saved.getCurrentStep());
        assertEquals("IN_PROGRESS", saved.getProgressStatus());
        assertTrue(saved.getRequiredTermsCodes().contains("10"), "필수약관 코드가 JSON으로 직렬화돼야 함");
        assertTrue(saved.getOptionalTermsCodes().contains("20"));
        assertNotNull(session.getAttribute("PRODUCT_JOIN_TERMS"));
    }

    // ── 2) 체크포인트 저장 실패는 best-effort: 라이브 흐름을 막지 않는다 ────────
    @Test
    void checkpointFailure_doesNotBreakLiveFlow() {
        when(productJoinDao.selectProductForJoin(PRODUCT_NO)).thenReturn(demandDepositProduct());
        when(productJoinDao.countRequiredTerms(PRODUCT_NO)).thenReturn(1);
        when(productJoinDao.countMatchedRequiredTerms(eq(PRODUCT_NO), anyList())).thenReturn(1);
        when(progressDao.upsertProgress(any())).thenThrow(new RuntimeException("DB down"));

        ProductJoinTermsRequestDto dto = new ProductJoinTermsRequestDto();
        dto.setProductNo(PRODUCT_NO);
        dto.setRequiredTermsCodes(List.of("10"));
        dto.setOptionalTermsCodes(List.of());

        HttpSession session = fakeSession();
        assertDoesNotThrow(() -> service.saveTermsToSession(dto, USER_NO, session));
        // 미러 저장이 실패해도 세션(활성 작업본)에는 저장돼 있어야 함
        assertNotNull(session.getAttribute("PRODUCT_JOIN_TERMS"));
    }

    // ── 2-b) 동시 INSERT 충돌(ORA-00001) → UPDATE 폴백 ────────────────────
    @Test
    void checkpoint_fallsBackToUpdate_onUniqueConflict() {
        when(productJoinDao.selectProductForJoin(PRODUCT_NO)).thenReturn(demandDepositProduct());
        when(productJoinDao.countRequiredTerms(PRODUCT_NO)).thenReturn(1);
        when(productJoinDao.countMatchedRequiredTerms(eq(PRODUCT_NO), anyList())).thenReturn(1);
        // 부분 유니크 인덱스 환경에서 동시 INSERT 충돌
        when(progressDao.upsertProgress(any())).thenThrow(new DuplicateKeyException("ORA-00001 unique constraint"));

        ProductJoinTermsRequestDto dto = new ProductJoinTermsRequestDto();
        dto.setProductNo(PRODUCT_NO);
        dto.setRequiredTermsCodes(List.of("10"));
        dto.setOptionalTermsCodes(List.of("20"));

        HttpSession session = fakeSession();
        assertDoesNotThrow(() -> service.saveTermsToSession(dto, USER_NO, session));

        // 충돌 시 UPDATE 폴백이 같은 스냅샷으로 호출돼야 함
        ArgumentCaptor<ProductJoinProgressDto> captor = ArgumentCaptor.forClass(ProductJoinProgressDto.class);
        verify(progressDao).updateInProgress(captor.capture());
        assertEquals("TERMS", captor.getValue().getCurrentStep());
        assertEquals(USER_NO, captor.getValue().getUserNo());
        assertEquals(PRODUCT_NO, captor.getValue().getProductNo());
        // 세션(활성 작업본)은 정상 저장
        assertNotNull(session.getAttribute("PRODUCT_JOIN_TERMS"));
    }

    // ── 3) 이어서: 약관 복원 + 무효 인증 미복원 + 중복정리 + VERIFY 라우팅 ──────
    @Test
    void resume_restoresTerms_skipsInvalidVerification() {
        ProductJoinProgressDto progress = new ProductJoinProgressDto();
        progress.setJoinProgressNo(1L);
        progress.setUserNo(USER_NO);
        progress.setProductNo(PRODUCT_NO);
        progress.setVerificationNo(5L);
        progress.setCurrentStep("VERIFY");
        progress.setProgressStatus("IN_PROGRESS");
        progress.setRequiredTermsCodes("[\"10\"]");
        progress.setOptionalTermsCodes("[\"20\"]");
        progress.setCreatedDt(new Date());
        progress.setUpdatedDt(new Date());

        when(progressDao.selectLatestInProgress(USER_NO, PRODUCT_NO)).thenReturn(progress);
        when(productJoinDao.selectProductForJoin(PRODUCT_NO)).thenReturn(demandDepositProduct());
        // 저장된 OCR 인증은 만료/무효
        when(productJoinDao.countValidVerification(5L, USER_NO, PRODUCT_NO)).thenReturn(0);

        HttpSession session = fakeSession();
        ProductJoinResumeDto info = service.resumeIntoSession(USER_NO, PRODUCT_NO, session);

        // 약관은 세션에 복원
        ProductJoinTermsRequestDto restored =
                (ProductJoinTermsRequestDto) session.getAttribute("PRODUCT_JOIN_TERMS");
        assertNotNull(restored);
        assertEquals(List.of("10"), restored.getRequiredTermsCodes());
        assertEquals(List.of("20"), restored.getOptionalTermsCodes());

        // 무효 인증은 세션에 복원하지 않음(재인증 유도)
        assertNull(session.getAttribute("PRODUCT_JOIN_VERIFICATION_NO"));
        // 민감/시간민감 항목은 복원하지 않음
        assertNull(session.getAttribute("PRODUCT_JOIN_FORM"));
        assertNull(session.getAttribute("PRODUCT_JOIN_COUPON"));

        // D 라우팅: 인증 필요 + 무효 → VERIFY 우선
        assertTrue(info.isAvailable());
        assertEquals("VERIFY", info.getRouteStep());
        assertTrue(info.isIdentityRequired());
        assertFalse(info.isIdentityVerified());

        // C 중복정리: 최신(keep=1L) 외 IN_PROGRESS 만료 호출
        verify(progressDao).expireOtherInProgress(USER_NO, PRODUCT_NO, 1L);
    }

    // ── 4) 진행행 없으면 재개 불가 ─────────────────────────────────────
    @Test
    void resumeInfo_notAvailable_whenNoRow() {
        when(progressDao.selectLatestInProgress(USER_NO, PRODUCT_NO)).thenReturn(null);
        ProductJoinResumeDto info = service.getResumeInfo(USER_NO, PRODUCT_NO);
        assertFalse(info.isAvailable());
    }

    // ── 5) 상품 판매중지(B): 재개 차단 + 진행행 만료 ───────────────────────
    @Test
    void resumeInfo_blocksAndExpires_whenProductUnsellable() {
        ProductJoinProgressDto progress = new ProductJoinProgressDto();
        progress.setJoinProgressNo(1L);
        progress.setUserNo(USER_NO);
        progress.setProductNo(PRODUCT_NO);
        progress.setCurrentStep("TERMS");
        progress.setProgressStatus("IN_PROGRESS");
        when(progressDao.selectLatestInProgress(USER_NO, PRODUCT_NO)).thenReturn(progress);
        // 상품이 더 이상 판매되지 않음(active!='Y')
        when(productJoinDao.selectProductForJoin(PRODUCT_NO)).thenReturn(null);

        ProductJoinResumeDto info = service.getResumeInfo(USER_NO, PRODUCT_NO);

        assertFalse(info.isAvailable());
        verify(progressDao).expireAllInProgress(USER_NO, PRODUCT_NO);
    }
}
