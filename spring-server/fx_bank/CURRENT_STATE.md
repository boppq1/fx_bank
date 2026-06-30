# fx_bank 프론트 현황 조사 — CURRENT_STATE (읽기 전용 리포트)

> 본 문서는 **코드 수정 없이** 현재 구현 상태/원인만 조사한 리포트다. 다음 단계의 실제 수정 프롬프트 작성을 위한 기준 자료.
> 조사 대상(2차): `C:\dev\ws_stsx_bank\spring-serverx_bank`

## 0. 프로젝트 구조

### 0-1. templates 디렉토리 트리 (.html 전체)

루트: `C:\dev\ws_sts\fx_bank\spring-server\fx_bank\src\main\resources\templates\`

```
templates/
├─ a.html                       (auth-guard.js 만 로드하는 테스트용 보호 페이지)
├─ index.html                   (Silent Refresh 테스트용 메인, 인라인 스크립트만)
├─ fx-home.html                 (실제 홈 페이지 — header+body+footer 조립)
├─ body.html                    (fragment: th:fragment="body")
├─ auth-body.html               (fragment: th:fragment="authBody")
├─ header.html                  (fragment: th:fragment="header")
├─ footer.html                  (fragment: th:fragment="footer")
├─ login.html
├─ register.html
├─ reauth.html
├─ admin/
│  ├─ adminProList.html
│  ├─ adminProDetail.html
│  ├─ adminTermsRegister.html
│  ├─ adminVersionHis.html
│  └─ mbtiService.html
├─ fx/
│  ├─ exchange.html
│  ├─ exchange-calculator.html
│  ├─ exchange-rate.html
│  ├─ guide.html
│  ├─ fx-portfolio-simulator.html   (빈 껍데기 — body 내용 없음, 미사용 추정)
├─ event/
│  ├─ event.html
│  └─ event-status.html
└─ product/
   ├─ list.html
   ├─ detail.html
   ├─ reviews.html
   ├─ review-form.html
   ├─ my-subscriptions.html
   └─ join/
      ├─ form.html
      ├─ terms.html
      ├─ terms-reader.html
      ├─ signature.html
      ├─ coupon.html
      └─ complete.html
```

참고: `a.html`, `index.html`, `fx/fx-portfolio-simulator.html`, `fx/exchange.html` 는 fragment include 없이 단독 구성(`exchange.html`은 th:replace 그렙에 안 잡힘 — 자세한 link/script는 코드에서 확인 안 됨, 별도 미조사). `fx-portfolio-simulator.html`은 `<body>`가 비어 있는 미완성/미사용 파일.

### 0-2. static/css, static/js 구조

루트: `...\src\main\resources\static\`

```
static/
├─ css/
│  ├─ fx-home.css     (전역 공통: header/footer/홈 레이아웃)
│  ├─ fx-auth.css     (로그인/회원가입/재인증 전용)
│  ├─ fx-guide.css    (가이드·환율·환전계산기·상품 전반)
│  ├─ event.css       (이벤트 페이지 전용)
│  └─ chatbot.css     (챗봇 위젯 전용)
└─ js/
   ├─ auth.js             (auth-body 회원가입/로그인 로직)
   ├─ auth-guard.js       (보호 페이지 가드)
   ├─ auth-refresh.js     (공용 single-flight 토큰 재발급)
   ├─ header-auth.js      (헤더 로그인상태 토글)
   ├─ reauth.js
   ├─ fx-home.js          (홈 환율 캐러셀/카드)
   ├─ fx-currency.js      (환율·환전계산기 공용 통화 데이터)
   ├─ fx-calc.js          (환전계산기)
   ├─ fx-rate.js          (환율조회)
   ├─ fx-guide.js         (가이드)
   └─ event.js            (이벤트)
