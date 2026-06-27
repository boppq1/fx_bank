/* fx-home.js — 메인 대시보드 환율 (DB 기반 /api/fx/rates/main-detail)
 * 히어로 featured 카드 = 좌우로 넘기는 캐러셀(통화 슬라이드).
 * 아래 환율 카드 = 전 통화 표시 + 클릭 시 위 캐러셀과 동기화.
 * 미니 스파크라인은 3개월 실데이터(SVG). 외부 API 직접 호출 없음.
 */
(function () {
  var ACCENT = { 'USD': '#3B82F6', 'JPY(100)': '#FB7185', 'CNH': '#F59E0B', 'EUR': '#14B8A6', 'CNY': '#F59E0B', 'GBP': '#6D5BFF', 'AUD': '#14B8A6', 'CAD': '#FB7185', 'CHF': '#3B82F6' };
  var NAME = { 'USD': '미국 달러', 'JPY(100)': '일본 엔(100)', 'CNH': '중국 위안', 'EUR': '유로', 'CNY': '중국 위안', 'GBP': '영국 파운드', 'AUD': '호주 달러', 'CAD': '캐나다 달러', 'CHF': '스위스 프랑' };

  function accent(c) { return ACCENT[c] || '#6D5BFF'; }
  function name(c) { return NAME[c] || c; }
  function initials(c) { return (c || '').replace(/[^A-Za-z]/g, '').substring(0, 2).toUpperCase() || '–'; }
  function fmt(n) { return Number(n).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  function pct(n) { return (n > 0 ? '+' : '') + Number(n).toFixed(2) + '%'; }
  function arrow(n) { return n > 0 ? '▲' : (n < 0 ? '▼' : '–'); }
  function dir(n) { return n > 0 ? 'up' : (n < 0 ? 'down' : ''); }
  function hexRgba(hex, a) {
    var h = hex.replace('#', '');
    return 'rgba(' + parseInt(h.substr(0, 2), 16) + ',' + parseInt(h.substr(2, 2), 16) + ',' + parseInt(h.substr(4, 2), 16) + ',' + a + ')';
  }

  function sparkSvg(vals, color, w, h) {
    if (!vals || vals.length < 2) return '';
    var min = Math.min.apply(null, vals), max = Math.max.apply(null, vals);
    var range = (max - min) || 1, n = vals.length, pad = 3;
    var d = vals.map(function (v, i) {
      var x = pad + (i / (n - 1)) * (w - 2 * pad);
      var y = pad + (1 - (v - min) / range) * (h - 2 * pad);
      return (i === 0 ? 'M' : 'L') + x.toFixed(1) + ' ' + y.toFixed(1);
    }).join(' ');
    var last = vals[n - 1], lx = w - pad, ly = pad + (1 - (last - min) / range) * (h - 2 * pad);
    return '<svg data-sparkline width="' + w + '" height="' + h + '" viewBox="0 0 ' + w + ' ' + h + '" preserveAspectRatio="none">'
      + '<path class="spark-line" d="' + d + '" fill="none" stroke="' + color + '" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>'
      + '<circle cx="' + lx.toFixed(1) + '" cy="' + ly.toFixed(1) + '" r="2.6" fill="' + color + '"/></svg>';
  }

  var data = [];
  var idx = 0;

  function featuredHtml(f) {
    var c = f.currencyCode;
    return '<div class="hf-top"><span class="hf-flag">' + initials(c) + '</span>'
      + '<div><div class="hf-code">' + c + '</div><div class="hf-name">' + name(c) + '</div></div></div>'
      + '<p class="hf-rate">' + fmt(f.baseRate) + '<span class="hf-unit"> 원</span></p>'
      + '<span class="hf-change ' + dir(f.changePct) + '">' + arrow(f.changePct) + ' ' + pct(f.changePct) + ' <span style="opacity:.7">전일대비</span></span>'
      + '<div class="hf-spark">' + sparkSvg(f.spark, 'rgba(255,255,255,.92)', 300, 56) + '</div>'
      + '<div class="hf-sub">'
      + '<div><span>살 때</span><strong>' + fmt(f.sellRate) + '</strong></div>'
      + '<div><span>팔 때</span><strong>' + fmt(f.buyRate) + '</strong></div>'
      + '<div><span>매매기준율</span><strong>' + fmt(f.baseRate) + '</strong></div>'
      + '</div>';
  }

  function renderDots() {
    var d = document.getElementById('hfDots');
    if (!d) return;
    d.innerHTML = data.map(function (_, i) {
      return '<button type="button" class="hf-dot' + (i === idx ? ' is-active' : '') + '" data-i="' + i + '" aria-label="' + (i + 1) + '번"></button>';
    }).join('');
  }

  function highlightCards() {
    var cards = document.querySelectorAll('#rateCards .rate-card');
    cards.forEach(function (el) {
      el.classList.toggle('is-active', Number(el.getAttribute('data-i')) === idx);
    });
  }

  function renderFeatured() {
    var stage = document.getElementById('heroFeature');
    if (!stage) return;
    if (!data.length) { stage.innerHTML = '<div class="hf-skeleton">환율 데이터가 없습니다</div>'; return; }
    stage.innerHTML = featuredHtml(data[idx]);
    stage.classList.remove('hf-anim');
    void stage.offsetWidth;          // 애니메이션 재시작
    stage.classList.add('hf-anim');
    renderDots();
    highlightCards();
    // 히어로 스파크라인 드로우인 (통화 전환 시마다 다시 그려짐)
    if (window.Motion) { var hs = stage.querySelector('[data-sparkline]'); if (hs) window.Motion.drawSpark(hs); }
  }

  function go(n) {
    if (!data.length) return;
    idx = (n % data.length + data.length) % data.length;
    renderFeatured();
  }

  function renderCards() {
    var el = document.getElementById('rateCards');
    if (!el) return;
    if (!data.length) { el.innerHTML = '<div class="rate-card-skel">환율 데이터가 없습니다.</div>'; return; }
    el.innerHTML = data.map(function (r, i) {
      var a = accent(r.currencyCode);
      return '<div class="rate-card' + (i === idx ? ' is-active' : '') + '" data-i="' + i + '" data-reveal style="--rc:' + a + '" role="button" tabindex="0">'
        + '<div class="rc-top"><span class="rc-flag" style="background:' + hexRgba(a, 0.12) + ';color:' + a + '">' + initials(r.currencyCode) + '</span>'
        + '<div><div class="rc-code">' + r.currencyCode + '</div><div class="rc-name">' + name(r.currencyCode) + '</div></div></div>'
        + '<p class="rc-rate" data-countup>' + fmt(r.baseRate) + '</p>'
        + '<div class="rc-meta"><span class="rc-change ' + dir(r.changePct) + '">' + arrow(r.changePct) + ' ' + pct(r.changePct) + '</span></div>'
        + '<div class="rc-spark">' + sparkSvg(r.spark, a, 200, 40) + '</div>'
        + '</div>';
    }).join('');
    // 새로 렌더된 카드들을 모션 엔진에 등록(리빌 시 카운트업·스파크라인 트리거)
    if (window.Motion) window.Motion.scan();
  }

  function bind() {
    var prev = document.getElementById('hfPrev');
    var next = document.getElementById('hfNext');
    if (prev) prev.addEventListener('click', function () { go(idx - 1); });
    if (next) next.addEventListener('click', function () { go(idx + 1); });

    var dots = document.getElementById('hfDots');
    if (dots) dots.addEventListener('click', function (e) {
      var b = e.target.closest('.hf-dot'); if (b) go(Number(b.getAttribute('data-i')));
    });

    var cards = document.getElementById('rateCards');
    if (cards) {
      cards.addEventListener('click', function (e) {
        var c = e.target.closest('.rate-card'); if (c) go(Number(c.getAttribute('data-i')));
      });
      cards.addEventListener('keydown', function (e) {
        if (e.key !== 'Enter' && e.key !== ' ') return;
        var c = e.target.closest('.rate-card'); if (c) { e.preventDefault(); go(Number(c.getAttribute('data-i'))); }
      });
    }

    // 터치 스와이프 (히어로 카드)
    var stage = document.getElementById('heroFeature');
    if (stage) {
      var sx = 0;
      stage.addEventListener('touchstart', function (e) { sx = e.changedTouches[0].clientX; }, { passive: true });
      stage.addEventListener('touchend', function (e) {
        var dx = e.changedTouches[0].clientX - sx;
        if (Math.abs(dx) > 40) go(dx < 0 ? idx + 1 : idx - 1);
      }, { passive: true });
    }
  }

  document.addEventListener('DOMContentLoaded', function () {
    bind();
    fetch('/api/fx/rates/main-detail')
      .then(function (r) { return r.json(); })
      .then(function (res) {
        data = (res && res.data) ? res.data : [];
        var u = data.map(function (r) { return r.currencyCode; }).indexOf('USD');
        idx = u >= 0 ? u : 0;
        renderCards();
        renderFeatured();
      })
      .catch(function (e) {
        console.error('메인 환율 로드 실패', e);
        renderCards();
        renderFeatured();
      });
  });
})();
