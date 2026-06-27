# BNK 외환 — 프리미엄 디자인/모션 시스템

> 모든 페이지가 **공유하는 단일 모션 시스템**. 페이지별로 새 효과를 발명하지 말고, 아래 토큰·유틸·data 속성의 **조합**으로만 표현한다. (토스/레볼루트 톤 · 신뢰감 우선)

로드 위치: `templates/header.html` fragment 상단에서 `motion.css` + `motion.js` 를 전 페이지에 주입(헤더 미사용 admin 페이지는 각 `<head>`에 개별 추가).

---

## 1. 토큰 (CSS 변수)

기존 `fx-home.css :root` (브랜드 컬러/그림자/`--t`/`--ease`)는 **유지**. `motion.css :root` 에서 모션 토큰만 추가:

| 범주 | 토큰 | 값/용도 |
|---|---|---|
| 이징 | `--ease-out-expo` | `cubic-bezier(.16,1,.3,1)` — 리빌/진입 기본 |
| | `--ease-out-quart` | `cubic-bezier(.25,1,.5,1)` — 호버/UI 전이 |
| | `--ease-spring` | `cubic-bezier(.34,1.56,.64,1)` — 강조/팝(절제해서) |
| 듀레이션 | `--dur-fast` 140ms / `--dur-base` 320ms / `--dur-slow` 620ms | press / hover·전환 / 리빌 |
| 엘리베이션 | `--elev-rest` / `--elev-hover` / `--elev-overlay` | 카드 rest→hover→오버레이 그림자 |
| 글로우·메쉬 | `--glow-violet`, `--grad-mesh` | 호버 발광, 히어로 메쉬 배경 |
| 기타 | `--reveal-dist` 24px | 리빌 이동 거리 |

원칙: **색은 브랜드 바이올렛/라벤더 팔레트 안에서만**. 새 색 남발 금지. 그라데이션/깊이/글로우로 고급감을 낸다.

---

## 2. 모션 유틸 (motion.css) — 선언적 사용

### 스크롤 리빌
- `data-reveal` (기본 = up), `data-reveal="fade|scale|left|right"`.
- 부모에 `data-stagger="80"` → 자식 `[data-reveal]` 80ms 간격 순차 등장.
- 초기 숨김은 `html.motion` 일 때만 적용(JS 없으면 그대로 노출). 뷰포트 진입 시 `.is-in`.

```html
<section data-reveal>...</section>
<div class="grid" data-stagger="90">
  <article data-reveal></article>
  <article data-reveal></article>
</div>
```

### 호버/프레스
- `.lift` — 카드/버튼 hover 시 `translateY(-4px)` + `--elev-hover`.
- `.press` — `:active` 눌림(`scale(.97)`).
- `.glow` — hover 시 바이올렛 발광.

### 상태/로더
- `.skeleton` — 로딩 자리(쉬머). `.live-dot` — 실시간 펄스 점.
- `.spark-line` (SVG path) — 드로우인. `[data-ripple]` — 클릭 리플.

---

## 3. 모션 엔진 (motion.js) — data 속성 자동 초기화

| 속성 | 동작 |
|---|---|
| `data-reveal` / `[data-stagger]` | IntersectionObserver 로 진입 시 1회 리빌 |
| `data-countup` (`data-countup-suffix="%"` 등) | 0→값 카운트업(리빌 시 1회). 소수자리/로케일 자동 |
| `data-sparkline` (내부 `.spark-line`) | stroke-dashoffset 드로우인 |
| `data-ripple` | 클릭 위치 리플 |

공개 API `window.Motion`: `reveal(el)`, `countup(el)`, `drawSpark(svg)`, `scan()`, `reduce`(boolean).
→ **동적 렌더 콘텐츠**(예: `fx-home.js` 가 그린 환율 카드)는 렌더 직후 `Motion.scan()` 또는 개별 `Motion.countup(el)` 호출로 효과 적용.

---

## 4. 규칙 (위반 금지)

1. **백엔드 무결성**: `th:*`, 폼 `action/method`, input `name/id`, 인증/JWT/리다이렉트 JS, MyBatis 흐름 불변. 디자인은 마크업/CSS/모션 레이어에서만.
2. **reduced-motion**: 모든 모션은 `@media (prefers-reduced-motion: reduce)` 에서 제거/축소(motion.css 가 전역 처리, motion.js 도 즉시 최종상태).
3. **성능**: `transform`/`opacity` 위주. layout 유발 속성 애니 지양. `will-change` 는 리빌 등 꼭 필요한 곳만.
4. **접근성**: 포커스 링 항상 표시(`:focus-visible`), 키보드 동선 유지, 장식 `aria-hidden`, 의미 요소 라벨.
5. **일관성**: 동일 이징/듀레이션/등장 리듬 공유. 페이지별 스노우플레이크 금지.

---

## 5. 맥락별 강도 가이드

| 영역 | 강도 | 비고 |
|---|---|---|
| 메인/홈 | 높음(간판) | 히어로 메쉬+오케스트레이션, 카운트업, 스파크라인 |
| 인증(login/register/reauth) | 절제 | 카드 페이드+살짝 스케일, 탭 슬라이드, 플로팅 라벨 |
| 상품 목록/상세 | 중 | grid stagger, lift, 금리 카운트업, CTA sticky |
| 가입 플로우 | 중 | 단계 전환·진행바·옵션 피드백·완료 축하 |
| 이벤트/마케팅 | 자유(플레이풀) | 과감하되 토큰은 동일 |
| 어드민 | 낮음 | 가벼운 폴리시(전환·호버)만 |