```

별도 하위 폴더/이미지/폰트 에셋 없음 (css, js 2개 폴더만 존재).

### 0-3. 공통 fragment 위치 및 include 매핑

fragment 정의 4개 (모두 templates 루트):
- `header.html` → `th:fragment="header"` (line 9)
- `footer.html` → `th:fragment="footer"` (line 9)
- `body.html` → `th:fragment="body"` (line 10) — 홈 전용 메인 콘텐츠
- `auth-body.html` → `th:fragment="authBody"` (line 9) — 로그인/회원가입 카드

모든 include 방식은 `th:replace="~{프래그먼트파일 :: 프래그먼트명}"` (th:insert 사용처 없음).

| fragment | include 하는 페이지 |
|---|---|
| `header :: header` | fx-home, login, register, reauth, fx/guide, fx/exchange-rate, fx/exchange-calculator, product/list, product/detail, product/reviews, product/review-form, product/my-subscriptions, product/join/{form,terms,terms-reader,signature,coupon,complete}, event/event, event/event-status (총 약 20개 페이지) |
| `footer :: footer` | header를 include하는 위 페이지 전부 동일 (header/footer는 항상 쌍으로 사용) |
| `body :: body` | fx-home.html (홈 1곳 전용) |
| `auth-body :: authBody` | login.html, register.html (2곳) |

추가 사실:
- fragment 안에 관련 `<script>`를 같이 넣어 둠. `auth-body.html`은 fragment(`<main>`) 내부에 daum postcode + `auth.js` 포함(line 259-260). `header.html`은 fragment 내부에 `auth-refresh.js`, `header-auth.js`, 구글번역 스크립트 포함(line 112-184). `body.html`은 fragment 내부에 `fx-home.js` + 챗봇 인라인 스크립트 포함.
- admin 5개 페이지(adminProList, adminProDetail, adminTermsRegister, adminVersionHis, mbtiService)는 header/footer fragment를 include하지 않음 — 각자 `<style>` 인라인으로 자체 스타일 보유(공통 fragment 미사용).

### 0-4. CSS 적용 방식 (페이지별 link 매핑)

**페이지별 분리 + 공통 베이스 조합** 방식. `fx-home.css`가 사실상 공통 베이스(header/footer fragment가 항상 link), 거기에 페이지군별 전용 CSS를 추가로 link.

| CSS 파일 | link 하는 곳 |
|---|---|
| `fx-home.css` | 공통 베이스. header.html, footer.html, body.html, auth-body는 아니고 → fx-home, login, register, reauth, fx/*, product/*, event/* 거의 전 페이지 head에서 link (fragment 통해서도 중복 link됨) |
| `fx-auth.css` | login.html, register.html, reauth.html, auth-body.html |
| `fx-guide.css` | fx/guide, fx/exchange-rate, fx/exchange-calculator, product/list, product/detail, product/reviews, product/review-form, product/my-subscriptions, product/join/{form,terms,terms-reader,signature,coupon,complete} |
| `event.css` | event/event.html, event/event-status.html |
| `chatbot.css` | fx-home.html (head, line 8) |

즉 전형적으로 각 페이지 head에 `fx-home.css`(공통) + 해당 페이지군 CSS(fx-auth / fx-guide / event 중 1개)를 2줄로 link. 단일 통합 CSS 파일은 아님. admin 페이지는 외부 CSS link 없이 `<style>` 인라인만.

### 0-5. 빌드/번들링 방식

- **순수 Thymeleaf + 정적 서빙(static)** 방식. 별도 프론트엔드 빌드 도구 없음.
- `build.gradle` 존재 (Spring Boot 3.3.4, Java 21, Gradle). 의존성: spring-boot-starter-web/security/thymeleaf/validation, thymeleaf-extras-springsecurity6, MyBatis+Oracle(ojdbc11), Redis, JWT(jjwt 0.12.3), Lombok, PDF 처리(tabula/pdfbox 2.0.31/commons-csv).
- `settings.gradle`: `rootProject.name = 'fx_bank'` (단일 모듈, 멀티프로젝트 아님).
- `package.json` 코드에서 확인 안 됨 (조사 루트 전체에서 발견되지 않음) → webpack/vite/npm 등 JS 번들러 미사용.
- JS/CSS는 모두 `/css`, `/js` 정적 경로로 직접 서빙(`th:href="@{/css/...}"`, `th:src="@{/js/...}"`), 트랜스파일/미니파이/번들 단계 없음. 외부 스크립트는 CDN 직접 link(구글 번역 `translate.google.com`, daum postcode `t1.kakaocdn.net`).

---

# 항목별 조사

### 1. [전역] 이모티콘 전수조사

#### 1) 관련 파일 경로 (상대경로, 이모지/픽토그램 포함 파일 전부)

**진짜 유니코드 이모지(컬러 픽토그램) 포함 — `<img>` 교체 1순위**
- `templates/auth-body.html`
- `templates/body.html`
- `templates/header.html`
- `templates/index.html`
- `templates/reauth.html`
- `templates/admin/adminTermsRegister.html`
- `templates/admin/mbtiService.html`
- `templates/event/event-status.html`
- `templates/event/event.html`
- `templates/product/join/complete.html`
- `static/js/auth.js`
- `static/js/event.js`
- `static/js/reauth.js`
- `static/css/fx-guide.css` (`content: "ℹ️"`, `content: "⚠️"`)

**UI 기호(화살표/별/셰브론 등) — 폰트/CSS·아이콘 교체 검토 대상**
- `templates/body.html` (`★ → ‹ › ● …`)
- `templates/header.html` (`▾`)
- `templates/admin/mbtiService.html` (`▼`)
- `templates/product/detail.html` (`⌄` CSS content)
- `static/css/fx-home.css` (`▾ ▴` 셰브론 pseudo-element)
- `static/js/fx-home.js` (`▲ ▼ –`)
- `static/js/fx-rate.js` (`‹ › …` 페이지네이션)
- 그 외 다수 파일의 `·`(가운뎃점) `–`(en dash) `—`(em dash) `→` `←` — 대부분 **본문 구분자/주석**으로 장식 아님(교체 불필요).

**비고:** `static/` 아래에 이미지 디렉토리(`img`/`images`/`icons`/`assets`)나 이미지 파일이 **하나도 없음**(코드에서 확인). `<img>` 교체 시 새 에셋 폴더를 만들어야 함.

---

#### 2) 현재 로직 (렌더링 방식)

- 대부분의 이모지는 **HTML 인라인 텍스트**로 `<span class="...-ico">🪪</span>` 형태로 직접 박혀 있어 브라우저/OS 기본 이모지 폰트로 렌더링됨(아이콘 폰트나 SVG 아님).
- 일부는 **CSS `content`** 로 렌더(`fx-guide.css`의 `ℹ️/⚠️`, `fx-home.css`의 `▾/▴`, `product/detail.html`의 `⌄`).
- 일부는 **JS 문자열**로 동적 삽입: 상태 토글(`auth.js`/`reauth.js`의 `⏳/✅/❌`), `alert()`(`event.js`의 `🎉`), 페이지네이션 버튼(`fx-rate.js`의 `‹ › …`), 등락 화살표(`fx-home.js`의 `▲▼–`).
- Thymeleaf 인라인 삼항으로 상태 표기: `event-status.html`이 `${...} ? '✅ 인증완료' : '❌ 미인증'`.

---

#### 3) 핵심 코드 스니펫 (실제 인용)

**templates/auth-body.html — 좌측 소개 패널 + 입력창 아이콘 (단순 장식/입력 아이콘)**
```html
20: <span class="feat-ico">🪪</span>  ... <strong>신분증 OCR 인증</strong>
24: <span class="feat-ico">📈</span>  ... <strong>실시간 환율·환전 계산</strong>
28: <span class="feat-ico">🌏</span>  ... <strong>다국어 지원</strong>
52: <span class="in-ico">👤</span>   <input id="loginId" ...>
60: <span class="in-ico">🔒</span>   <input type="password" id="loginPw" ...>
62: <button class="pw-toggle" data-target="loginPw" aria-label="비밀번호 표시">👁</button>
99: <span class="idcard-ico">🪪</span>  (신분증 업로더 드롭존)
161:<span class="in-ico">✉️</span>   <input type="email" id="email" ...>
```

**templates/body.html — 빠른이용/추천상품/AI/챗봇 (버튼·카드 아이콘 + 상태/장식)**
```html
19:  <span class="hero-badge">● 실시간 환율 · AI 외환 도우미</span>
40:  <button class="hf-nav" id="hfPrev" aria-label="이전 통화">‹</button>
42:  <button class="hf-nav" id="hfNext" aria-label="다음 통화">›</button>
70:  <span class="qc-ico">🧮</span> ... <span class="qc-arrow">→</span>   (환전계산기)
75:  <span class="qc-ico">📈</span> ... 환율조회
80:  <span class="qc-ico">📘</span> ... 외환 상품·서비스 안내
103: <span class="product-ico">💵</span>   111: <span class="product-score">★ 4.8</span>
118: <span class="product-ico">🌐</span>   133: 💶   147: 💴
112: <a class="product-link">가입하기 →</a>
169: <span class="ai-ico">💬</span>   184: <span>💬</span> (챗봇 헤더)
187: <button id="chatbot-close" class="chatbot-close" aria-label="닫기">✕</button>
```

**templates/header.html — 상단 유틸/검색/알림 (버튼 아이콘)**
```html
36:  <button class="util-lang notranslate" id="langBtn">🌐 <span id="langCur">한국어</span> ▾</button>
99:  <span class="gnb-search-ico">🔍</span>  <input placeholder="상품 검색...">
102: <button class="gnb-bell" aria-label="알림">🔔</button>
```

**templates/event/event-status.html — 인증 상태표시 (Thymeleaf 삼항)**
```html
20: <span th:text="${event.b == 'Y'} ? '✅ 인증완료' : '❌ 미인증'"></span>  (n/k 동일 25,30)
36: <h3>🎉 축하합니다!</h3>
```

**static/js/auth.js / reauth.js — OCR 상태표시(동적, 텍스트 강조)**
```js
auth.js:265  setOcrStatus("⏳ 신분증을 인식하는 중입니다...", "loading");
auth.js:278  setOcrStatus("❌ 인식 실패: " + result.message, "error");
auth.js:296  textEl.textContent = "✅ 신분증 인증 완료 (필요 시 직접 수정하세요)";
reauth.js:38/47/57/61  동일 패턴 ⏳ / ❌ / ✅
```

**static/js — 화살표/페이지네이션 (기능성 기호)**
```js
fx-home.js:15  function arrow(n){ return n>0 ? '▲' : (n<0 ? '▼' : '–'); }   // 등락 표시
fx-rate.js:73  html += pageBtn(page-1, '‹', {disabled: page===1});
fx-rate.js:82  if (startP>2) html += '<span class="guide-page-dots">…</span>';
fx-rate.js:92  html += pageBtn(page+1, '›', {disabled: page===pages});
```

**CSS pseudo-element 아이콘**
```css
fx-guide.css:258  .guide-note::before { content: "ℹ️"; ... }
fx-guide.css:260  .guide-note.guide-warn::before { content: "⚠️"; }
fx-home.css:393   .gnb-item.has-sub > .gnb-link::after{ content:'▾'; ... }  // 펼침 표시
fx-home.css:396   .gnb-item.is-open > .gnb-link::after{ transform:rotate(180deg);}  // ▴
product/detail.html:45  content: '⌄';   (아코디언 셰브론)
```

**templates/index.html / admin — 콘솔로그/관리자 화면**
```html
index.html:9   <h2>🏦 부산은행 외환 시스템 - 메인 페이지</h2>   13: 🔒 안전하게 로그아웃
index.html:35  console.log("🎉 [Silent Refresh 성공]...");  39: ✅...  42: ❌...  (콘솔/내부 메시지)
adminTermsRegister.html:186 <div class="icon">📄</div>   313: 📎 ${file.name}   377: ✅ 등록 완료
mbtiService.html:327~330 <span class="si">⏳</span>  (분석 단계)  388: ✅/⏳ 토글
mbtiService.html:348 🎯 딱 맞는 상품  353: 💡 함께 보면 좋은 상품  357: <span class="chevron">▼</span>
mbtiService.html:456~460 📊 / 🙌 / 📋 / ℹ️ (정보부족 배너 아이콘 맵)
```

**기타 단발 이모지**
```html
event/event.html:42        <p>📱 이벤트 참여는 앱에서만 가능합니다.</p>
product/join/complete.html:30 <p class="guide-note">🎉 가입이 정상적으로 완료되었습니다.</p>
reauth.html:29             <span class="idcard-ico">🪪</span>
fx/guide.html:475         ☎ 84-28-7301-6200 (FAQ 본문)
```

---

#### 4) 맥락 분류 + `<img>` 교체 위치 메모 (영역별)

| 파일:라인 | 이모지 | 분류 | `<img>` 교체 위치 메모 |
|---|---|---|---|
| auth-body.html:20,24,28 | 🪪📈🌏 | 단순 장식(소개 리스트) | `<span class="feat-ico">` 안의 텍스트를 `<img class="feat-ico" src=...>`로 치환 |
| auth-body.html:52,60,110,119,161 | 👤🔒✉️ | 입력창 아이콘 | `<span class="in-ico">` 내부를 `<img>`로. 단 CSS `.in-ico` 폭/정렬 영향 |
| auth-body.html:62 | 👁 | 버튼 아이콘(비번 토글) | `<button class="pw-toggle">` 내부 텍스트→`<img>`, aria-label 유지 |
| auth-body.html:99 / reauth.html:29 | 🪪 | 버튼 아이콘(신분증 드롭존) | `<span class="idcard-ico">` 내부 교체 |
| body.html:70,75,80 | 🧮📈📘 | 버튼/카드 아이콘(빠른이용) | `<span class="qc-ico">` 내부를 `<img>`로 |
| body.html:103,118,133,147 | 💵🌐💶💴 | 카드 아이콘(상품) | `<span class="product-ico">` 내부 교체 |
| body.html:169,184 | 💬 | 배너/위젯 아이콘 | `<span class="ai-ico">` / `<span>` 내부 교체 |
| body.html:19 / 33,111… / 40,42,96 / 72… / 187 | ●/★/‹›/→/✕ | 장식·평점·캐러셀nav·CTA화살표·닫기버튼 | 기호류는 SVG 아이콘 or 아이콘폰트로 통일 권장(`<img>`보단 인라인 SVG 적합) |
| header.html:36 | 🌐 ▾ | 버튼 아이콘(언어선택) | `langBtn` 첫 글로브를 `<img>`로, `▾`는 CSS 화살표 유지 가능 |
| header.html:99,102 | 🔍🔔 | 버튼 아이콘(검색/알림) | `<span class="gnb-search-ico">` / `<button class="gnb-bell">` 내부 교체 |
| event-status.html:20,25,30,36 | ✅❌🎉 | 상태표시(인증결과) | Thymeleaf 삼항이라 단순 `<img>` 치환 어려움 → 상태 클래스+CSS 배경이미지 권장 |
| event.js:49,62 / index.js / auth.js / reauth.js | 🎉⏳✅❌ | 상태표시(alert/동적텍스트) | `alert()`·`textContent` 문자열이라 `<img>` 불가, 텍스트만 제거 or DOM 노드로 변경 필요 |
| complete.html:30 / event.html:42 | 🎉📱 | 텍스트 강조(안내문) | `<p>` 앞에 `<img>` prepend |
| mbtiService.html:327-330,348,353,357,388,456-460 | ⏳🎯💡▼✅📊🙌📋ℹ️ | 상태/장식/아이콘맵 | `.si`/`.chevron`은 `<img>` 치환 가능, JS 아이콘맵(456-460)은 src 맵으로 변경 |
| adminTermsRegister.html:186,313,377 | 📄📎✅ | 아이콘/상태(업로드) | 186 `<div class="icon">`만 정적 `<img>` 치환 용이, 313/377은 JS 문자열 |
| index.html:9,13,35,39,42,49 | 🏦🔒🎉✅❌🚨 | 제목/버튼/콘솔로그 | 9·13만 화면 노출(→`<img>`/제거), 35·39·42·49는 콘솔/내부메시지(교체 불요) |
| fx-guide.css:258,260 | ℹ️⚠️ | 노트/경고 아이콘(CSS content) | `content` 제거 후 `background-image: url(...)` 로 전환 |
| fx-home.css:393,396 / detail.html:45 | ▾▴⌄ | 셰브론(CSS content) | 기능성 셰브론, 기존 유지 권장(이미지 교체 비권장) |
| fx-home.js:15 / fx-rate.js:73,82,88,92 | ▲▼–‹›… | 등락/페이지네이션 | 기능성 기호, 텍스트 유지 권장 |
| guide.html:475 | ☎ | 텍스트 장식(FAQ) | 인라인 `<img>` prepend 가능 |

---

#### 5) 수정 시 영향 범위

- **공용 레이아웃(header.html / footer.html / body.html / auth-body.html)**: header/footer는 `th:replace`/`th:insert`로 전 페이지에 포함되므로, 여기 아이콘을 바꾸면 모든 화면에 동시 반영됨.
- **CSS 클래스 의존성**: `.feat-ico .in-ico .qc-ico .product-ico .ai-ico .idcard-ico .gnb-search-ico .gnb-bell` 등은 `fx-auth.css`, `fx-home.css`에서 폰트크기/정렬 기준으로 스타일됨. 텍스트 이모지를 `<img>`로 바꾸면 width/height·line-height·vertical-align을 CSS에서 함께 조정해야 깨지지 않음.
- **JS 동적 삽입 문자열**: `auth.js`/`reauth.js`의 `setOcrStatus(...)`, `event.js`의 `alert(...)`, `fx-rate.js`의 `pageBtn(...)`, `fx-home.js`의 `arrow(...)`, `mbtiService.html`의 아이콘맵/`.si` 토글 — 이들은 마크업이 아니라 JS 문자열이라 `<img>` 직접 치환 불가. 함수 내부 로직(문자열→DOM노드/`<img>` 태그 문자열)을 같이 고쳐야 함.
- **Thymeleaf 삼항(event-status.html)**: `'✅ 인증완료' : '❌ 미인증'` 구조라 이미지로 바꾸려면 표현식을 클래스 토글 방식으로 재작성 필요(연쇄로 `event.css`의 `.status` 스타일 영향).
- **에셋 부재**: `static/`에 이미지 폴더/파일이 전혀 없음 → 새 디렉토리(예: `static/img`) 신설 + `th:src="@{/img/...}"` 경로 매핑 필요(정적 리소스 핸들러 설정 확인 필요, 코드에서 핸들러 설정은 확인 안 됨).
- **`notranslate` 영역**: `header.html`의 `langBtn`(🌐)은 `class="notranslate"` 라 다국어 번역 제외 처리와 함께 묶여 있음 — 교체 시 클래스 유지 주의.
- **콘솔/내부 메시지(index.html 35/39/42/49, 각 JS 주석의 → ← — ·)**: 사용자 화면 비노출이거나 단순 주석 구분자라 교체 대상에서 제외해도 무방.

### 2. [전역] 헤더 알림(종) 아이콘

**1) 관련 파일 경로** (상대경로)
- `src/main/resources/templates/header.html` — 종 아이콘 마크업(102번째 줄)
- `src/main/resources/static/css/fx-home.css` — `.gnb-bell` 스타일(136~140번째 줄)
- (참고) `src/main/resources/static/js/header-auth.js`, `src/main/resources/static/js/auth-refresh.js` — 헤더에서 로드되는 JS이나, **종/알림 관련 코드 없음**(검색 결과 매치 0건)

**2) 현재 로직**
- 헤더 GNB 우측 도구 영역(`.gnb-tools`)에 검색창 옆 단순 `<button>` 으로 종 이모지(🔔)만 렌더링된다.
- 클릭 핸들러·드롭다운·뱃지(미읽음 카운트)가 전혀 연결돼 있지 않다. CSS에는 hover 시 배경색 변경(`.gnb-bell:hover`)만 정의돼 있어 시각적 피드백만 있고 클릭해도 아무 동작이 없다.
- 즉 **단순 장식(placeholder) UI**다. `gnb-bell` 식별자를 templates/static/java 전체에서 검색해도 JS 이벤트 바인딩이나 알림 API 호출, 뱃지 카운트 요소가 코드에서 확인 안 됨.

**3) 핵심 코드 스니펫** (실제 파일 그대로 인용)

`templates/header.html` (97~105번째 줄):
```html
      <div class="gnb-tools">
        <div class="gnb-search">
          <span class="gnb-search-ico">🔍</span>
          <input type="text" placeholder="상품 검색...">
        </div>
        <button type="button" class="gnb-bell" aria-label="알림">🔔</button>
        <a th:href="@{/login}" class="gnb-login auth-guest">로그인</a>
        <button type="button" class="gnb-login auth-user" id="gnbLogout" hidden>로그아웃</button>
      </div>
