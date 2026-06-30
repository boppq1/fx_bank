/* ============================================================
   motion.js · KLS 외환 프리미엄 모션 엔진 (바닐라, 무의존)
   - data-* 선언으로 자동 초기화
       data-reveal[=up|fade|scale|left|right]  스크롤 등장 (IntersectionObserver)
       [data-stagger="80"]                     자식 [data-reveal] 순차 지연(ms)
       data-countup (+data-countup-suffix/prefix)  숫자 카운트업 (리빌 시 1회)
       data-sparkline (SVG, 내부 .spark-line)  선이 그려지는 드로우인
       data-ripple                             클릭 리플
   - prefers-reduced-motion 이면 애니메이션 없이 즉시 최종상태
   - window.Motion API 로 동적 콘텐츠(예: fx-home.js 가 그린 카드)에서 수동 트리거 가능
   - 첫 줄에서 html.motion 부여 → motion.css 의 초기 숨김이 적용됨(미동작 시 콘텐츠 노출 폴백)
   ============================================================ */
(function () {
  "use strict";
  var REDUCE = !!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches);

  // JS 활성 신호 (동기 실행 → 하위 콘텐츠 페인트 전에 초기 숨김 적용, FOUC 최소화)
  document.documentElement.classList.add('motion');

  /* ---------------- 숫자 카운트업 ---------------- */
  function parseNum(s){ var n = parseFloat(String(s).replace(/[^0-9.\-]/g, '')); return isNaN(n) ? 0 : n; }
  function decimalsOf(s){ var m = String(s).match(/\.(\d+)/); return m ? m[1].length : 0; }
  function countup(el){
    if (!el || el.__counted) return; el.__counted = true;
    var raw = el.getAttribute('data-countup');
    if (raw === null || raw === '') raw = el.textContent;
    var to = parseNum(raw), dec = decimalsOf(raw);
    var prefix = el.getAttribute('data-countup-prefix') || '';
    var suffix = el.getAttribute('data-countup-suffix') || '';
    function fmt(v){ return prefix + v.toLocaleString('ko-KR', { minimumFractionDigits: dec, maximumFractionDigits: dec }) + suffix; }
    if (REDUCE) { el.textContent = fmt(to); return; }
    var dur = 900, start = null;
    function step(ts){
      if (!start) start = ts;
      var p = Math.min(1, (ts - start) / dur);
      var eased = 1 - Math.pow(1 - p, 3); // easeOutCubic
      el.textContent = fmt(to * eased);
      if (p < 1) requestAnimationFrame(step); else el.textContent = fmt(to);
    }
    requestAnimationFrame(step);
  }

  /* ---------------- 스파크라인 드로우인 ---------------- */
  function drawSpark(svg){
    if (!svg) return;
    var paths = svg.querySelectorAll('.spark-line');
    Array.prototype.forEach.call(paths, function (p) {
      try { p.style.setProperty('--spark-len', p.getTotalLength()); } catch (e) {}
      requestAnimationFrame(function () { p.classList.add('is-drawn'); });
    });
  }

  /* ---------------- 리빌 (+ countup/sparkline 동시 트리거) ---------------- */
  function revealEl(el){
    el.classList.add('is-in');
    if (el.hasAttribute('data-countup')) countup(el);
    Array.prototype.forEach.call(el.querySelectorAll('[data-countup]'), countup);
    if (el.matches && el.matches('[data-sparkline]')) drawSpark(el);
    Array.prototype.forEach.call(el.querySelectorAll('[data-sparkline]'), drawSpark);
  }

  function initReveal(){
    var items = Array.prototype.slice.call(document.querySelectorAll('[data-reveal]'));
    // stagger: 부모 [data-stagger] 의 자식 [data-reveal] 에 순차 지연
    Array.prototype.forEach.call(document.querySelectorAll('[data-stagger]'), function (parent) {
      var base = parseFloat(parent.getAttribute('data-stagger')) || 80;
      Array.prototype.forEach.call(parent.querySelectorAll('[data-reveal]'), function (k, i) {
        k.style.transitionDelay = (i * base) + 'ms';
      });
    });
    if (REDUCE || !('IntersectionObserver' in window)) { items.forEach(revealEl); return; }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (en.isIntersecting) { revealEl(en.target); io.unobserve(en.target); }
      });
    }, { threshold: 0.14, rootMargin: '0px 0px -8% 0px' });
    items.forEach(function (el) { io.observe(el); });
  }

  /* ---------------- 리플 ---------------- */
  function initRipple(){
    if (REDUCE) return;
    document.addEventListener('click', function (e) {
      var host = e.target.closest && e.target.closest('[data-ripple]');
      if (!host) return;
      host.classList.add('ripple-host');
      var r = host.getBoundingClientRect();
      var size = Math.max(r.width, r.height);
      var span = document.createElement('span');
      span.className = 'ripple';
      span.style.width = span.style.height = size + 'px';
      span.style.left = (e.clientX - r.left - size / 2) + 'px';
      span.style.top = (e.clientY - r.top - size / 2) + 'px';
      host.appendChild(span);
      setTimeout(function () { span.remove(); }, 700);
    });
  }

  // 동적 콘텐츠(렌더 후) 수동 트리거용 공개 API
  window.Motion = {
    reveal: revealEl,
    countup: countup,
    drawSpark: drawSpark,
    reduce: REDUCE,
    // 새로 DOM 에 추가된 [data-reveal] 들을 다시 스캔(예: fx-home.js 카드 렌더 후)
    scan: initReveal
  };

  function init(){ initReveal(); initRipple(); }
  if (document.readyState !== 'loading') init();
  else document.addEventListener('DOMContentLoaded', init);
})();
