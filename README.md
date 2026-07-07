# 💱 FX Bank — AI 기반 외화 상품 추천 및 외환 뱅킹 서비스

BNK 부산은행 외환 서비스를 모티브로 제작한, 사용자 행동 기반 **금융 MBTI 추천**과 신분증/통장 **객체 탐지·OCR 우대금리 판별**, 외환 **RAG 챗봇**을 결합한 외환 뱅킹 플랫폼입니다.

사용자에게는 외화 상품 조회·가입, 금융 MBTI 기반 맞춤 추천, 우대금리 자동 판별, 외환 챗봇 기능을 제공하고, 관리자에게는 상품 등록·결재, 약관(PDF) 버전 관리, 회원/계좌/로그 관리 기능을 제공합니다.

---

## 목차

1. 프로젝트 소개
2. 팀 구성 및 담당 기능
3. 주요 기능
4. 기술 스택
5. 시스템 아키텍처
6. 프로젝트 구조
7. 핵심 기능 상세 — AI 금융 상품 추천 (금융 MBTI)
8. 실행 방법
9. 보안 및 안정성
10. 개선 사항

---

## 1. 프로젝트 소개

최근 디지털 금융 서비스 이용이 증가하면서 사용자는 외화 상품을 쉽고 편리하게 비교·가입할 수 있는 환경을 요구하고 있습니다. 하지만 외환 상품은 종류가 다양하고 조건이 복잡해 자신에게 맞는 상품을 찾기 어렵다는 문제가 있습니다.

본 프로젝트는 사용자 행동 로그와 리뷰 데이터를 분석해 **금융 MBTI(16유형)** 를 산출하고, 이를 기반으로 개인화된 외화 상품을 추천하며, 신분증·통장 이미지 인식으로 우대금리를 자동 판별하고, 외환 용어를 학습한 챗봇까지 제공하는 외환 뱅킹 서비스 구현을 목표로 했습니다.

### 개발 기간

| 구분 | 기간 |
|---|---|
| 기획 | `[YYYY/MM/DD ~ MM/DD]` |
| 설계 | `[MM/DD ~ MM/DD]` |
| 구현 | `[MM/DD ~ MM/DD]` |
| 발표 및 마무리 | `[MM/DD]` |
| 총 개발 기간 | `[약 O주]` |

---

## 2. 팀 구성 및 담당 기능

| 이름 | 담당 영역 | 주요 구현 내용 |
|---|---|---|
| `[이름]` | 메인 뱅킹/인증 | JWT + Redis 인증, 약관 관리, 관리자 워크플로우 |
| `[이름]` | 우대금리 판별 | YOLO 객체 탐지(통장), Naver CLOVA OCR 연동 |
| `[이름]` | 외환 챗봇 | LangChain + ChromaDB 기반 RAG 챗봇 |
| `[이름]` | 모바일 클라이언트 | Flutter 앱, 사용자 화면 |
| **다롱행** | **AI 금융 상품 추천** | **사용자 로그 기반 금융 MBTI 산출, GPT 리뷰 감성분석, ChromaDB 벡터 검색 추천, APScheduler 배치** |

> 팀원 이름/역할은 실제 구성에 맞게 채워 넣으면 됨.

---

## 3. 주요 기능

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
| 약관 관리 | 약관 버전 관리 | 상품×약관유형 슬롯 + 버전 이력(`is_current`, `effective_dt`) |
| 결재 관리 | 상품 승인/반려 | 금리 담당 → 약관 담당 → 상품 기획 → 최종 승인 4단계 워크플로우 |
| 이벤트 관리 | 공지/이벤트 등록 | 이벤트, 쿠폰 관리 |
| 로그 관리 | 사용자 행동 로그 | AI 추천의 입력 데이터로 활용되는 `user_logs` 적재 |

---

## 4. 기술 스택

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

## 5. 시스템 아키텍처

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

## 6. 프로젝트 구조

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

### 7-1. 개요

리뷰 분석 배치와 사용자 실시간 분석, 두 축으로 동작합니다.

```
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
```

### 7-2. 금융 MBTI 구조