```

`static/css/fx-home.css` (136~140번째 줄):
```css
.gnb-bell{
  border:0;background:transparent;cursor:pointer;font-size:17px;
  width:34px;height:34px;border-radius:9px;transition:background var(--t) var(--ease);
}
.gnb-bell:hover{background:var(--lavender);}
```

- 참고: 버튼에 `id`가 없고 클래스만 `gnb-bell` 하나이며, 뱃지/카운터용 자식 요소(`<span>` 등)나 `data-*` 속성도 없다. `type="button"` 이라 폼 제출 동작도 없다.

**4) 수정 시 영향 범위**
- `header.html`의 `header` Thymeleaf fragment(`th:fragment="header"`)는 전역 공통 헤더이므로, 이 버튼을 수정하면 **이 fragment를 include하는 모든 페이지**에 동일하게 반영된다(전역 영향).
- `.gnb-bell` 클래스 스타일은 `fx-home.css`에만 정의돼 있으므로, 마크업의 클래스명을 바꾸면 해당 CSS 규칙(136~140번째 줄)도 함께 수정 필요.
- 현재 어떤 JS도 이 버튼을 참조하지 않으므로, 종 버튼 자체를 추가/제거/리네임해도 깨지는 기존 JS 로직은 코드에서 확인 안 됨. 다만 새로 드롭다운/뱃지/클릭 핸들러를 붙이려면 헤더에서 이미 로드 중인 `header-auth.js`(로그인 상태 토글 담당) 또는 신규 스크립트와의 연동, 그리고 미읽음 카운트를 제공할 백엔드 알림 API가 필요한데 **현재 코드베이스에 알림 관련 엔드포인트/스크립트는 확인 안 됨**(신규 구현 대상).

### 3. [전역] 푸터 FAMILY SITE / 카테고리

**1) 관련 파일 경로** (상대경로)
- `src/main/resources/templates/footer.html` — FAMILY SITE 셀렉트 + 카테고리(`footer-col`) 마크업 본체 (`th:fragment="footer"`)
- `src/main/resources/static/css/fx-home.css` — footer 그리드/카테고리/FAMILY SITE 셀렉트 스타일 (L326~369, 반응형 L376·L408~409)
- footer 프래그먼트를 `th:replace="~{footer :: footer}"`로 참조하는 페이지 (전역, 21개):
  - `templates/fx-home.html`(L13), `login.html`(L13), `reauth.html`(L52), `register.html`(L13)
  - `templates/event/event-status.html`(L42), `event/event.html`(L46)
  - `templates/product/detail.html`(L190), `list.html`(L120), `reviews.html`(L69), `my-subscriptions.html`(L48), `review-form.html`(L52)
  - `templates/product/join/complete.html`(L100), `form.html`(L120), `coupon.html`(L48), `signature.html`(L60), `terms.html`(L65), `terms-reader.html`(L39)
  - `templates/fx/exchange-calculator.html`(L108), `exchange-rate.html`(L62), `guide.html`(L574)

**2) 현재 로직**
- footer는 Thymeleaf 프래그먼트(`th:fragment="footer"`) 단일 정의이며, 위 21개 페이지가 `th:replace="~{footer :: footer}"`로 동일하게 가져다 쓴다(전역 공통).
- 구조는 3단: (1) 정책 링크 줄(`footer-policy`), (2) 본문(`footer-main`)에 브랜드 1열 + 카테고리 4열(`footer-col`: 금융상품/환율·환전/고객지원/회사), (3) 하단(`footer-bottom`)에 카피라이트 + FAMILY SITE `<select>`.
- 카테고리/FAMILY SITE 모두 정적 하드코딩이다. 모든 링크는 `href="#"`이고, FAMILY SITE 셀렉트는 `name="select"`만 있을 뿐 JS 핸들러나 `th:` 동적 바인딩이 코드에서 확인 안 됨(실제 이동 동작 없음).
- 그리드 레이아웃은 `fx-home.css`의 `.footer-grid`(`1.6fr 1fr 1fr 1fr 1fr`)로 5열 배치, 1040px/680px 미디어쿼리에서 3열→2열로 줄어든다.

**3) 핵심 코드 스니펫**

footer.html — 카테고리(`footer-col`) 영역 (L52~82):
```html
      <div class="footer-col">
        <h4>금융상품</h4>
        <a href="#">예금·적금</a>
        <a href="#">펀드·ETF</a>
        <a href="#">외화예금</a>
        <a href="#">MMF</a>
      </div>
      <div class="footer-col">
        <h4>환율·환전</h4>
        <a href="#">실시간 환율</a>
        <a href="#">환전 신청</a>
        <a href="#">환전 내역</a>
        <a href="#">환율 알림</a>
      </div>
      <div class="footer-col">
        <h4>고객지원</h4>
        <a href="#">공지사항</a>
        <a href="#">FAQ</a>
        <a href="#">1:1 문의</a>
        <a href="#">챗봇 상담</a>
      </div>
      <div class="footer-col">
        <h4>회사</h4>
        <a href="#">소개</a>
        <a href="#">이용약관</a>
        <a href="#">개인정보처리방침</a>
        <a href="#">채용</a>
      </div>
```

footer.html — FAMILY SITE 영역 (L88~100):
```html
  <div class="footer-bottom">
    <div class="footer-inner footer-bottom-inner">
      <span class="copyright">COPYRIGHT ⓒ 2025 BUSANBANK ALL RIGHTS RESERVED</span>
      <select class="family-site" name="select">
        <option value="site1">FAMILY SITE</option>
        <option value="site2">행복한 BNK</option>
        <option value="site3">부산은행금융역사관</option>
        <option value="site4">BNK부산은행갤러리</option>
        <option value="site5">BNK부산은행조은극장</option>
        <option value="site6">부은동우회</option>
      </select>
    </div>
  </div>
```

fx-home.css — 카테고리/FAMILY SITE 스타일 (L349~369):
```css
.footer-col h4{font-size:14px;font-weight:700;color:var(--text);margin:0 0 16px;}
.footer-col a{
  display:block;font-size:13.5px;color:var(--text-sub);
  padding:6px 0;transition:color var(--t) var(--ease);
}
.footer-col a:hover{color:var(--violet);}
/* 카피라이트 */
.footer-bottom-inner{
  display:flex;align-items:center;justify-content:space-between;
  padding:20px 32px;
}
.family-site{
  background:var(--surface);
  color:var(--text-sub);border:1px solid var(--border);
  border-radius:8px;padding:9px 14px;font-size:12.5px;cursor:pointer;
  outline:none;
}
.family-site option{background:var(--surface);color:var(--text);}
```

**4) 수정 시 영향 범위**
- footer.html은 단일 프래그먼트이므로, FAMILY SITE/카테고리 마크업을 고치면 위 **21개 페이지 전체**에 즉시 반영된다(전역 영향).
- 클래스명(`footer-col`, `footer-grid`, `family-site`, `footer-bottom-inner`)을 변경하면 `fx-home.css`의 해당 셀렉터(L328~329 그리드, L349~354 카테고리, L358~369 FAMILY SITE)와 반응형(L376 `.footer-grid`, L408 `.footer-grid`, L409 `.footer-bottom-inner`) 스타일이 모두 깨지므로 CSS도 함께 수정 필요.
- 카테고리 컬럼 개수를 늘리거나 줄이면 `.footer-grid`의 `grid-template-columns:1.6fr 1fr 1fr 1fr 1fr`(5열 = 브랜드1 + 카테고리4) 값과 반응형 분기(3열/2열)를 같이 조정해야 레이아웃이 어긋나지 않는다.
- FAMILY SITE 셀렉트에 실제 이동 기능을 붙이려면 현재 JS 핸들러가 코드에서 확인 안 됨 → 신규 스크립트 추가가 필요하며, `href="#"` 정적 링크들도 마찬가지로 동작 코드 없음.
- `fx-home.css`는 footer 외 홈/헤더/대시보드 등 광범위한 전역 스타일 파일이므로, footer 관련 셀렉터만 정확히 한정해 수정해야 다른 영역에 부작용이 없다.

### 4. [메인] 실시간 환율 아래 영역

#### 1) 관련 파일 경로 (상대경로, 프로젝트 루트 `spring-server/fx_bank` 기준)
- `src/main/resources/templates/body.html` — 메인 페이지 본문 fragment(`th:fragment="body"`), "실시간 환율" 섹션 포함
- `src/main/resources/static/js/fx-home.js` — 실시간 환율 카드/히어로 캐러셀 렌더링(`/api/fx/rates/main-detail` 호출)
- `src/main/resources/css/fx-home.css` — body.html이 링크하는 스타일(`rates-section`, `quick-section`, `products` 등)
- `src/main/resources/templates/fx/guide.html` — "외환 상품·서비스 안내" 전체 페이지(외화예금/수수료/**FAQ**/법규 탭 포함). 단, 독립 페이지(fragment 아님)
- `src/main/resources/templates/header.html` — GNB 메뉴에 `외환상품`, `FAQ`(`/fx/guide#faq`) 링크
- `src/main/resources/templates/footer.html` — 고객지원 영역에 `FAQ`(href="#", 미연결)

#### 2) 현재 로직
"실시간 환율" 섹션(`section.rates-section`)은 빈 컨테이너 `<div id="rateCards">`(스켈레톤)만 두고, `fx-home.js`가 `DOMContentLoaded` 시 `/api/fx/rates/main-detail`를 fetch해 카드를 동적 렌더링한다. 그 **바로 아래**는 `body.html` 내에서 순서대로 ① "빠른 이용"(`quick-section`, 환전계산기/환율조회/외환안내 3개 퀵카드) → ② "추천 상품"(`products`, 하드코딩된 외화예금/적금 4개 카드) → ③ "AI 배너"(`ai-section`) → ④ AI 챗봇 위젯 순으로 정적 배치돼 있다. "실시간 환율" 바로 아래에 독립된 "외환 상품" 또는 "FAQ" 섹션은 **메인에 없음**.

#### 3) 핵심 코드 스니펫

`templates/body.html` (49~86행) — 실시간 환율 섹션과 그 바로 아래(빠른 이용):
```html
<!-- ===================== 실시간 환율 ===================== -->
<section class="section rates-section">
  <div class="section-inner">
    <div class="section-head">
      <div>
        <span class="eyebrow">LIVE RATES</span>
        <h2 class="section-title">실시간 환율</h2>
      </div>
      <a th:href="@{/fx/exchange-rate}" class="section-more">전체 환율 보기 ›</a>
    </div>
    <div class="rate-cards" id="rateCards">
      <div class="rate-card-skel">환율 불러오는 중…</div>
    </div>
  </div>
</section>

<!-- ===================== 빠른 이용 ===================== -->
<section class="section quick-section">
  <div class="section-inner">
    <div class="quick-grid">
      <a class="quick-card qc-violet" th:href="@{/fx/exchange-calculator}"> ... </a>
      <a class="quick-card qc-blue"   th:href="@{/fx/exchange-rate}"> ... </a>
      <a class="quick-card qc-mint"   th:href="@{/fx/guide}">
        <span class="qc-ico">📘</span>
        <div class="qc-text"><strong>외환 상품·서비스 안내</strong><span>환전·환율·외화예금·FAQ</span></div>
      </a>
    </div>
  </div>
</section>
```
→ 그 다음 순서: `<section class="section products">`(88행, 추천 상품 — 카드 4개 하드코딩) → `<section class="section ai-section">`(165행) → 챗봇 위젯(181행).

`static/js/fx-home.js` (137~153행) — rateCards 데이터 소스:
```js
document.addEventListener('DOMContentLoaded', function () {
  bind();
  fetch('/api/fx/rates/main-detail')
    .then(function (r) { return r.json(); })
    .then(function (res) {
      data = (res && res.data) ? res.data : [];
      ...
      renderCards(); renderFeatured();
    })
```

`templates/fx/guide.html` (441~448행) — FAQ는 이 페이지의 탭 패널로 존재:
```html
<!-- ===================== 5. FAQ ===================== -->
<section class="guide-panel" data-panel="faq">
  <div class="guide-section">
    <h2 class="guide-section-title">자주 묻는 질문 (FAQ)</h2>
    <details class="guide-faq">
      <summary>수입신용장 거래 관련 영수증 출력은 어떻게 하나요?</summary>
      <div class="guide-faq-body"> ... </div>
    </details>
    ...
```

#### "외환 상품" / "FAQ" 섹션 존재 여부 (templates 전체 검색 결과)
- **"외환 상품(외환 상품·서비스 안내)"**: `templates/fx/guide.html`에 전체 페이지로 존재. 단 이 파일은 `<html>`로 시작하고 `header :: header`를 replace 하는 **독립 완성 페이지**이며, 상단에 `th:fragment` 선언이 **없음** → 메인에서 그대로 끼워 넣을 수 있는 재사용 fragment/조각이 **아님**(현재는 링크(`/fx/guide`)로만 연결).
- **"FAQ"**:
  - `templates/fx/guide.html` 442행 `data-panel="faq"` 탭 패널(질문 다수, `<details class="guide-faq">` 구조)에 실콘텐츠로 존재 — **하지만 fragment 아님**.
  - `templates/header.html` GNB에 링크(`/fx/guide#faq`)로 노출.
  - `templates/footer.html`에는 `FAQ` 텍스트 링크가 있으나 `href="#"`로 **미연결**.
- 즉, 메인(body.html)에 바로 붙일 수 있는 **재사용 가능한 `th:fragment` 형태의 "외환 상품" 또는 "FAQ" 조각은 코드에서 확인 안 됨**. 콘텐츠 자체는 `fx/guide.html`에 있으나 전부 페이지 통째 형태.

#### 4) 수정 시 영향 범위
- `body.html`의 실시간 환율 섹션 아래에 새 섹션을 끼워 넣으면, 바로 뒤 `quick-section`/`products`/`ai-section`의 시각적 순서·간격에 영향. 특히 `<section class="section ...">` 공통 클래스와 `fx-home.css`의 섹션 간 마진/배경 교차 스타일에 의존하므로 CSS 동반 조정 필요.
- 실시간 환율 컨테이너 id(`rateCards`)·히어로 stage id(`heroFeature`)는 `fx-home.js`가 직접 참조 → id/구조 변경 시 JS 렌더 깨짐. 데이터는 `/api/fx/rates/main-detail` 엔드포인트에 의존.
- "외환 상품"/"FAQ"를 메인에 재사용하려면 `fx/guide.html`의 해당 `<section>`을 `th:fragment`로 분리(또는 별도 fragment 파일 신설)해야 하며, 그 경우 `guide.html` 자체 렌더(탭 JS `data-panel`/`guide-tab-btn` 로직)와 `fx-guide.css` 스타일을 함께 가져가야 메인에서 정상 표시됨.
- FAQ 링크 정합성: `header.html`(`/fx/guide#faq`)·`footer.html`(`href="#"`)이 FAQ를 가리키므로, FAQ 위치/앵커를 바꾸면 이 링크들도 함께 수정 필요.

(메인 `body.html`의 "실시간 환율" 바로 아래에 별도 "외환 상품" 또는 "FAQ" 섹션이 현재 배치돼 있다는 사실은 코드에서 확인 안 됨 — 아래는 빠른 이용 → 추천 상품 → AI 배너 순.)

### 5. [메인] 추천 상품 퀵 메뉴

