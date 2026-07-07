## BNK 외환 상품 관리 및 AI 맞춤 추천 서비스

https://klsbank.store/ 

BNK 부산은행 외환 서비스를 모티브로 제작한 은행 상품 가입 및 고객 상품 안내 웹 서비스입니다.
사용자에게는 외환 상품 조회, 금융 MBTI 기반 맞춤 추천, 우대금리 자동 판별, 상품 가입, AI 챗봇을 제공하고, 관리자에게는 상품 등록, 약관 관리 기능을 제공합니다.

## 목차

1. 프로젝트 소개
2. 팀 구성 및 담당 기능
3. 주요 기능
4. 기술 스택
5. 시스템 아키텍처
6. ERD 요약
7. 프로젝트 구조
8. 핵심 기능 상세
9. 실행 방법
10. 개선 사항
11. 회고

## 프로젝트 소개

최근 디지털 금융 서비스 이용이 증가하면서 사용자는 은행 업무를 쉽고 편리하게 이용할 수 있는 환경을 요구하고 있습니다.
하지만 외환 서비스는 금융 용어와 상품 종류가 다양하여 사용자가 자신에게 맞는 상품을 빠르게 찾기 어렵다는 문제가 있습니다.

본 프로젝트는 사용자 페이지, 관리자 페이지, AI 추천, AI 챗봇, 보안 시스템을 포함한 외환 상품 가입 및 관리자 서비스를 구현하는 것을 목표로 했습니다.

### 개발 기간

| 구분 | 기간 |
|---|---|
| 기획 | `[6/2 ~ 6/12]` |
| 설계 | `[6/8 ~ 6/12]` |
| 구현 | `[6/12 ~ 6/30]` |
| 발표 및 마무리 | `[7/2]` |
| 총 개발 기간 | `[약 4주]` |

---

## 팀 구성 및 담당 기능

| 훈련생 | 역할 | 담당 업무 |
|---|---|---|
| 이유림 | 팀장 | 객체탐지, 서버 배포 |
| 김건엽 | 팀원 | 보안 설정(Security), 다국어 챗봇 |
| **김다현** | **팀원** | **상품 감성 분석, 개인화 추천 시스템 (금융 MBTI)** |
| 신정훈 | 팀원 | 환율, 환전 / 상품 가입 |
| 이민주 | 팀원 | 서버 배포 / 상품 가입 |

---

## 주요 기능

### 사용자 기능

| 구분 | 기능 | 설명 |
|---|---|---|
| 상품 조회 | 외화 상품 목록/상세 | 금리, 가입 기간, 가입 금액, 약관 PDF 확인 |
| AI 추천 | 금융 MBTI 기반 맞춤 추천 | 행동 로그 분석 → MBTI 산출 → 성향 매칭 상품 추천 |
| 우대금리 판별 | 통장/신분증 이미지 인식 | YOLO 객체 탐지 + OCR로 조건 자동 검증 |
| 외환 챗봇 | 외환 정보 안내 | 외환 용어·상품 문서 기반 RAG 답변 생성 |
| 인증 | 회원가입/로그인 | JWT + Redis 세션, HttpOnly 쿠키 |

### 관리자 기능

| 구분 | 기능 | 설명 |
|---|---|---|
| 상품 관리 | 상품 등록/수정/조회 | 금리, 통화, 대상, 약관 PDF 등록 |
| 약관 관리 | 약관 버전 관리 | 상품×약관유형 슬롯 + 버전 이력 |
| 결재 관리 | 상품 승인/반려 | 금리 담당 → 약관 담당 → 상품 기획 → 최종 승인 4단계 워크플로우 |
| 이벤트 관리 | 공지/이벤트 등록 | 이벤트, 쿠폰 관리 |

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Java 21, Spring Boot 3.3.4, Spring Security, MyBatis, Thymeleaf |
| Database | Oracle Database (OCI), Redis |
| AI / 추천 | FastAPI, ChromaDB, OpenAI API(GPT, `text-embedding-3-small`), APScheduler |
| 객체 탐지 / OCR | Ultralytics YOLO, OpenCV, Naver CLOVA OCR |
| 챗봇 | LangChain, ChromaDB |
| 인증 | JWT(jjwt), HttpOnly Cookie, Redis 블랙리스트 |
| Mobile | Flutter |
| 문서 처리 | Apache PDFBox, Tabula |
| 인프라 | Docker, Docker Compose, Nginx |
| Build Tool | Gradle |

