/* fx-rate.js — 환율조회: /api/fx/rates/latest 의 통화별 최신 환율을 표로 렌더 */
(function () {
  var tbody = document.getElementById('fxRateBody');
  var search = document.getElementById('fxRateSearch');
  var all = [];

  function fmt(n) {
    if (n === null || n === undefined) return '-';
    return Number(n).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function fmtDt(s) {
    if (!s) return '-';
    return String(s).replace('T', ' ').substring(0, 16);
  }
  function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>]/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;' }[c];
    });
  }

  function render(rows) {
    tbody.innerHTML = '';
    if (!rows || rows.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--text-sub)">조회된 환율이 없습니다.</td></tr>';
      return;
    }
    rows.forEach(function (r) {
      var tr = document.createElement('tr');
      tr.innerHTML =
        '<th>' + esc(r.currencyCode) + '</th>' +
        '<td>' + fmt(r.baseRate) + '</td>' +
        '<td>' + fmt(r.sellRate) + '</td>' +
        '<td>' + fmt(r.buyRate) + '</td>' +
        '<td>' + fmtDt(r.announcedAt) + '</td>';
      tbody.appendChild(tr);
    });
  }

  function load() {
    fetch('/api/fx/rates/latest')
      .then(function (res) { return res.json(); })
      .then(function (res) {
        all = (res && res.data) ? res.data : [];
        render(all);
      })
      .catch(function (e) {
        console.error('환율 조회 실패', e);
        render([]);
      });
  }

  if (search) {
    search.addEventListener('input', function () {
      var q = search.value.trim().toUpperCase();
      render(all.filter(function (r) {
        return (r.currencyCode || '').toUpperCase().indexOf(q) !== -1;
      }));
    });
  }

  document.addEventListener('DOMContentLoaded', load);
})();