#### 1) 관련 파일 경로
- `src/main/resources/templates/body.html` — 퀵 메뉴(`quick-section`, 65~86줄)와 추천 상품(`products`, 88~162줄) 마크업이 모두 여기 있음
- `src/main/resources/templates/fx-home.html` — 메인 페이지. `<div th:replace="~{body :: body}"></div>`로 body.html의 `body` fragment를 끼워넣음 (12줄)
- `src/main/java/com/example/bank/fx/controller/FxMainController.java` — 메인(`@GetMapping("/")` → `"fx-home"`) 및 퀵 메뉴가 링크하는 3개 라우트(`/fx/exchange-calculator`, `/fx/exchange-rate`, `/fx/guide`) 정의
- `src/main/resources/css/fx-home.css` — `quick-card`, `product-card` 등 스타일 (body.html 6줄에서 로드)

#### 2) 현재 로직
- 메인은 `FxMainController`가 `"fx-home"` 뷰를 반환하고, fx-home.html이 `body :: body` fragment를 `th:replace`로 포함한다.
- 질문의 "추천 상품 퀵 메뉴"는 body.html 안에서 사실상 두 블록으로 나뉜다. (1) "빠른 이용"(`quick-section`) — 3개 `quick-card` 링크, (2) "추천 상품"(`products`) — 4개 `product-card`.
- 두 블록 모두 **완전 정적 하드코딩**이다. Thymeleaf 반복(`th:each`)/모델 바인딩이 없고, 금리·평점·상품명(달러/유로/엔화 정기예금 등)·태그(인기/신규/추천)가 텍스트로 박혀 있다. 컨트롤러는 추천 상품 데이터를 모델에 넣지 않는다.
- 퀵 메뉴 3장은 각각 `@{/fx/exchange-calculator}`, `@{/fx/exchange-rate}`, `@{/fx/guide}`로 이동하며, 이 3개 라우트는 `FxMainController`에 실제 존재한다.

#### 3) 핵심 코드 스니펫

퀵 메뉴 (`body.html` 65~86줄):
```html
  <!-- ===================== 빠른 이용 ===================== -->
  <section class="section quick-section">
    <div class="section-inner">
      <div class="quick-grid">
        <a class="quick-card qc-violet" th:href="@{/fx/exchange-calculator}">
          <span class="qc-ico">🧮</span>
          <div class="qc-text"><strong>환전계산기</strong><span>우대율 적용 예상 금액</span></div>
          <span class="qc-arrow">→</span>
        </a>
        <a class="quick-card qc-blue" th:href="@{/fx/exchange-rate}">
          <span class="qc-ico">📈</span>
          <div class="qc-text"><strong>환율조회</strong><span>통화별 최신 고시환율</span></div>
          <span class="qc-arrow">→</span>
        </a>
        <a class="quick-card qc-mint" th:href="@{/fx/guide}">
          <span class="qc-ico">📘</span>
          <div class="qc-text"><strong>외환 상품·서비스 안내</strong><span>환전·환율·외화예금·FAQ</span></div>
          <span class="qc-arrow">→</span>
        </a>
      </div>
    </div>
  </section>
```

추천 상품 (`body.html` 88~114줄, 카드 4개 중 첫 카드 + 헤더만 인용):
```html
  <!-- ===================== 추천 상품 ===================== -->
  <section class="section products">
    <div class="section-inner">
      <div class="section-head">
        <div>
          <span class="eyebrow">추천 상품</span>
          <h2 class="section-title">나에게 딱 맞는 외화상품</h2>
        </div>
        <a th:href="@{/product/list}" class="section-more">전체 상품 보기 ›</a>
      </div>
      <div class="product-grid">
        <article class="product-card">
          <div class="product-top">
            <span class="product-ico">💵</span>
            <span class="tag tag-purple">인기</span>
          </div>
          <span class="product-cat">외화예금</span>
          <h3 class="product-name">달러 정기예금</h3>
          <p class="product-rate">3.8%</p>
          <p class="product-sub">연 금리 · 12개월</p>
          <div class="product-foot">
            <span class="product-score">★ 4.8</span>
            <a th:href="@{/product/list}" class="product-link">가입하기 →</a>
          </div>
        </article>
        ... (멀티 외화적금 4.2% / 유로 정기예금 3.1% / 엔화 정기예금 2.4% 카드 동일 구조)
```

메인 조립 (`fx-home.html` 11~13줄):
```html
	<div th:replace="~{header :: header}"></div>
	<div th:replace="~{body :: body}"></div>
	<div th:replace="~{footer :: footer}"></div>
```

#### 4) 수정 시 영향 범위
- **링크 의존성(퀵 메뉴)**: 3개 `quick-card`는 `/fx/exchange-calculator`, `/fx/exchange-rate`, `/fx/guide`에 직접 연결됨. 이 경로/카드를 바꾸면 `FxMainController`의 동일 매핑(21~36줄) 및 각 대상 템플릿(`fx/exchange-calculator.html`, `fx/exchange-rate.html`, `fx/guide.html`)과 정합성을 같이 봐야 함. HERO 섹션(body.html 29~30줄)도 같은 두 경로를 버튼으로 재사용함.
- **링크 의존성(추천 상품)**: 모든 추천 카드의 "가입하기"와 헤더 "전체 상품 보기"가 전부 `@{/product/list}` 한 곳으로만 연결됨(개별 상품 상세로 안 감). 개별 상품 라우팅으로 바꾸려면 `product/list.html` 또는 `product/detail.html` 및 해당 컨트롤러까지 손봐야 함.
- **데이터 정합성**: 추천 상품 4개(상품명/금리/평점/태그)는 하드코딩이라 실제 DB의 외화상품 목록과 연동되어 있지 않음. 동적화(예: `th:each` + 모델 추가)하려면 `FxMainController.fxHome()`에서 모델 주입이 필요하고, 그렇지 않은 한 카드 수정은 body.html 텍스트만 고치면 됨(다른 화면엔 영향 없음).
- **스타일 의존성**: `quick-section/quick-grid/quick-card/qc-*`, `products/product-grid/product-card/tag-*` 클래스는 `css/fx-home.css`에 정의됨. 클래스명 변경 시 CSS 동반 수정 필요.
- **포함 구조**: body.html은 `body` fragment 단위로 fx-home.html에 삽입됨. fragment 이름/구조를 바꾸면 fx-home.html 12줄의 `~{body :: body}`도 영향. (index.html은 body.html을 참조하지 않아 영향 없음 — 코드에서 확인됨.)

(참고: `admin/mbtiService.html`에도 "추천" 문자열이 있으나 메인 퀵 메뉴와 무관한 관리자 화면임.)

### 6. [메인] 리뷰 익명화

**1) 관련 파일 경로** (상대경로)
- `src/main/resources/templates/product/reviews.html` — 리뷰 전체 목록 페이지 (작성자명 렌더)
- `src/main/resources/templates/product/detail.html` — 상품 상세 내 리뷰 2개 미리보기 (작성자명 렌더)
- `src/main/resources/templates/product/review-form.html` — 리뷰 작성 폼 (작성자명 미표시, 관련 없음)
- `src/main/java/com/example/bank/product/controller/ProductController.java` — `reviews`/`hasMoreReviews` 모델 주입, reviews.html·detail.html 렌더
- `src/main/java/com/example/bank/product/controller/ProductReviewController.java` — 리뷰 작성 API(`/api/products/{productNo}/reviews`), 조회와 무관
- `src/main/java/com/example/bank/product/service/ProductServiceImpl.java` — `getProductReviews()` → `productDao.selectProductReviews()`
- `src/main/java/com/example/bank/product/dao/IProductDao.java` — `selectProductReviews` 매퍼 인터페이스
- `src/main/java/com/example/bank/product/dto/ProductReviewDto.java` — `userName` 필드 보유
- `src/main/resources/mapper/mybatis/product.xml` — `selectProductReviews` SQL (작성자명 실제 출처)

**2) 현재 로직**
`ProductController`가 `getProductReviews(productNo)`로 리뷰 리스트를 조회해 `reviews` 모델로 넘긴다(상세는 `.limit(2)`, 전체목록 페이지는 전체). 템플릿은 `th:each`로 돌며 `${review.userName}`을 그대로 출력한다. `userName` 값은 더미/하드코딩이 아니라 매퍼 SQL `selectProductReviews`에서 `product_reviews r JOIN users u`로 조인해 **`u.name_ko AS user_name`** (회원 실명)을 가져온 것이다. 즉 작성자 이름은 DB의 실명 컬럼에서 비롯된다.

**3) 핵심 코드 스니펫**

작성자명의 실제 출처 — `mapper/mybatis/product.xml` (selectProductReviews):
```xml
SELECT
    r.review_no,
    r.user_no,
    r.product_no,
    u.name_ko AS user_name,
    r.review_text,
    r.rating,
    r.created_dt,
    r.updated_dt
FROM product_reviews r
JOIN users u
  ON r.user_no = u.user_no
WHERE r.product_no = #{productNo}
ORDER BY r.created_dt DESC
```
참고: `product_reviews` 테이블 자체에는 작성자명 컬럼이 없고(`insertProductReview`는 `review_no, user_no, product_no, review_text, rating, created_dt, updated_dt`만 저장), 이름은 조회 시 `users.name_ko` 조인으로만 채워진다.

렌더 위치 1 — `templates/product/reviews.html` (58~64행):
```html
<article class="guide-card review" th:each="review : ${reviews}">
    <p class="review-meta">
        <span class="review-author" th:text="${review.userName}">작성자</span>
        <span class="review-rating"><span th:text="${review.rating}">5</span>점</span>
        <span th:text="${#dates.format(review.createdDt, 'yyyy-MM-dd')}">2026-06-22</span>
    </p>
    <p class="review-text" th:text="${review.reviewText}">리뷰 내용</p>
</article>
```

렌더 위치 2 — `templates/product/detail.html` (171~173행):
```html
<article class="review" th:each="review : ${reviews}">
    <p><strong th:text="${review.userName}">작성자</strong> · <span th:text="${review.rating}">4.8</span>점 · <span th:text="${#dates.format(review.createdDt, 'yyyy-MM-dd')}">2026-06-16</span></p>
    <p th:text="${review.reviewText}">리뷰 내용</p>
</article>
```

DTO 필드 — `ProductReviewDto.java`:
```java
private Long userNo;     // 사용자 번호
private String userName; // 작성자 이름
```

**익명화 방법별 결론:**
- 작성자 이름은 `users.name_ko`(실명)에서 옵니다. `product_reviews` 테이블에 작성자명 컬럼은 없습니다.
- **프론트만으로 가능**: `reviews.html`·`detail.html` 두 곳의 `${review.userName}` 출력 부분을 고정 문자열(예: "익명")로 바꾸거나, 이름 일부 마스킹용 Thymeleaf 표현으로 가공하면 됩니다. 백엔드 변경 없이 표시만 익명화 가능.
- **다만 부분 마스킹(예: 홍*동)을 서버에서 일관 처리하려면** 매퍼 SQL의 `u.name_ko AS user_name`을 마스킹 표현식으로 바꾸거나 Service(`getProductReviews`)에서 `userName`을 가공하는 백엔드 수정이 더 안전합니다(JSON으로 실명이 클라이언트에 내려가는 것 자체를 막으려면 백엔드 처리가 필수).
- 단, 두 렌더 페이지 모두 SSR(Thymeleaf)이며 `userName`이 별도 JSON API로 노출되는 경로는 코드에서 확인되지 않음(리뷰 조회 REST 엔드포인트 없음, 작성 API만 존재). 따라서 "표시 익명화"만 목표라면 프론트(템플릿 2곳)만으로 충분합니다.

**4) 수정 시 영향 범위**
- **템플릿만 수정 시**: `reviews.html`, `detail.html` 두 곳을 동일하게 바꿔야 함(한 곳만 바꾸면 불일치). 다른 템플릿에는 리뷰 작성자 표시 없음.
- **매퍼(`product.xml` selectProductReviews) 수정 시**: 같은 SQL을 쓰는 모든 호출부(`getProductReviews` → detail.html 미리보기 + reviews.html 전체목록)에 동시 적용됨. `users` 조인을 제거/마스킹하면 두 화면 모두 영향. 다른 select(avg_rating, review_count 집계 쿼리)는 `users` 조인을 쓰지 않으므로 영향 없음.
- **DTO(`ProductReviewDto.userName`) 수정 시**: 해당 DTO를 참조하는 `ProductServiceImpl.getProductReviews`, `IProductDao.selectProductReviews`, 두 템플릿 모두 영향. 작성 API(`ProductReviewController`/`insertProductReview`)는 `userName`을 저장/사용하지 않으므로 영향 없음.
- **avgRating·reviewCount 등 평점 집계**(detail.html 118행)는 작성자명과 무관하므로 익명화와 별개로 영향받지 않음.

### 7·8. [로그인] 간편로그인 텍스트 / 비밀번호 찾기

**1) 관련 파일 경로**
- `src/main/resources/templates/login.html` — 셸 페이지. `header`/`auth-body`/`footer` fragment를 `th:replace`로 조립만 함.
- `src/main/resources/templates/auth-body.html` — 실제 로그인 폼 마크업(간편로그인 버튼·비밀번호 찾기 링크가 여기 있음). `<main th:fragment="authBody">`.
- `src/main/resources/static/js/auth.js` — 로그인/회원가입 동작 스크립트. 간편로그인·비밀번호 찾기 관련 핸들러는 **없음**.
- `src/main/resources/static/css/fx-auth.css` — `.social-*`, `.link-find` 스타일(색상만, 동작 없음).