---

## 시스템 아키텍처

```
                          ┌─────────────┐
                          │   Flutter   │  (bnk_app - 모바일 클라이언트)
                          └──────┬──────┘
                                 │ HTTPS
                          ┌──────▼──────┐
                          │    Nginx    │  (Reverse Proxy / SSL)
                          └──────┬──────┘
             ┌───────────┬───────┼───────────┬────────────┐
             ▼           ▼       ▼           ▼            ▼
      ┌─────────────┐ ┌────────────┐ ┌──────────────┐ ┌─────────────┐
      │ Spring Boot │ │fastapi-agent│ │ fastapi-bnk  │ │ fastapi-ocr │
      │  (Main API) │ │(AI 추천/분석)│ │ (통장 탐지)  │ │(신분증 OCR) │
      └──────┬──────┘ └─────┬──────┘ └──────────────┘ └─────────────┘
             │              │
      ┌──────▼──────┐ ┌─────▼──────┐        ┌──────────────┐
      │  Oracle DB  │ │  ChromaDB  │        │ fastapi-chat │
      │    Redis    │ │ (벡터 검색) │        │ (외환 RAG 챗봇)│
      └─────────────┘ └────────────┘        └──────────────┘
```

---

## ERD 요약

### 주요 테이블

| 테이블 | 설명 |
|---|---|
| users | 회원 정보 |
| user_sensitive_infos | 마스킹된 주민번호 등 민감정보 |
| admins | 관리자 정보 및 권한 |
| products | 상품 기본 정보 |
| product_currencies | 상품별 지원 통화 |
| product_rates | 상품별 금리 정보 |
| product_preferential_rates | 우대 금리 조건 |
| product_reviews | 상품 리뷰 (감성 분석 대상) |
| product_subscriptions | 상품 가입 정보 |
| product_join_progress | 상품 가입 진행 상태 (이어하기) |
| product_terms | 상품×약관유형 슬롯 |
| product_term_versions | 약관 버전 이력 |
| terms_types | 약관 유형 |
| required_terms_agreements / optional_terms_agreements | 약관 동의 이력 |
| id_verifications | 신분증 인증(OCR) 이력 |
| electronic_signatures | 전자 서명 |
| exchange_rates | 고시 환율 정보 |
| foreign_accounts / foreign_account_balances | 외화 계좌 및 잔액 이력 |
| user_logs | 사용자 행동 로그 (AI 추천 입력 데이터) |
| event / coupon | 이벤트, 쿠폰 |

---

## 프로젝트 구조

```
fx_bank/
├── spring-server/fx_bank/          # 메인 뱅킹 백엔드
│   └── src/main/java/com/example/bank/
│       ├── admin/                  # 상품/약관/우대금리 관리자 기능
│       ├── chatbot/                # 챗봇 연동 API
│       ├── event/                  # 이벤트/공지 관리
│       ├── fx/                     # 환율 조회, 스케줄러
│       ├── personal/               # 회원/인증
│       └── product/                # 금융 상품 (가입, 조회)
│
├── fastapi-agent/                  # ★ AI 금융 상품 추천 (금융 MBTI) — 담당 파트
│   ├── main.py                     # API 진입점, JWT+Redis 인증, 리뷰 분석 스케줄러
│   ├── database.py                 # Oracle 커넥션 풀
│   ├── chroma_store.py             # 임베딩 생성/저장/유사도 검색
│   └── services/
│       ├── review_service.py       # 리뷰 GPT 감성분석 배치
│       └── user_service.py         # 사용자 MBTI 산출 및 추천
│
├── fastapi-bnk/                    # YOLO 기반 통장 객체 탐지
├── fastapi-ocr/                    # YOLO + CLOVA OCR 기반 신분증 인식
├── fastapi-chat/                   # 외환 RAG 챗봇 (LangChain + ChromaDB)
├── flutter/bnk_app/                # 모바일 클라이언트
├── nginx/                          # 리버스 프록시 설정
└── docker-compose.yml              # 전체 서비스 오케스트레이션
```

