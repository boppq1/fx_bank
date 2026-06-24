/* fx-calc.js — 환전계산기
 * 통화 드롭다운은 "미국 달러 (USD)" 표기(값=코드), 계산은 /api/fx/calc.
 * 결과는 카운트업 + 등장 애니메이션으로 표시(1차처럼 생동감).
 */
(function () {
  function $(id) { return document.getElementById(id); }

  var curSel, amount, prefer, dateEl, amountUnit, seg, panel, buySell = 'buy';
  var reduceMotion = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  function num(v) {
    var n = Number(String(v).replace(/[^0-9.]/g, ''));
    return isNaN(n) ? 0 : n;
  }
  function fmt(n, dec) {
    dec = dec == null ? 2 : dec;
    return Number(n).toLocaleString('ko-KR', { minimumFractionDigits: dec, maximumFractionDigits: dec });
  }

  // 숫자 카운트업
  function animateNum(el, to, dec, suffix) {
    if (!el) return;
    suffix = suffix || '';
    if (reduceMotion) { el.textContent = fmt(to, dec) + suffix; return; }
    var dur = 750, t0 = null;
    function step(ts) {
      if (t0 === null) t0 = ts;
      var p = Math.min(1, (ts - t0) / dur);
      var e = 1 - Math.pow(1 - p, 3);     // easeOutCubic
      el.textContent = fmt(to * e, dec) + suffix;
      if (p < 1) requestAnimationFrame(step);
      else el.textContent = fmt(to, dec) + suffix;
    }
    requestAnimationFrame(step);
  }

  function loadCurrencies() {
    fetch('/api/fx/rates/latest')
      .then(function (r) { return r.json(); })
      .then(function (res) {
        var rows = (res && res.data) ? res.data : [];
        var codes = FXCUR.sortCodes(rows.map(function (r) { return r.currencyCode; }));
        if (!codes.length) { curSel.innerHTML = '<option value="">환율 데이터 없음</option>'; return; }
        curSel.innerHTML = codes.map(function (c) { return '<option value="' + c + '">' + FXCUR.label(c) + '</option>'; }).join('');
        syncUnit();
      })
      .catch(function (e) { console.error('통화 목록 로드 실패', e); });
  }

  function syncUnit() { if (amountUnit && curSel) amountUnit.textContent = curSel.value || ''; }

  function calc() {
    var currencyCode = curSel.value;
    if (!currencyCode) { alert('통화를 선택하세요.'); return; }
    var pref = num(prefer.value), amt = num(amount.value), date = dateEl.value || '';

    var url = '/api/fx/calc?currencyCode=' + encodeURIComponent(currencyCode)
      + '&buySell=' + buySell + '&prefer=' + pref + (date ? '&date=' + date : '');

    fetch(url)
      .then(function (r) { return r.json(); })
      .then(function (res) {
        if (!res || !res.success || !res.data) {
          alert((res && res.message) || '환율 데이터를 불러오지 못했습니다.');
          return;
        }
        var d = res.data;
        var krw = amt * d.appliedRate;
        var saved = (d.preferDiscount || 0) * amt;

        // 텍스트(정적) 항목
        $('rDir').textContent = (buySell === 'buy') ? '외화 살 때 (원화 지급)' : '외화 팔 때 (원화 수령)';
        $('rCur').textContent = currencyCode;
        $('rInput').textContent = fmt(amt, 0) + ' ' + currencyCode + ' 기준 · 우대율 ' + pref + '%';
        $('rDate').textContent = (d.baseDate || '').replace('T', ' ').substring(0, 10);

        // 등장 애니메이션 재시작
        if (panel) { panel.classList.remove('is-calc'); void panel.offsetWidth; panel.classList.add('is-calc'); }

        // 숫자 카운트업
        animateNum($('rKrw'), krw, 0, ' 원');
        animateNum($('rApplied'), d.appliedRate, 2);
        animateNum($('rNotice'), d.noticeRate, 2);
        animateNum($('rBase'), d.baseRate, 2);
        animateNum($('rSpread'), d.spread, 2);
        animateNum($('rSaved'), saved, 0, ' 원');

        // 모바일에서는 결과로 스크롤
        if (window.innerWidth < 900 && panel) panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
      })
      .catch(function (e) {
        console.error('환전 계산 실패', e);
        alert('환율 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
      });
  }

  document.addEventListener('DOMContentLoaded', function () {
    curSel = $('calcCurrency'); amount = $('calcAmount'); prefer = $('calcPrefer');
    dateEl = $('calcDate'); amountUnit = $('calcAmountUnit'); seg = $('calcSeg');
    panel = document.querySelector('.calc-result-panel');

    loadCurrencies();
    curSel.addEventListener('change', syncUnit);

    amount.addEventListener('input', function () {
      var v = num(amount.value);
      amount.value = v ? v.toLocaleString('ko-KR') : '';
    });

    // 살 때 / 팔 때
    if (seg) seg.addEventListener('click', function (e) {
      var b = e.target.closest('.seg-btn'); if (!b) return;
      buySell = b.getAttribute('data-bs');
      seg.querySelectorAll('.seg-btn').forEach(function (x) { x.classList.toggle('is-active', x === b); });
    });

    // 우대율 칩
    var pc = $('preferChips');
    if (pc) pc.addEventListener('click', function (e) {
      var b = e.target.closest('.guide-chip'); if (!b) return;
      prefer.value = b.getAttribute('data-v');
      pc.querySelectorAll('.guide-chip').forEach(function (x) { x.classList.toggle('is-active', x === b); });
    });

    // 금액 칩
    var ac = $('amountChips');
    if (ac) ac.addEventListener('click', function (e) {
      var b = e.target.closest('.guide-chip'); if (!b) return;
      amount.value = Number(b.getAttribute('data-v')).toLocaleString('ko-KR');
    });

    $('calcBtn').addEventListener('click', calc);
  });
})();