**2) 현재 로직**
- `login.html`은 본문을 `auth-body.html`의 `authBody` fragment로 끼워 넣어 렌더링한다.
- 간편로그인은 카카오/네이버/애플 3개의 `<button type="button">`로, 텍스트 라벨이 버튼 요소 자체의 텍스트 노드("카카오"/"네이버"/"애플")이다. CSS로 브랜드 색만 입혔고 `onclick`·`data-*`·JS 이벤트 바인딩이 전혀 없어 클릭해도 아무 동작이 없다(순수 UI 더미).
- "비밀번호 찾기"는 `<a href="#" class="link-find">`로, href가 `#`이고 JS 핸들러도 없어 페이지 상단 이동 외 실제 비밀번호 재설정 기능에 연결돼 있지 않다.
- OAuth 엔드포인트/리다이렉트(`/oauth*`, kakao/naver SDK 호출 등)는 `src/main` 전체 grep에서 마크업/CSS 외 **코드에서 확인 안 됨**.

**3) 핵심 코드 스니펫**

`templates/auth-body.html` (66~82행) — 비밀번호 찾기 링크 + 간편로그인 버튼:
```html
<div class="login-meta">
  <label class="checkbox">
    <input type="checkbox" id="keepLogin">
    <span>로그인 상태 유지</span>
  </label>
  <a href="#" class="link-find">비밀번호 찾기</a>
</div>

<button type="button" id="loginBtn" class="btn btn-primary btn-block">로그인</button>

<div class="divider"><span>간편 로그인</span></div>

<div class="social-row">
  <button type="button" class="social social-kakao">카카오</button>
  <button type="button" class="social social-naver">네이버</button>
  <button type="button" class="social social-apple">애플</button>
</div>
```

`static/js/auth.js` (16~24행) — DOMContentLoaded 초기화 목록. 간편로그인/비밀번호찾기 init 함수가 등록돼 있지 않음(바인딩 부재 증거):
```js
document.addEventListener("DOMContentLoaded", function () {
  initTabs();
  initPasswordToggle();
  initLogin();
  initRegister();
  initIdCardOcr();
  initTermsAgreement();
  initPhoneHyphen();
});
```
- `auth.js` 내 `social-kakao`/`social-naver`/`social-apple`/`link-find`/`oauth` 문자열은 전혀 등장하지 않음. 즉 텍스트 라벨은 각 `<button>`/`<a>` 요소의 텍스트일 뿐이며 OAuth 연결 없음.

`static/css/fx-auth.css` (198~200행) — 동작 아닌 색상만 부여:
```css
.social-kakao{background:#FEE500;color:#3A2929;}
.social-naver{background:#03C75A;color:#fff;}
.social-apple{background:#111;color:#fff;}
```

**4) 수정 시 영향 범위**
- 간편로그인 버튼/비밀번호 찾기 링크의 **라벨 텍스트·구조 변경**은 `auth-body.html` 한 곳만 고치면 됨. 단 이 fragment는 `login.html`(및 회원가입 진입 시 동일 fragment)에서 공유되므로 로그인/회원가입 화면 양쪽에 함께 반영됨.
- 클래스명(`.social`, `.social-kakao/naver/apple`, `.link-find`, `.social-row`, `.divider`)을 바꾸면 `fx-auth.css`의 해당 선택자(150~151, 191~200, 315행 반응형 포함)도 같이 수정해야 스타일이 깨지지 않음.
- 실제 OAuth/비밀번호 재설정 기능을 붙이려면 `auth.js`에 신규 핸들러 추가 + 서버측 인증 컨트롤러/엔드포인트가 필요하나, 현재 `src/main`에는 대응 백엔드가 **코드에서 확인 안 됨**(신규 구현 대상).
- 로그인 버튼(`#loginBtn`)은 `auth.js`의 `initLogin()`/`handleLogin()`(82~121행)과 `/api/auth/login`에 연결돼 있으므로, 간편로그인/비밀번호찾기와 달리 이 버튼 영역은 건드릴 때 로그인 동작에 영향이 있으니 주의.

### 9. [상품가입] 옵션 선택 중복 출력 ★버그

#### 1) 관련 파일 경로 (상대경로)
- `src/main/resources/templates/product/join/form.html` — 옵션 선택 UI(통화 select, 가입기간/금리 select) 및 인라인 JS 전부
- `src/main/java/com/example/bank/product/controller/ProductJoinPageController.java` — `form()` 핸들러가 모델에 옵션 리스트 주입
- `src/main/java/com/example/bank/product/service/ProductServiceImpl.java` — `getProductCurrencies()`, `getProductRates()`
- `src/main/java/com/example/bank/product/dao/IProductDao.java` — `selectProductCurrencies`, `selectProductRates`
- `src/main/resources/mapper/mybatis/product.xml` — 위 두 쿼리 실제 SQL (라인 176~204)
- (참고) `src/main/resources/templates/product/join/signature.html`, `terms.html` — 같은 form 데이터를 표시만 함(옵션 렌더링 아님)

#### 2) 현재 로직 (렌더링 방식)
옵션 데이터는 전적으로 **서버 → 모델 → Thymeleaf `th:each`** 경로다. `ProductJoinPageController.form()`이 `currencies`(통화), `rates`(가입기간/금리)를 모델에 **각 1회만** add하고(`ProductJoinPageController.java` 64~66행), `form.html`이 `<option th:each="currency : ${currencies}">`(74행)와 `<option th:each="rate : ${rates}">`(93행)로 한 행당 옵션 하나를 그린다. JS는 통화/금리 옵션을 만들지 않는다 — JS가 동적으로 그리는 select는 출금계좌(`#withdrawalAccountNo`)뿐이며, 그것도 `select.innerHTML = ...`로 매번 초기화 후 append라 중복되지 않는다(216, 226~231행).

#### 3) 핵심 코드 스니펫

옵션 렌더링 (form.html 70~98행, th:each는 단일 루프·중첩 없음):
```html
<select id="currencyCode" class="guide-select" required>
    <option value="">선택</option>
    <option th:each="currency : ${currencies}"
            th:value="${currency.currencyCode}"
            th:text="${currency.currencyCode + ' - ' + currency.currencyName}">USD</option>
</select>
...
<select id="rateNo" class="guide-select" required>
    <option value="">선택</option>
    <option th:each="rate : ${rates}"
            th:value="${rate.rateNo}"
            th:attr="data-period=${rate.periodMonth},data-rate=${rate.interestRate}"
            th:text="${rate.periodMonth + '개월 / ' + rate.interestRate + '%'}">12개월 / 4.5%</option>
</select>
```

모델 주입 (ProductJoinPageController.java 64~66행, 각 리스트 1회만 add):
```java
model.addAttribute("product", product);
model.addAttribute("currencies", productService.getProductCurrencies(productNo));
model.addAttribute("rates", productService.getProductRates(productNo));
```

실제 SQL (product.xml 192~204행, 단일 테이블·JOIN 없음 → SQL 레벨 행 증식 없음):
```xml
<select id="selectProductRates" resultType="com.example.bank.product.dto.ProductRateDto">
    SELECT rate_no, product_no, period_month, interest_rate
    FROM product_rates
    WHERE product_no = #{productNo}
    ORDER BY period_month
</select>
```
(`selectProductCurrencies`도 동일하게 `product_currencies` 단일 SELECT, 176~188행)

#### 4) 원인 분석 (★중복 후보 — 코드 근거와 함께)

코드상 렌더링 파이프라인은 깨끗하다. 다음 후보들은 **코드 근거로 배제**된다:
- **th:each 중첩** — 배제. 각 select 안에 단일 `th:each`만 존재(74, 93행), 바깥 루프 없음.
- **모델에 같은 값 두 번 add** — 배제. `form()`에서 `currencies`/`rates` 각각 1회만 add(64~66행).
- **fragment 중복 include** — 배제. include는 `header`/`footer` fragment뿐(27, 120행)이고 옵션 영역은 fragment가 아님.
- **JS가 이벤트마다 두 번 그림 / DOMContentLoaded 중복 바인딩** — 배제. 통화·금리 옵션을 JS가 그리지 않음. `<script>` 블록은 1개(122행)뿐, DOMContentLoaded 자체가 없음. JS로 그리는 건 출금계좌 select뿐인데 `innerHTML` 리셋 후 append라 누적 안 됨(216행). `currencyCode`의 change 리스너도 `if (requiresWithdrawalAccount)` 가드 안에서 1회만 바인딩(236~239행).
- **SQL JOIN으로 인한 행 증식** — 배제. 두 쿼리 모두 단일 테이블 SELECT, JOIN 없음(product.xml 176~204행).

**가장 유력한 후보: 데이터 리스트 자체의 중복(= `product_rates` / `product_currencies` 테이블에 동일 상품에 대해 중복 행이 존재).**
근거: 렌더링·모델·JS 어디에도 행을 증식시키는 코드가 없으므로, 화면에 옵션이 중복으로 보인다면 `selectProductRates`/`selectProductCurrencies`가 반환하는 List 원소가 이미 중복이라는 뜻이다. SQL에 `DISTINCT`가 없고 `WHERE product_no = #{productNo}`만 거르므로, 같은 `product_no`에 `(period_month, interest_rate)`(또는 `currency_code`)가 똑같은 행이 2건 이상 적재되어 있으면 그대로 옵션 2개가 출력된다. 특히 금리는 `ORDER BY period_month`만 적용돼 "12개월 / 4.5%"가 두 번 나란히 나오는 전형적 증상이 된다.

보강 근거: 해당 rate/currency 데이터를 관리하는 admin INSERT 매퍼가 없다(`IAdminProductDao.xml`에 `product_rates`/`product_currencies` 키워드 검색 결과 없음). 즉 시드/수동 적재 데이터일 가능성이 높아 중복 INSERT가 끼어들 여지가 크다.

**확인 방법 / 수정 방향(읽기 전용 분석 범위):**
- 진단 SQL 예: `SELECT product_no, period_month, interest_rate, COUNT(*) FROM product_rates GROUP BY product_no, period_month, interest_rate HAVING COUNT(*) > 1;` (통화는 `currency_code` 기준 동일 패턴) — 1건이라도 나오면 데이터 중복 확정.
- 데이터가 실제 중복이면 근본 수정은 DB 중복 행 정리 + 유니크 제약 추가가 정석. SQL을 못 건드리는 임시 방어로는 `selectProductRates`에 `DISTINCT`/그룹핑 또는 `th:each`에 중복 제거 로직을 넣는 방법이 있으나, 이는 증상만 가린다.

**주의(코드에서 확인 안 됨):** 실제 DB 행의 중복 여부는 코드만으로는 확인 불가. 다만 코드 경로상 중복을 만들 다른 원인이 전부 배제되므로, 데이터 레벨 중복이 사실상 유일하게 남는 후보다.

**수정 시 영향 범위:** `selectProductRates`/`selectProductCurrencies` 쿼리는 `getProductRates`/`getProductCurrencies`를 통해 `form.html`(가입 옵션) 외에 상품 상세(`ProductController`/`detail.html` 계열, `getProductDetail`과 함께 노출되는 금리·통화 표시)에서도 쓰일 수 있으므로, 쿼리에 `DISTINCT`/그룹핑을 넣으면 그 화면들의 출력에도 동일하게 영향을 준다. DB에서 중복 행을 삭제하는 방식이면 금리/통화를 참조하는 모든 화면과 가입 시 `rateNo`로 저장되는 가입 로직(`ProductJoinService`/`form` API)에 영향이 갈 수 있어, 어느 `rate_no`를 남길지(가입 이력의 FK 정합성) 확인이 필요하다.

### 10. [상품가입] 가입 버튼 반응형에서 안 뜸 ★버그

#### 1) 관련 파일 경로 (상대경로, 2차 루트 기준)
- `src/main/resources/templates/product/list.html` — 목록 표의 "가입하기" 버튼(`.join-button`), 인라인 `<style>`, 클릭 JS
- `src/main/resources/templates/product/detail.html` — 상세 하단 "가입하기" 버튼(`#joinBtn`), 클릭 JS
- `src/main/resources/static/css/fx-guide.css` — `.guide-table-wrap`/`.guide-table`(표 가로스크롤·min-width), `.guide-actions`, 반응형 `@media`
- `src/main/resources/static/css/fx-home.css` — 공용 `.btn`/`.btn-primary`, 반응형 `@media`(헤더/그리드만 처리)

#### 2) 현재 로직
- 목록(list.html): 상품들이 `<table class="guide-table">`(11개 컬럼)로 렌더되고, "가입" 컬럼이 맨 마지막(11번째). 표는 `.guide-table-wrap`(가로스크롤) 안에 있고 `.guide-table`에 `min-width:520px`가 걸려 있다. JS는 `.join-button`마다 click 핸들러만 부착(숨김/조건부 렌더 없음).
- 상세(detail.html): 본문 맨 아래 `.guide-actions`(flex) 안에 `#joinBtn`이 항상 렌더되며, 클릭 시 토큰 확인 후 `/product/join/{no}/terms`로 이동.

#### 3) 핵심 코드 스니펫

list.html — 가입 버튼이 표의 마지막 셀(11번째 컬럼)에 위치:
```html
<th>상품번호</th>...<th>상세</th><th>가입</th>   <!-- 11개 컬럼, '가입'이 끝 -->
...
<td>
    <button type="button" class="join-button"
            th:attr="data-product-no=${product.productNo},data-product-type=${product.productType}">가입하기</button>
</td>
```

fx-guide.css — 표 래퍼 가로스크롤 + 표 최소폭 520px (★버그 핵심):
```css
.guide-table-wrap { overflow-x: auto; margin: 6px 0 16px; -webkit-overflow-scrolling: touch; }
.guide-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13.6px;
    min-width: 520px;
}
.guide-table thead th { ... white-space: nowrap; }   /* 헤더 줄바꿈 금지 → 표 폭 더 커짐 */
```