---

## 7. 핵심 기능 상세 — AI 금융 상품 추천 (금융 MBTI)

### 7-1. 구조 및 파이프라인

리뷰 분석 배치(Batch)와 사용자 실시간 분석(Real-time) 두 축으로 동작합니다.

[매일 10:30 KST 배치]                    [사용자가 앱 접속 시]
product_reviews                          user_logs (최근 50건)
│                                        │
▼                                        ▼
GPT 감성/성향 분석                     키워드 매칭 스코어링
│                                        │
▼                                        ▼
ChromaDB 저장(임베딩)              금융 MBTI 4축 산출 (S/R, P/F, G/L, A/W)
│                                        │
▼                                        ▼
products.product_tendency 갱신        MBTI → 성향(tendency) 매핑
│
▼
ChromaDB 유사도 검색 (query = 성향 + 검색어)
│
▼
메인 추천 2개 + 서브 추천 3개 반환


### 7-2. 금융 MBTI 매핑 구조

| 축 | 의미 |
|---|---|
| **S / R** | 안전(Safe) 지향 vs 수익(Return, 환리스크 감수) 지향 |
| **P / F** | 목적성(Purpose, 장기 계획) vs 편의성(Fast, 즉시성) |
| **G / L** | 글로벌(Global, 외화) vs 국내(Local, 원화) |
| **A / W** | 적극(Active) vs 관망(Wait) |

* **성향(Tendency) 판정 공식:** * `S` + `P` $\rightarrow$ **목적외화형**
  * `S` + `F` $\rightarrow$ **편의환전형**
  * `R` + `G` $\rightarrow$ **환율수익형**
  * `R` + `L` $\rightarrow$ **고금리외화형**
* 총 16가지의 MBTI 조합별 맞춤형 설명 문구가 결과 화면에 노출됩니다.

### 7-3. 핵심 컴포넌트 구현 요약

* **JWT + Redis 인증 연동 (`main.py`)**
  * Spring Boot가 발급한 JWT의 Payload만 디코드한 뒤, Redis 세션(`USER:{user_id}`)으로 유효성을 크로스 체크합니다.
  * Spring의 로그아웃/만료 상태를 별도 Secret 공유 없이 실시간 동기화합니다.
* **FastAPI Lifespan & 스케줄러 (`main.py`)**
  * `lifespan` 컨텍스트 안에서 APScheduler를 제어하여 인스턴스당 매일 10:30분에 정확히 1회만 배치가 실행되도록 보장합니다.
* **Oracle 커넥션 풀 최적화 (`database.py`)**
  * `ping_interval=60` 설정을 통해 유휴 상태 연결 끊김 문제를 자동 감지 및 재연결로 해결했습니다.
* **벡터 저장소 구축 (`chroma_store.py`)**
  * `text-embedding-3-small` 모델과 코사인 유사도(`cosine`) 기반 컬렉션을 사용합니다. `upsert`를 적용해 재분석 시 중복 데이터가 덮어써지도록 설계했습니다.
* **GPT 기반 리뷰 분석 배치 (`review_service.py`)**
  * Structured Outputs(JSON 포맷 강제)을 활용해 리뷰에서 감성 점수, 키워드, 대표 성향을 추출한 뒤 ChromaDB와 RDB를 갱신합니다.
* **사용자 스코어링 및 3단계 폴백 추천 (`user_service.py`)**
  * 최근 로그 50건을 키워드 사전과 매칭해 4축 MBTI를 산출합니다. (신규 유저는 연령대별 기본값으로 대체)
  * **추천 로직:** 성향 일치 상품(**메인 2개**) + 유사도 기반 상품(**서브 3개**)을 반환하며, 상품 부족 시 `서브 상품 채움` $\rightarrow$ `감성 점수 상위 상품 채움` 순으로 3단계 폴백을 수행합니다.