| 축 | 의미 |
|---|---|
| **S / R** | 안전(Safe) 지향 vs 수익(Return, 환리스크 감수) 지향 |
| **P / F** | 목적성(Purpose, 장기 계획) vs 편의성(Fast, 즉시성) |
| **G / L** | 글로벌(Global, 외화) vs 국내(Local, 원화) |
| **A / W** | 적극(Active) vs 관망(Wait) |

```python
def get_tendency_from_mbti(mbti):
    s_or_r, p_or_f, g_or_l = mbti[0], mbti[1], mbti[2]
    if s_or_r == "S" and p_or_f == "P":
        return "목적외화형"
    elif s_or_r == "S" and p_or_f == "F":
        return "편의환전형"
    elif s_or_r == "R" and g_or_l == "G":
        return "환율수익형"
    elif s_or_r == "R" and g_or_l == "L":
        return "고금리외화형"
    else:
        return "목적외화형"
```

16가지 조합(`SPGA`, `RFLW` 등)마다 이모지가 붙은 설명 문구를 미리 정의해 결과 화면에 노출합니다.

### 7-3. 코드 설명

**① JWT + Redis 인증 (`main.py`)**
Spring Boot가 발급한 JWT를 서명 검증 없이 payload만 디코드하고, 실제 유효성은 Redis 세션(`USER:{user_id}`)으로 확인합니다. Spring에서 로그아웃/만료 처리를 하면 Redis 키가 사라지므로, 별도 시크릿 없이도 Spring과 동일한 인증 상태를 공유합니다.

```python
def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    payload = jwt.decode(credentials.credentials, options={"verify_signature": False})
    user_json = redis_client.get(f"USER:{payload.get('sub')}")
    if not user_json:
        raise HTTPException(status_code=401, detail="만료된 세션 다시 로그인해야 됨")
    return json.loads(user_json)
```

**② 배치 스케줄러 (`main.py`)**
FastAPI `lifespan` 안에서 APScheduler를 시작/종료해 인스턴스당 정확히 한 번만 동작하도록 관리합니다.

```python
scheduler.add_job(scheduled_review_analysis, trigger=CronTrigger(hour=10, minute=30))

@asynccontextmanager
async def lifespan(app: FastAPI):
    scheduler.start(); yield; scheduler.shutdown(wait=False)
```

**③ Oracle 커넥션 풀 (`database.py`)**
`ping_interval=60`으로 60초마다 죽은 연결을 자동 감지·교체해 유휴 상태 연결 끊김 문제를 해결했습니다.

```python
_pool = oracledb.create_pool(
    user=..., dsn=..., wallet_location=...,
    min=2, max=10, increment=1, ping_interval=60,
)
```

**④ 벡터 저장소 (`chroma_store.py`)**
`text-embedding-3-small`로 임베딩 후 코사인 유사도 기반 컬렉션에 저장, `upsert`로 재분석 시 중복 없이 덮어씁니다.

```python
collection = chroma_client.get_or_create_collection(name="reviews", metadata={"hnsw:space": "cosine"})

def add_review(review_no, product_no, review_text, sentiment, keywords, tendency, embedding):
    collection.upsert(ids=[str(review_no)], embeddings=[embedding], documents=[review_text],
        metadatas=[{"product_no": str(product_no), "sentiment": sentiment,
                    "keywords": ", ".join(keywords), "tendency": tendency}])
```

**⑤ 리뷰 감성 분석 배치 (`review_service.py`)**
활성 상품 리뷰를 조회 → GPT에 JSON 형식 응답을 강제해 감성/키워드/성향 추출 → ChromaDB 적재와 동시에 상품별 감성 점수·대표 성향 집계.

```python
prompt = f"""
아래 외화 금융 상품 리뷰를 분석해 주세요.
반드시 아래 JSON 형식으로만 답하세요.
{{"sentiment": "긍정/부정/중립", "keywords": [...], "tendency": "목적외화형/편의환전형/환율수익형/고금리외화형"}}
[리뷰] {review_text}
"""
# 상품별 집계 후 대표 성향/감성 점수 반영
score = sentiments.count("긍정") - sentiments.count("부정")
best_tendency = max(counts, key=counts.get) if counts else "안정형"
```

**⑥ 사용자 MBTI 산출 및 추천 (`user_service.py`)**
최근 로그 50건을 키워드 사전과 매칭해 8개 항목을 스코어링, 각 축에서 점수가 높은 알파벳을 채택합니다.