fx-guide.css — 반응형 `@media`: 표/가입버튼을 건드리는 규칙이 없음(그리드·hero·패딩만):
```css
@media (max-width: 680px) {
    .guide-hero { padding: 40px 0; }
    .guide-section { padding: 22px 18px 16px; }
    .guide-form, .guide-result-grid { grid-template-columns: 1fr; }
}
```

detail.html — 상세의 가입 버튼(flex 컨테이너, 숨김 규칙 없음):
```html
<div class="guide-actions">   <!-- .guide-actions { display:flex; gap:10px } -->
    <a class="btn btn-light" th:href="@{/product/list}">상품 목록으로</a>
    <button type="button" id="joinBtn" class="btn btn-primary">가입하기</button>
</div>
```

#### 4) 원인 분석 (코드 근거 기반, 후보별)

가장 유력한 1순위 — list.html 목록 표의 가로 오버플로로 가입 컬럼이 화면 밖:
- `.guide-table`에 `min-width:520px`가 강제되어 있고, 실제로는 11개 컬럼(상품번호/상품명/유형/통화/기본금리/최고금리/가입기간/최소금액/평점/상세/가입)이라 520px보다 훨씬 넓어진다. `.guide-table thead th { white-space: nowrap }`로 헤더가 줄바꿈도 안 돼 표 폭이 더 커진다.
- 부모 `.guide-table-wrap { overflow-x: auto }`라서 모바일 폭에서는 **표가 좌→우로 가로 스크롤** 되는데, "가입" 컬럼이 **맨 오른쪽 마지막 컬럼**이라 초기 뷰포트에는 거의 항상 화면 밖으로 잘려 안 보인다. 즉 "안 뜬다"기보다 **가로 스크롤하지 않으면 보이지 않는** 상태다. (display:none이 아니라 위치상 가려짐)
- 근거: 위 `.guide-table-wrap`/`.guide-table` CSS + 컬럼 순서(`<th>가입</th>`이 끝), 그리고 모바일 `@media (max-width:680px)`에 표/버튼 관련 규칙이 전무함.

2순위 후보(상세 detail.html 쪽) — 가능성 낮음:
- `#joinBtn`은 `.guide-actions`(display:flex) 안에 있고, fx-guide.css/fx-home.css 어디에도 `.btn`/`.guide-actions`/`#joinBtn`을 `display:none`·`visibility:hidden` 처리하는 미디어쿼리가 없다. 따라서 상세 페이지 버튼은 반응형에서 정상 노출되어야 함(여기가 버그면 다른 원인일 가능성).

배제된 후보(코드 근거로 아님):
- "미디어쿼리 display:none": fx-guide.css의 `display:none`은 `.guide-tabs::-webkit-scrollbar`/`.guide-panel`/`.guide-faq summary marker`뿐, 가입버튼과 무관. fx-home.css의 `display:none`은 `.gnb-search`/`.gnb-tools`/`.gnb-dropdown` 등 헤더 전용.
- "JS가 못 그림 / 조건부 숨김": list.html·detail.html JS 모두 `.join-button`/`#joinBtn`에 **click 핸들러만** 붙일 뿐 숨김·조건부 렌더 로직 없음. 버튼은 항상 DOM에 존재.
- "부모 overflow:hidden로 잘림": 표 래퍼는 `overflow-x:auto`(스크롤)지 hidden이 아님 → 완전 소실이 아니라 스크롤로 접근 가능.

권장 점검/수정 방향(읽기전용 조사 결과로서의 제안): 모바일에서 목록을 표 대신 카드형으로 전환하거나, "가입" 컬럼을 sticky 처리하거나, `.guide-table`의 `min-width`/컬럼수를 모바일에서 줄이는 `@media` 추가가 필요. 수정 시 영향 범위는 `.guide-table*` 규칙을 공유하는 다른 가이드/환율 표 화면(fx-guide.css의 `.guide-table-wrap` 사용처 전부)이며, 컬럼 구조를 바꾸면 list.html의 `colspan="11"`(빈 결과 행)도 함께 맞춰야 한다.

### 11. [상품가입] 정적→동적(애니메이션)

#### 1) 관련 파일 경로 (상대경로, 2차 루트 기준)
- `src/main/resources/templates/product/detail.html` — 가입 진입점(`#joinBtn` "가입하기" → `/product/join/{No}/terms`)
- `src/main/resources/templates/product/join/terms.html` — 1단계 약관 동의 (+ "이어서 가입" 복원 모달)
- `src/main/resources/templates/product/join/terms-reader.html` — 약관 정독(스크롤 끝까지 읽어야 버튼 활성화)
- `src/main/resources/templates/product/join/coupon.html` — 2단계 우대금리 쿠폰 선택
- `src/main/resources/templates/product/join/form.html` — 3단계 본인인증(OCR/휴대폰)+가입정보 입력
- `src/main/resources/templates/product/join/signature.html` — 4단계 전자서명(canvas)+요약 확인
- `src/main/resources/templates/product/join/complete.html` — 5단계 가입 완료 내역(정적 테이블)
- `src/main/resources/static/css/fx-guide.css` — 공용 레이아웃/토큰. 기존 애니 정의(`guideFade`, `.guide-card:hover`, `calcIn`/`calcPop`, `prefers-reduced-motion`) 위치
- `src/main/resources/static/css/fx-home.css` — `.btn`/`.btn-primary:hover` 등 공용 버튼 transition·`@keyframes hfIn` 정의

#### 2) 현재 로직 (렌더링/동작)
각 단계는 **독립된 전체 페이지**이며, 단계 이동은 모두 `location.href = '/product/join/{No}/...'` 풀 페이지 네비게이션으로 처리된다(SPA·전환 애니메이션 없음). 모든 단계가 공통 레이아웃 `guide-hero`(상단 보라색 그라데이션 헤더) + `guide-wrap > guide-section` 카드를 쓴다. **단계 진행 상태를 보여주는 스텝퍼/프로그레스 바는 코드에서 확인 안 됨**(각 페이지가 hero 제목으로만 현재 단계를 표시). 폼/쿠폰/약관 데이터는 fetch로 받아 JS가 `innerHTML`로 즉시 삽입한다(등장 애니메이션 없음). terms-reader는 `scroll` 이벤트로 정독 판정 후 버튼 활성화, signature는 canvas pointer 이벤트로 그리기, complete는 Thymeleaf 서버렌더 정적 테이블이다.

#### 3) 핵심 코드 스니펫

**(a) 단계 전환 = 풀 페이지 이동 (애니메이션 없음)** — `terms.html`
```js
location.href = `/product/join/${productNo}/coupon`;   // terms → coupon
```
`coupon.html`: `location.href = `/product/join/${productNo}/form`;` / `form.html`: `location.href = `/product/join/${productNo}/signature`;` / `signature.html`: `location.href = `/product/join/${productNo}/complete?subscriptionNo=...`;` — 모두 동일 패턴.

**(b) 쿠폰 옵션: 이미 transition/hover/선택 피드백 존재** — `coupon.html` `<style>`
```css
.coupon { ...; cursor: pointer; transition: border-color var(--t) var(--ease), background var(--t) var(--ease); }
.coupon:hover { border-color: var(--violet-soft); }
.coupon:has(input:checked) { border-color: var(--violet); background: var(--lavender); }
```
단, 목록 자체는 `couponList.innerHTML = body.data.map(...)`로 즉시 삽입 → 등장 애니메이션 없음.

**(c) 동적 삽입 지점(등장 효과 넣기 좋은 곳)** — `terms.html` / `coupon.html`
```js
// terms.html: 약관 행은 Thymeleaf th:each 서버렌더(정적)
// coupon.html: 쿠폰 카드는 JS로 즉시 삽입(애니메이션 없음)
couponList.innerHTML = body.data.map(coupon => `<label class="coupon">...`).join('');
```

**(d) 정독 판정 / 인증 상태 변경 — 시각 피드백 미약** — `form.html`
```js
function setOcrStatus(text, type){ status.textContent = text; status.className = 'status ' + (type||''); }
// .status.ok 색만 바뀜 — 인증 성공 시 체크/펄스 등 모션 없음
```
`terms-reader.html`: `if (canConfirm()) enableConfirm();` — 정독 완료 시 버튼 `disabled` 해제만(전환 모션 없음).

**(e) 이미 코드베이스에 있는 재사용 가능한 애니 자산** — `fx-guide.css`
```css
.guide-panel.is-active { display: block; animation: guideFade .2s var(--ease); }
@keyframes guideFade { from { opacity:0; transform: translateY(6px); } to { opacity:1; transform: translateY(0); } }
.guide-card:hover { box-shadow: var(--shadow); transform: translateY(-2px); }   /* 카드 호버 lift (form.html의 join-card가 이미 .guide-card 사용) */
.calc-result-panel.is-calc .calc-bd:nth-of-type(n) { animation: calcIn .42s var(--ease) backwards; animation-delay: ...; }  /* 순차 등장(stagger) 패턴 */
@media (prefers-reduced-motion: reduce){ ... animation: none; }   /* 접근성 가드 선례 존재 */
```
`fx-home.css`: `@keyframes hfIn{from{opacity:0;transform:translateX(10px);}...}`, `.btn-primary:hover{transform:translateY(-1px);box-shadow:var(--shadow-lg);}` (버튼 호버 모션은 이미 적용됨).

#### 동적 효과 넣을 만한 지점 정리
- **단계 전환**: 현재 풀 페이지 이동이라 전환 끊김. 페이지 진입 시 `guide-section`/`guide-hero`에 `guideFade`류 fade-up 적용(가장 저비용). 단계 표시 **스텝퍼/프로그레스 바 신규 추가**(terms→coupon→form→signature→complete 5단계, 코드에 미존재).
- **카드 호버**: `form.html`의 `.join-card`는 이미 `.guide-card`라 hover lift가 잠재 적용됨 — `coupon`/`term-row`에도 동일 lift 확장 가능.
- **스크롤 등장**: 현 코드에 IntersectionObserver/scroll-reveal **없음**. `detail.html`의 긴 섹션(기본정보·금리표·약관 아코디언·리뷰)과 `coupon` 카드 목록에 fade-up 순차 등장(`calcIn` stagger 패턴 재사용) 적용 여지.
- **옵션 선택 피드백**: 쿠폰 `:has(input:checked)`는 색만 변함 → 선택 시 scale/체크 펄스 추가. `form.html` `.guide-radio input:checked + span`도 색만 변함.
- **동적 삽입 등장**: `coupon.html`의 `innerHTML` 삽입 직후 각 `.coupon`에 stagger fade-in 부여.
- **상태 전환 모션**: OCR/휴대폰 인증 성공(`.status.ok`), terms-reader 정독완료 버튼 활성화, signature 완료 버튼 enable에 펄스/체크 모션.
- **완료 화면**: `complete.html` 정적 테이블 → 가입 성공 체크/컨페티·행 순차 등장.
- **주의(접근성)**: 추가 시 기존 선례대로 `@media (prefers-reduced-motion: reduce)` 가드 동반 권장.

#### 4) 수정 시 영향 범위
- **공용 CSS 토큰/클래스**: 모든 join 페이지가 `fx-home.css`+`fx-guide.css`를 동시 link하고 `.guide-hero/.guide-section/.guide-card/.btn/.guide-input/.guide-select/.guide-radio` 공용 클래스를 공유. 이 클래스에 애니/transition을 손대면 join 5단계뿐 아니라 `fx/exchange-calculator.html`, `fx/exchange-rate.html`, `fx/guide.html`, `product/detail.html`, `product/list.html`, 리뷰 페이지 등 **guide-* 레이아웃을 쓰는 전 페이지**에 동시 영향.
- **JS가 의존하는 클래스명은 변경 금지**: `terms.html`(`.term-row`/`.read-state`), `coupon.html`(`.coupon`/`.rate`/`.empty`, radio `name="couponNo"`), `form.html`(`.status`/`.ok`/`.error`, `className='status '+type`), `signature.html`(`canvas#signatureCanvas` 크기를 `getBoundingClientRect`로 읽음), `terms-reader.html`(`#reader` 높이/스크롤로 정독 판정). 애니용 wrapper/추가 클래스로 처리해야 안전(각 파일 주석에 "기능/클래스명 유지" 명시됨).
- **스텝퍼 신규 추가** 시 5개 파일(terms/coupon/form/signature/complete) 헤더 영역에 동일 마크업 삽입 필요 — 공용 fragment(header/footer처럼 `th:replace`)가 없으므로 5곳 개별 반영, 단계값 하드코딩 주의.
- **signature canvas / terms-reader scroll 영역**에 transform/animation을 직접 적용하면 좌표 계산(`getBoundingClientRect`)·`scrollHeight` 판정이 틀어질 수 있어 해당 요소 자체에는 모션 적용 지양.
- **`prefers-reduced-motion` 미동반 시** 기존 `calc-*` 가드와 정책 불일치 발생.

### 12. [반응형/CSS] a.gnb-link::after 보라색 줄 ★버그

**1) 관련 파일 경로** (상대경로, 2차 루트 기준)
- `src/main/resources/static/css/fx-home.css` — `.gnb-link::after` 정의(데스크톱 + `@media (max-width:680px)`), `--violet` 변수 정의
- `src/main/resources/templates/header.html` — GNB 마크업(`.gnb-item`/`.gnb-link`/`.gnb-dropdown`) 및 모바일 아코디언 토글 JS
- 그 외 `gnb-link` 정의 파일: 위 CSS 1개뿐(다른 css 파일에는 `.gnb-link::after` 정의 없음)