> ⚠️ **주의 사항**: GPT 프롬프트의 성향(Tendency) 라벨명과 서비스 내 MBTI 매핑 라벨명이 정확히 일치해야 메인 추천 로직이 정상 작동합니다. 어긋날 경우 폴백 로직만 타게 됩니다.

### 7-4. API 응답 예시 (`POST /analyze-user`)

```json
{
  "status": "ok",
  "mbti": "RPGA",
  "mbti_description": "도전적으로 목표를 세우고 글로벌하게 적극적으로 투자하는 타입이에요 🚀",
  "tendency": "환율수익형",
  "main_recommendations": [ /* 성향 일치 상품 2개 */ ],
  "sub_recommendations": [ /* 유사도 기반 보조 상품 3개 */ ]
}

---

## 실행 방법

### 1. 전체 서비스 실행 (Docker Compose)

```bash
git clone https://github.com/사용자명/fx_bank.git
cd fx_bank
docker-compose up -d --build
```

전체 실행 시 각 서비스는 아래 포트로 접속할 수 있습니다.

| 서비스 | 포트 |
|---|---|
| Nginx | 7960 |
| fastapi-bnk | 7961 |
| fastapi-agent | 7962 |
| fastapi-ocr | 7963 |
| Spring Boot | 7968 |
| Redis | 6379 |

### 2. 환경 설정

`docker-compose.yml`이 참조하는 아래 파일들을 본인 환경에 맞게 준비해야 합니다.

```
# Oracle Wallet & Spring 설정
/home/사용자/application.properties
/home/사용자/Wallet_XXXXXXXX

# fastapi-agent/.env
OPENAI_API_KEY=발급받은_API_KEY
DB_USER=DB계정
DB_PASSWORD=DB비밀번호
DB_DSN=호스트:포트/서비스명
DB_WALLET_PATH=/wallet
REDIS_HOST=redis
REDIS_PORT=6379

# fastapi-ocr/.env
CLOVA_INVOKE_URL=발급받은_URL
CLOVA_SECRET_KEY=발급받은_SECRET_KEY
```

### 3. AI 추천 서버 단독 실행 (개발 시)

AI 추천 서버는 Spring Boot와 별도로 FastAPI 서버를 실행합니다.

```bash
cd fastapi-agent
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

---

## 개선 사항

| 개선 항목 | 내용 |
|---|---|
| Tendency 라벨 통일 | GPT 리뷰 분석 성향(4종)과 MBTI 매핑 성향 라벨 일치 → `/analyze-reviews` 재실행 |
| 추천 다양성 | 메인/서브 구성 비율 및 폴백 기준 고도화 |
| 콜드 스타트 개선 | 로그 기반 추천 정확도를 높이기 위한 초기 행동 유도 UX |
| 배치 모니터링 | 리뷰 분석 배치 실패/재시도 알림 체계 추가 |
| 인증 강화 | FastAPI 측에서도 JWT 서명 검증 추가 고려 |

---

## 회고

이번 프로젝트를 통해 외화 상품 안내 서비스는 단순한 상품 조회·가입 기능뿐만 아니라, 사용자 행동 데이터를 기반으로 한 개인화, 이미지 인식을 통한 자동화, 대화형 안내까지 함께 고려해야 한다는 점을 배웠습니다.

특히 AI 금융 상품 추천 파트는 사용자 로그가 거의 없는 신규 유저에게도 항상 의미 있는 추천을 내려줘야 한다는 점이 가장 큰 과제였습니다. 연령대 기본값 → 유사도 기반 추천 → 감성 점수 상위 추천으로 이어지는 3단계 폴백 구조를 설계하면서, 추천 시스템은 정확도만큼이나 콜드 스타트 상황에 대한 방어 로직이 중요하다는 것을 확인할 수 있었습니다. 또한 GPT로 생성한 리뷰 성향 라벨과 로그 기반 MBTI 매핑 라벨을 동일한 체계로 맞춰야 벡터 검색 필터링이 의도대로 동작한다는 점도 실제 운영 중 발견한 중요한 교훈이었습니다.

추후에는 Tendency 라벨 체계 통일, 추천 다양성 고도화, 배치 실패 모니터링을 보완하여 더 실제 서비스에 가까운 형태로 개선하고자 합니다.
