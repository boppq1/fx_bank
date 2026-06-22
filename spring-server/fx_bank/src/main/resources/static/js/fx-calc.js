/* fx-calc.js — 환전계산기
 * 통화 목록은 /api/fx/rates/latest 로 채우고, 계산은 /api/fx/calc 호출.
 * 예상 원화 금액 = 입력 외화 금액 × 적용환율(appliedRate).
 */
(function () {
  function $(id) { return document.getElementById(id); }

  var curSel = $('calcCurrency');
  var amount = $('calcAmount');
  var prefer = $('calcPrefer');
  var dateEl = $('calcDate');
  var result = $('calcResult');

  function num(v) {
    var n = Number(String(v).replace(/[^0-9.]/g, ''));
    return isNaN(n) ? 0 : n;
  }
  function fmt(n) {
    if (n === null || n === undefined) return '-';
    return Number(n).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function won(n) {
    return Number(Math.round(n)).toLocaleString('ko-KR') + ' 원';
  }

  // 금액 입력 시 천단위 콤마
  amount.addEventListener('input', function () {
    var v = num(amount.value);
    amount.value = v ? v.toLocaleString('ko-KR') : '';
  });

  // 통화 목록 채우기
  fetch('/api/fx/rates/latest')
    .then(function (r) { return r.json(); })
    .then(function (res) {
      var rows = (res && res.data) ? res.data : [];
      curSel.innerHTML = '';
      if (!rows.length) {
        var o0 = document.createElement('option');
        o0.value = ''; o0.textContent = '환율 데이터 없음';
        curSel.appendChild(o0);
        return;
      }
      rows.forEach(function (r) {
        var o = document.createElement('option');
        o.value = r.currencyCode;
        o.textContent = r.currencyCode;
        curSel.appendChild(o);
      });
    })
    .catch(function (e) { console.error('통화 목록 로드 실패', e); });

  $('calcBtn').addEventListener('click', function () {
    var currencyCode = curSel.value;
    if (!currencyCode) { alert('통화를 선택하세요.'); return; }

    var buySellEl = document.querySelector('input[name="calcBuySell"]:checked');
    var buySell = buySellEl ? buySellEl.value : 'buy';
    var pref = num(prefer.value);
    var amt = num(amount.value);
    var date = dateEl.value || ''; // yyyy-MM-dd

    var url = '/api/fx/calc?currencyCode=' + encodeURIComponent(currencyCode)
      + '&buySell=' + buySell + '&prefer=' + pref
      + (date ? '&date=' + date : '');

    fetch(url)
      .then(function (r) { return r.json(); })
      .then(function (res) {
        if (!res || !res.success || !res.data) {
          alert((res && res.message) || '환율 데이터를 불러오지 못했습니다.');
          result.hidden = true;
          return;
        }
        var d = res.data;
        var krw = amt * d.appliedRate;
        var saved = (d.preferDiscount || 0) * amt;

        $('rKrw').textContent = won(krw);
        $('rApplied').textContent = fmt(d.appliedRate);
        $('rNotice').textContent = fmt(d.noticeRate);
        $('rBase').textContent = fmt(d.baseRate);
        $('rSpread').textContent = fmt(d.spread);
        $('rSaved').textContent = won(saved);
        $('rDir').textContent = (buySell === 'buy') ? '외화 살 때 (원화 지급)' : '외화 팔 때 (원화 수령)';
        $('rCur').textContent = currencyCode;
        $('rDate').textContent = (d.baseDate || '').replace('T', ' ').substring(0, 16);

        result.hidden = false;
        result.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      })
      .catch(function (e) {
        console.error('환전 계산 실패', e);
        alert('환율 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
      });
  });
})();