**2) 현재 로직**
- 데스크톱: `.gnb-link::after`는 `height:2px; background:var(--violet)`의 가로 **밑줄 호버 인디케이터**로, 평소 `transform:scaleX(0)`(숨김)이고 `.gnb-item:hover` 또는 `.gnb-link.is-active`일 때 `scaleX(1)`로 펼쳐진다.
- 모바일(`max-width:680px`): GNB가 브랜드 아래 전체폭 세로 리스트(아코디언)로 바뀐다. 밑줄용 `::after`는 `display:none`으로 끄고, 하위가 있는 항목(`.has-sub`)에 한해 `::after`를 `content:'▾'` 화살표로 **재정의**한다. JS가 모바일에서 상위메뉴 탭 시 `e.preventDefault()` 후 `.gnb-item`에 `.is-open`을 토글해 하위 드롭다운을 펼치고 ▾를 180° 회전시킨다.

**3) 핵심 코드 스니펫** (fx-home.css, 그대로 인용)

데스크톱 정의 (89–102행):
```css
.gnb-link{
  display:inline-block;padding:26px 18px;
  font-size:15.5px;font-weight:600;color:var(--text);
  position:relative;transition:color var(--t) var(--ease);
}
.gnb-link::after{
  content:"";position:absolute;left:18px;right:18px;bottom:18px;
  height:2px;background:var(--violet);
  transform:scaleX(0);transform-origin:left;transition:transform var(--t) var(--ease);
}
.gnb-item:hover .gnb-link,
.gnb-link.is-active{color:var(--violet);}
.gnb-item:hover .gnb-link::after,
.gnb-link.is-active::after{transform:scaleX(1);}
```

모바일 재정의 (`@media (max-width:680px)`, 390–396행):
```css
  .gnb-link{display:flex;align-items:center;justify-content:space-between;
            padding:13px 4px;font-size:16px;min-height:44px;line-height:1.4;}
  .gnb-link::after{display:none;}        /* 데스크톱 밑줄효과 제거 */
  .gnb-item.has-sub > .gnb-link::after{  /* 하위 있는 항목에 펼침(▾) 표시 */
    content:'▾';display:inline-block;font-size:12px;color:var(--text-sub);transition:transform var(--t) var(--ease);
  }
  .gnb-item.is-open > .gnb-link::after{transform:rotate(180deg);}   /* 열리면 ▴ */
```

header.html 토글 JS (175–179행):
```javascript
          link.addEventListener('click', function (e) {
            if (!gnbMq.matches) return;        // 데스크톱: 기존 동작 유지
            e.preventDefault();                // 모바일: 이동 대신 펼침/접힘
            item.classList.toggle('is-open');
          });
```

**원인 분석 (보라색 줄 후보 — 코드 근거)**

가장 유력한 단일 원인: **모바일 `::after` 무력화의 셀렉터 특이도(specificity) 불일치로, 하위 있는 항목에서 데스크톱 밑줄 `::after`가 다시 살아남.**

- 392행 `.gnb-link::after{display:none;}` 의 특이도는 (0,1,1).
- 393행 `.gnb-item.has-sub > .gnb-link::after{ content:'▾'; ... }` 의 특이도는 (0,2,1)로 **더 높다**. 이 규칙은 `display`를 지정하지 않으므로, `.has-sub` 항목의 `::after`는 392행의 `display:none`을 덮어쓰지 못한 채로 두는 게 아니라, **이 셀렉터가 매칭되면 `display:none`을 끄는 효과가 없다** — 즉 `display`는 392행 값(none)이 유지되어야 정상이다. 그러나 `content`/기타 속성은 393행이 이긴다. 문제는 다음 단계다.
- 화살표 규칙이 적용되는 `.has-sub` 항목에서, 이 `::after`는 데스크톱 블록(94–97행)의 `position:absolute; left:18px; right:18px; bottom:18px; height:2px; background:var(--violet); transform:scaleX(...)`을 **그대로 상속/유지**한다(모바일 블록은 `content`와 `color`, `font-size`만 덮어쓸 뿐 `position/height/background/transform/left/right/bottom`을 리셋하지 않음). 따라서 `.has-sub` 항목의 `::after`는 "▾ 글리프 + 여전히 `background:var(--violet)`인 absolute 박스 + `height:2px`"가 겹쳐 **보라색 2px 가로줄**로 보일 수 있다. 특히 `transform:scaleX(1)`이 켜지는 조건(아래)에서 줄이 드러난다.

이를 켜는 트리거(★보라색 줄이 "누를 때" 보이는 이유):
1. **`:hover` 잔존** — 101행 `.gnb-item:hover .gnb-link::after{transform:scaleX(1);}` 는 미디어쿼리 밖 전역 규칙이라 모바일에서도 살아있다. 모바일 블록은 이 hover 규칙을 끄지 않는다. 터치 디바이스에서 항목을 탭하면 `:hover`가 적용된 상태로 남는 경우가 많아, 데스크톱 밑줄 `::after`의 `scaleX(0)→scaleX(1)`이 발동되어 보라색 줄이 노출된다.
2. **`transform` 속성 충돌** — 396행 `.gnb-item.is-open > .gnb-link::after{transform:rotate(180deg);}` 는 화살표 회전 의도지만, 같은 `::after`가 데스크톱에서 부여한 `transform-origin:left`/`scaleX`와 같은 `transform` 채널을 공유한다. `is-open`이 아닌 hover만 걸린 `.has-sub` 항목에서는 97행/102행의 `scaleX(1)`이 남아 보라 박스가 가로로 펼쳐진다.

요약: 모바일 `.gnb-link::after{display:none}`가 (a) `.has-sub` 항목에서는 더 높은 특이도의 화살표 규칙 때문에 `::after`가 다시 렌더되며, (b) 그 화살표 규칙이 데스크톱의 `background:var(--violet); height:2px; position:absolute`를 리셋하지 않고, (c) 전역 `:hover ... scaleX(1)` 규칙이 모바일에서 꺼지지 않아, 탭/호버 시 보라색 밑줄이 그대로 드러나는 것이 핵심 원인이다.

**수정 시 영향 범위 / 권고 포인트**
- 393–394행 화살표 규칙에 `background:transparent; height:auto; position:static; transform:none;`(및 필요시 `left/right/bottom:auto`)를 명시해 데스크톱 밑줄 잔재를 완전히 리셋하면 같은 `::after`를 쓰는 모든 `.has-sub` 항목(외환상품/환율·환전/고객지원)에 영향.
- 또는 모바일 블록에서 hover 규칙을 무력화(예: `@media (max-width:680px){ .gnb-item:hover .gnb-link::after{transform:none;} }`)해야 하며, 이 경우 데스크톱 hover 밑줄 동작과 분리되는지 확인 필요(전역 99–102행과 상호작용).
- `transform`을 화살표 회전과 밑줄 scaleX가 공유하므로, 둘을 같은 의사요소에 얹은 구조 자체가 충돌 지점 — 화살표를 별도 의사요소(`::before`)나 별도 마크업으로 분리하면 근본 해결되나 header.html 마크업/CSS 양쪽 수정 필요.
- 영향받는 요소: 헤더 GNB 전 항목(데스크톱 호버 밑줄, 모바일 ▾ 화살표·아코디언). `--violet`(11행)은 다수 컴포넌트가 공유하므로 변수 자체는 건드리지 말 것.

(참고: `.gnb-link.is-active`는 CSS에 정의돼 있으나 header.html JS에서 `is-active`를 부여하는 코드는 코드에서 확인 안 됨 — 따라서 현 버그의 주 트리거는 `is-active`가 아니라 위의 `:hover` 잔존 + 화살표 규칙의 미리셋으로 판단됨.)

### 13. [챗봇] 다국어지원 텍스트

**1) 관련 파일 경로** (모두 상대경로, 루트: `spring-server/fx_bank/`)
- `src/main/resources/templates/body.html` — 챗봇 위젯 마크업(`#chatbot-widget`), "챗봇 상담 시작" 버튼(`#chatbot-open`), **"다국어 지원" 텍스트(173줄)**, 챗봇 동작 인라인 `<script>`(201~264줄)
- `src/main/resources/templates/auth-body.html` — 로그인/회원가입 좌측 소개 영역에 또 다른 **"다국어 지원" 텍스트(29줄)** (챗봇과 무관, 가입 페이지 소개용)
- `src/main/resources/static/css/chatbot.css` — 챗봇 위젯 전용 스타일(`.chatbot-widget`, `.chatbot-header`, `.msg` 등). `fx-home.html`(8줄)에서 `@{/css/chatbot.css}`로 로드
- `src/main/resources/templates/fx-home.html` — 위 CSS 링크 + `body :: body` 프래그먼트 포함(챗봇 마크업이 실제 렌더되는 페이지)
- `src/main/resources/templates/header.html`(115~143줄) — 실제 다국어 동작은 여기 "구글 번역 위젯"이 담당(챗봇 텍스트와 별개)
- 주의: 지시문의 "상담시작" `div`는 코드에서 확인 안 됨. 가장 가까운 것은 `<a ... id="chatbot-open" class="btn btn-light">챗봇 상담 시작</a>`(body.html 176줄, div 아님 a 태그). 별도 `chatbot.js` 파일은 없음 — 동작 JS는 body.html 인라인 스크립트.

**2) 현재 로직**
`fx-home.html`이 `header/body/footer` 프래그먼트를 합쳐 렌더링한다. body.html의 AI 배너(`#chatbot-open` "챗봇 상담 시작" 버튼)를 클릭하면 인라인 스크립트가 숨겨진 `#chatbot-widget`(`display:none`)을 `flex`로 표시한다. 전송 시 `POST /chatbot/ask`로 `{question}`을 보내고 `data.answer`를 말풍선에 출력한다. "다국어 지원"은 동작과 무관한 **정적 안내 문구**(`<p class="ai-sub">`)로 하드코딩되어 있으며, 실제 페이지 번역은 header.html의 Google Translate 위젯(`includedLanguages:'en,ja,zh-CN'`)이 처리한다.

**3) 핵심 코드 스니펫**

`templates/body.html` (164~198줄, AI 배너 + 위젯 마크업):
```html
<section class="section ai-section">
  <div class="section-inner">
    <div class="ai-banner">
      <div class="ai-left">
        <span class="ai-ico">💬</span>
        <div class="ai-text">
          <span class="ai-eyebrow">AI 금융 도우미</span>
          <h3 class="ai-title">외환 고민, AI에게 바로 물어보세요</h3>
          <p class="ai-sub">한국어 · English · 日本語 · 中文 등 다국어 지원 · RAG 기반 정확한 답변</p>
        </div>
      </div>
      <a href="#" id="chatbot-open" class="btn btn-light">챗봇 상담 시작</a>
    </div>
  </div>
</section>
<!-- AI 챗봇 위젯 -->
<div id="chatbot-widget" class="chatbot-widget" style="display:none;">
  <div class="chatbot-header">
    <div class="chatbot-title"><span>💬</span><span>AI 금융 도우미</span></div>
    <button id="chatbot-close" class="chatbot-close" aria-label="닫기">✕</button>
  </div>
  <div id="chatbot-messages" class="chatbot-messages">
    <div class="msg msg-bot">안녕하세요! 외환 관련 궁금한 점을 물어보세요.</div>
  </div>
  <div class="chatbot-input-area">
    <input type="text" id="chatbot-input" class="chatbot-input" placeholder="메시지를 입력하세요" />
    <button id="chatbot-send" class="chatbot-send">전송</button>
  </div>
</div>
```

`templates/auth-body.html` (27~30줄, 가입 페이지 소개용 두 번째 "다국어 지원"):
```html
<li>
  <span class="feat-ico">🌏</span>
  <div class="feat-text"><strong>다국어 지원</strong><span>한국어 · English · 日本語 · 中文</span></div>
</li>
```

`static/css/chatbot.css` (1~16줄):
```css
/* ===== 챗봇 위젯 ===== */
.chatbot-widget {
  position: fixed; bottom: 24px; right: 24px;
  width: 380px; height: 520px; max-height: 75vh;
  background: #fff; border-radius: 16px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.18);
  display: flex; flex-direction: column;
  overflow: hidden; z-index: 9999;
}
```

`templates/body.html` (인라인 동작 스크립트 핵심, 203~250줄):
```javascript
const widget   = document.getElementById('chatbot-widget');
const openBtn  = document.getElementById('chatbot-open');   // "챗봇 상담 시작" 버튼
openBtn.addEventListener('click', e => { e.preventDefault(); widget.style.display='flex'; input.focus(); });
const res = await fetch('/chatbot/ask', { method:'POST',
  headers:{'Content-Type':'application/json'}, body:JSON.stringify({question}) });
const data = await res.json();
loading.textContent = data.answer || '답변을 가져오지 못했습니다.';
```