```python
S_KEYWORDS = ["안전", "원금보장", "확정금리"]; R_KEYWORDS = ["환율", "수익", "환차익", "투자"]
mbti = ""
mbti += "S" if scores["S"] >= scores["R"] else "R"
mbti += "P" if scores["P"] >= scores["F"] else "F"
mbti += "G" if scores["G"] >= scores["L"] else "L"
mbti += "A" if scores["A"] >= scores["W"] else "W"
```

로그가 없는 신규 유저는 연령대 기본값으로 폴백합니다.

```python
AGE_DEFAULT_MBTI = {"20대": "RFGA", "30대": "RPGA", "40대": "SPGA", "50대이상": "SPLW"}
```

추천은 성향 일치 상품을 **메인 2개**, 유사도 기반 상품을 **서브 3개**로 구성하며, 부족할 경우 서브 → 감성 점수 상위 상품 순으로 **3단계 폴백**합니다.

```python
if meta.get("tendency") == tendency and len(main_recommendations) < 2:
    main_recommendations.append(item)
elif len(sub_recommendations) < 3:
    sub_recommendations.append(item)
# 폴백 1: 메인이 없으면 서브로 채움 / 폴백 2: 감성 점수 상위로 채움
```

> ⚠️ **알려진 이슈**: GPT 프롬프트의 tendency 라벨과 MBTI 매핑의 라벨을 4종으로 일치시켜야 `search_similar()` 필터링과 메인 추천 매칭이 정확합니다. 라벨이 어긋나면 메인 추천이 비어 폴백만 계속 타는 문제가 생깁니다.

### 7-4. 응답 예시 (`POST /analyze-user`)

```json
{
  "status": "ok",
  "mbti": "RPGA",
  "mbti_description": "도전적으로 목표를 세우고 글로벌하게 적극적으로 투자하는 타입이에요 🚀",
  "tendency": "환율수익형",
  "main_recommendations": [ /* 성향 일치 상품 2개 */ ],
  "sub_recommendations": [ /* 유사도 기반 보조 상품 3개 */ ]
}
```

---

## 8. 실행 방법

### 사전 준비
- Docker & Docker Compose
- Oracle DB 지갑 파일(Wallet), `application.properties`
- `fastapi-agent/.env` (OpenAI API Key, Redis, Oracle 접속 정보), `fastapi-ocr/.env` (CLOVA OCR Key)

### 전체 실행
```bash
docker-compose up -d --build
```

| 서비스 | 포트 |
|---|---|
| Nginx | 7960 |
| fastapi-bnk | 7961 |
| fastapi-agent | 7962 |
| fastapi-ocr | 7963 |
| Spring Boot | 7968 |
| Redis | 6379 |

### AI 추천 서버 단독 실행 (개발 시)
```bash
cd fastapi-agent
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

---

## 9. 보안 및 안정성

| 항목 | 내용 |
|---|---|
| JWT + Redis 세션 | Spring이 발급한 JWT를 FastAPI가 서명 검증 없이 payload만 사용하고, Redis 세션 존재 여부로 실제 로그인 상태 검증 |
| Oracle 커넥션 풀 | `ping_interval=60`으로 유휴 연결 자동 감지·교체 |
| ChromaDB upsert | 리뷰 재분석 시 중복 저장 방지 |
| 배치 격리 | APScheduler를 `lifespan`으로 관리해 인스턴스당 1회만 실행 |
| 약관 PDF | 버전 관리(`is_current`, `effective_dt`, `expired_dt`)로 이력 보존 |

---

## 10. 개선 사항

| 개선 항목 | 내용 |
|---|---|
| Tendency 라벨 통일 | GPT 리뷰 분석 성향(4종)과 MBTI 매핑 성향 라벨 일치 → `/analyze-reviews` 재실행 |
| 추천 다양성 | 메인/서브 구성 비율 및 폴백 기준 고도화 |
| 콜드 스타트 개선 | 로그 기반 추천 정확도를 높이기 위한 초기 행동 유도 UX |
| 배치 모니터링 | 리뷰 분석 배치 실패/재시도 알림 체계 추가 |
| 인증 강화 | FastAPI 측에서도 JWT 서명 검증 추가 고려 |