**4) 수정 시 영향 범위**
- "다국어 지원" 문구는 `body.html`(173줄)과 `auth-body.html`(29줄) **두 곳에 각각 하드코딩** — 하나만 고치면 다른 쪽은 그대로 남음. 정적 텍스트라 실제 번역 기능(header.html Google Translate)과는 무관, 문구 수정해도 동작엔 영향 없음.
- 챗봇 위젯의 id(`chatbot-widget`, `chatbot-open`, `chatbot-close`, `chatbot-send`, `chatbot-input`, `chatbot-messages`)는 body.html 인라인 스크립트가 `getElementById`로 직접 참조 — id 변경 시 같은 파일 스크립트(201~264줄) 동시 수정 필요.
- `chatbot.css`의 클래스(`.chatbot-widget`, `.msg`, `.chatbot-send` 등)는 body.html 마크업과 1:1 결합 — 클래스명 변경 시 양쪽 동시 수정. 단, `chatbot.css`는 `fx-home.html`에서만 링크됨(다른 페이지에선 위젯 미적용).
- 챗봇 전송 엔드포인트 `POST /chatbot/ask` 변경 시 body.html `fetch` URL 수정 필요(서버 측 컨트롤러는 본 조사 범위인 templates/static 밖이라 코드에서 확인 안 함).
- header.html의 Google Translate 설정(`includedLanguages:'en,ja,zh-CN'`)이 실제 다국어 동작 주체 — 지원 언어를 실제로 늘리려면 문구가 아니라 이 부분을 수정해야 함.

---

## 수정 시 주의할 점 / 서로 엮여있는 항목

### A. 공통 fragment 수정 = 전역 파급 (가장 먼저 인지할 것)

- **header.html (`th:fragment="header"`)**: 이 fragment를 `th:replace`/`th:insert`로 끌어쓰는 모든 페이지에 즉시 반영됨. 여기에 묶인 항목 다수:
  - 알림 종(🔔, 2번) — placeholder, JS/뱃지/API 없음
  - 언어선택 버튼(🌐 ▾, 1번) — `class="notranslate"` 유지 필수
  - 검색(🔍)·로그인/로그아웃 토글(`header-auth.js`)
  - GNB 보라색 밑줄 버그(12번)의 `.gnb-link::after`
  - Google Translate 위젯(13번, 실제 다국어 동작 주체, `includedLanguages:'en,ja,zh-CN'`)
  - FAQ 링크(`/fx/guide#faq`, 4번)
  - → header 한 곳을 건드릴 때 1·2·12·13·4번이 한 파일에서 충돌할 수 있음. 특히 이모티콘 제거(1번)와 종 버그/GNB 버그(2·12)가 같은 파일에서 만남.

- **footer.html (`th:fragment="footer"`)**: 명시적으로 21개 페이지가 `th:replace="~{footer :: footer}"`로 공유. FAMILY SITE/카테고리(3번) 마크업 변경 시 21곳 전부 반영. 클래스명(`footer-col`/`footer-grid`/`family-site`/`footer-bottom-inner`) 변경 시 fx-home.css의 해당 셀렉터·반응형 분기까지 동반 수정 필수.

- **body.html (`th:fragment="body"`)**: fx-home.html에서만 `~{body :: body}`로 포함(index.html은 미참조 — 영향 없음). 여기에 4·5·13번이 한 파일에 공존:
  - 실시간 환율 섹션 + 그 아래 빠른이용/추천상품/AI배너(4·5번)
  - 챗봇 위젯 마크업 + 인라인 동작 스크립트(201~264줄) + "다국어 지원" 텍스트(13번)
  - 이모티콘 다수(1번): 🧮📈📘💵🌐💶💴💬 등
  - → 4·5·13·1번 작업이 같은 파일에서 겹침. 특히 이모티콘 교체와 챗봇/추천상품 손질을 동시에 하면 body.html에 변경이 집중됨.

- **auth-body.html (`<main th:fragment="authBody">`)**: login.html과 회원가입 진입이 공유. 7·8번(간편로그인/비밀번호찾기)이 모두 이 파일이며, 1번 이모티콘(🪪📈🌏👤🔒✉️👁)도 여기 집중. 한 곳 수정이 로그인/회원가입 양쪽에 반영됨.

### B. 로그인 항목(7·8)은 같은 파일 + 같은 fragment

- **7번(간편로그인 텍스트)과 8번(비밀번호 찾기)은 동일 파일 `auth-body.html`의 같은 `authBody` fragment 안**에 인접해 있음(66~82행). login.html은 셸일 뿐 실제 마크업은 전부 auth-body.html.
- 둘 다 **순수 더미 UI**: 간편로그인 버튼은 `onclick`/`data-*`/JS 핸들러 전무, 비밀번호 찾기는 `href="#"`. auth.js의 DOMContentLoaded init 목록에 관련 init 없음.
- **주의**: 같은 fragment에 있는 `#loginBtn`은 `initLogin()`/`handleLogin()` → `/api/auth/login`에 실제 연결됨. 7·8번 텍스트/구조만 손대더라도 로그인 버튼 영역은 건드리지 말 것.
- 클래스(`.social-*`, `.link-find`, `.social-row`, `.divider`) 변경 시 fx-auth.css 동반 수정.

### C. 이모티콘 제거(1번)가 공통 영역과 겹치는 지점

이모티콘 제거는 단독 작업이 아니라 **A의 모든 공통 fragment와 교차**함:

- **챗봇(13번)**: body.html의 💬(169·184행), header(`<span>💬</span>`)가 이모티콘 대상. 챗봇 위젯 id/클래스는 인라인 스크립트가 직접 참조하므로 마크업 구조 변경 시 스크립트 동시 수정.
- **헤더(2번 종 포함)**: 🌐(notranslate 유지)·🔍·🔔·▾가 한 파일. 종(2번)은 어차피 placeholder라 이모티콘 교체와 동시에 처리 가능.
- **푸터(3번)**: footer 영역에는 진짜 이모지가 거의 없음(카피라이트 ⓒ 정도) — 이모티콘 작업과 footer 작업은 비교적 독립적.
- **교체 난이도 차등 (한 단위로 묶어 처리 금지)**:
  - HTML 인라인 `<span>` → `<img>` 직접 치환 가능(단 CSS width/height·vertical-align 동반 조정)
  - **CSS `content` (fx-guide.css `ℹ️/⚠️`, fx-home.css `▾/▴`, detail.html `⌄`)** → `<img>` 불가, background-image 전환 또는 유지
  - **JS 동적 문자열 (auth.js/reauth.js `setOcrStatus`, event.js `alert`, fx-rate.js 페이지네이션, fx-home.js `arrow`, mbtiService 아이콘맵, event-status.html Thymeleaf 삼항)** → 마크업 치환 불가, 함수/표현식 로직까지 수정 필요
  - **콘솔로그/주석 (index.html 35·39·42·49, 각종 ·–—→)** → 화면 비노출, 교체 제외
- **에셋 부재**: `static/`에 이미지 폴더/파일이 하나도 없음 → 새 `static/img` 신설 + 정적 리소스 핸들러 경로 확인 필요(핸들러 설정은 코드에서 확인 안 됨).

### D. 공통 CSS(fx-home.css) 수정의 광역 파급

- fx-home.css는 **홈/헤더/푸터/대시보드/GNB 전반의 전역 스타일 파일**. 여기에 묶인 버그/항목: 2번(`.gnb-bell`), 3번(footer 그리드·family-site), 12번(`.gnb-link::after`, `@media max-width:680px`), 그리고 `--violet` 변수.
- **12번 버그 수정 시 주의**: `.gnb-link::after`의 `transform` 채널을 데스크톱 밑줄(scaleX)과 모바일 화살표(rotate)가 공유하고, 전역 `:hover ... scaleX(1)` 규칙(99~102행)이 미디어쿼리 밖이라 모바일에서도 살아있음. 수정은 `.has-sub > .gnb-link::after`에 `background:transparent; height:auto; position:static; transform:none` 리셋을 추가하거나 모바일 hover 무력화. **`--violet` 변수 자체는 다수 컴포넌트 공유라 절대 건드리지 말 것.**
- fx-guide.css는 별개의 광역 파일(`.guide-table*`, `.guide-card`, `.guide-section`, `calcIn` 등) — 10번·11번이 여기 의존.

### E. 버그 3개(9·10·12)의 상호/공통파일 관계

- **9번(옵션 중복)**: 서로/공통파일과 **엮이지 않은 독립 데이터 레벨 문제**. 렌더링·모델·JS 경로는 깨끗(코드 근거로 모든 코드 원인 배제), 유력 원인은 `product_rates`/`product_currencies` 테이블 중복 행. 단, 수정 방향이 SQL(`selectProductRates`/`selectProductCurrencies`)·DB라면 같은 쿼리를 쓰는 **상품 상세(detail.html)의 금리/통화 표시와 가입 시 `rateNo` FK 정합성**까지 동시 영향. form.html만의 문제가 아님.
- **10번(반응형 가입 버튼)**: 9번과 다른 층위(CSS/레이아웃). 원인은 `display:none`이 아니라 `.guide-table`(min-width:520px, 11컬럼, 헤더 nowrap)이 `.guide-table-wrap{overflow-x:auto}` 안에서 가로 오버플로 → "가입" 컬럼이 맨 끝(11번째)이라 화면 밖. **공통파일 fx-guide.css의 `.guide-table*`를 공유하는 모든 가이드/환율 표 화면에 파급**되며, 컬럼 구조 변경 시 list.html의 `colspan="11"`(빈 결과 행)도 동반 수정.
- **12번(보라 줄)**: header.html + fx-home.css. **2번(종)·13번(번역)과 header.html을 공유**하고, **3번(footer)과 fx-home.css를 공유**. 즉 12번은 9·10번과는 무관하지만 공통파일을 통해 헤더/푸터 작업과 엮임.
- **세 버그의 공통 분모**: 9번=데이터/MyBatis, 10번=fx-guide.css/list.html, 12번=fx-home.css/header.html — **서로 직접 연관은 없으나 각자 다른 공통파일을 건드리므로 동시 작업 시 충돌 영역(fx-home.css는 3·12, fx-guide.css는 10·11)을 분리해서 진행**할 것.

### F. JS가 참조하는 id/클래스명 변경 금지(전 영역 공통 리스크)

- 챗봇(13번): `chatbot-widget/open/close/send/input/messages` — body.html 인라인 스크립트가 `getElementById` 직접 참조.
- 가입 단계(11번): `.term-row`/`.read-state`(terms), `.coupon`/`name="couponNo"`(coupon), `.status`/`.ok`/`.error`(form), `canvas#signatureCanvas`(signature, `getBoundingClientRect` 좌표 의존), `#reader` 스크롤(terms-reader 정독 판정) — 애니메이션 추가 시 wrapper/추가 클래스로 처리, 해당 요소 자체에 transform/animation 직접 적용 금지.
- 환율/메인(4번): `rateCards`/`heroFeature` id — fx-home.js가 직접 참조, `/api/fx/rates/main-detail` 의존.
- 가입 옵션(9번): `currencyCode`/`rateNo`/`#withdrawalAccountNo` — form.html JS 의존.

### G. 데이터/표시 익명화(6번)의 동기화 포인트

- 작성자명은 더미가 아니라 매퍼 SQL `selectProductReviews`의 `u.name_ko AS user_name`(실명) 출처.
- **표시 익명화만 목표면 프론트 2곳(`reviews.html`·`detail.html`)을 반드시 동일하게** 수정(한 곳만 바꾸면 불일치). 별도 리뷰 조회 REST 엔드포인트는 없음(SSR).
- 서버 일관 마스킹/실명 차단까지 원하면 매퍼 SQL 또는 `getProductReviews` 가공 — 이 경우 detail.html 미리보기·reviews.html 전체목록 양쪽에 동시 적용되고, avg_rating/review_count 집계 쿼리(users 미조인)는 영향 없음.

### H. 정적 하드코딩 항목 — 동적화하려면 백엔드 동반 필요 (프론트만으로 불가)

- **추천 상품 4개(5번)**: `th:each`/모델 바인딩 없는 완전 하드코딩. 동적화 시 `FxMainController.fxHome()` 모델 주입 필요. "가입하기"/"전체 상품 보기"가 전부 `/product/list` 단일 경로 → 개별 상세 라우팅으로 바꾸려면 컨트롤러까지.
- **FAMILY SITE/카테고리(3번)**: 모든 링크 `href="#"`, select `name="select"`만 — JS 핸들러 신규 필요.
- **알림 종(2번)·간편로그인/비번찾기(7·8번)**: 전부 placeholder, 기능 추가 시 신규 스크립트+백엔드 엔드포인트 필요(현재 코드에서 확인 안 됨).
- **메인에 "외환상품"/"FAQ" 섹션 삽입(4번)**: fx/guide.html에 콘텐츠는 있으나 `th:fragment`가 아닌 통째 페이지 → 재사용하려면 `<section>`을 fragment로 분리 + 탭 JS(`data-panel`)·fx-guide.css 동반. FAQ 위치/앵커 변경 시 header.html(`/fx/guide#faq`)·footer.html(`href="#"`) 링크도 함께 점검.

### I. 11번(애니메이션)과 다른 항목의 교차

- 단계 전환은 풀 페이지 이동(`location.href`)이며 스텝퍼/프로그레스 바는 코드에 없음. 신규 추가 시 join 5개 파일(terms/coupon/form/signature/complete)에 공용 fragment가 없어 **5곳 개별 반영** 필요(단계값 하드코딩 주의).
- 공용 클래스(`.guide-hero/.guide-section/.guide-card/.btn/.guide-input` 등)에 애니/transition을 얹으면 **10번이 속한 list.html, detail.html, fx/exchange-*, fx/guide.html 등 guide-* 레이아웃 전 페이지에 동시 파급**. 11번과 10번이 fx-guide.css를 통해 간접 연결됨.
- 기존 `prefers-reduced-motion` 가드 선례가 있으므로 신규 애니에도 동반해야 정책 일치.
