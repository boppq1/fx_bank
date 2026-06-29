/* fx-rate.js — 환율조회: 전체 기간 데이터(/api/fx/rates/all) + 검색/드롭다운 + 페이지네이션(20건/페이지) */
(function () {
  var PER = 20;
  var all = [];      // 전체 행
  var rows = [];     // 필터 적용 결과
  var page = 1;

  var elBody = function () { return document.getElementById('fxRateBody'); };
  var elPager = function () { return document.getElementById('fxPager'); };
  var elSearch = function () { return document.getElementById('fxRateSearch'); };
  var elSelect = function () { return document.getElementById('fxCurSelect'); };
  var elCount = function () { return document.getElementById('fxCount'); };
  var elCards = function () { return document.getElementById('fxRateCards'); };

  function fmt(n) {
    if (n === null || n === undefined) return '-';
    return Number(n).toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
  function fmtDate(s) { return s ? String(s).replace('T', ' ').substring(0, 10) : '-'; }

  function applyFilter() {
    var q = elSearch() ? elSearch().value : '';
    var cur = elSelect() ? elSelect().value : '';
    rows = all.filter(function (r) {
      if (cur && r.currencyCode !== cur) return false;
      return FXCUR.matches(r.currencyCode, q);
    });
    page = 1;
    render();
  }

  function render() {
    var body = elBody();
    var total = rows.length;
    var pages = Math.max(1, Math.ceil(total / PER));
    if (page > pages) page = pages;
    var start = (page - 1) * PER;
    var slice = rows.slice(start, start + PER);

    if (elCount()) elCount().textContent = total.toLocaleString('ko-KR');

    if (!slice.length) {
      body.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--text-sub)">조회 결과가 없습니다.</td></tr>';
    } else {
      body.innerHTML = slice.map(function (r) {
        return '<tr>'
          + '<th><div class="rt-cur"><span class="rt-name">' + FXCUR.name(r.currencyCode) + '</span>'
          + '<span class="rt-code">' + r.currencyCode + '</span></div></th>'
          + '<td>' + fmt(r.baseRate) + '</td>'
          + '<td>' + fmt(r.sellRate) + '</td>'
          + '<td>' + fmt(r.buyRate) + '</td>'
          + '<td>' + fmtDate(r.announcedAt) + '</td>'
          + '</tr>';
      }).join('');
    }
    renderMobileCards(slice);
    renderPager(pages);
  }

  function renderMobileCards(slice) {
    var cards = elCards();
    if (!cards) return;
    if (!slice.length) {
      cards.innerHTML = '<div class="fx-rate-empty">조회 결과가 없습니다.</div>';
      return;
    }
    cards.innerHTML = slice.map(function (r) {
      return '<article class="fx-rate-card">'
        + '<div class="fx-rate-card-head">'
        + '<div class="fx-rate-currency"><span class="fx-rate-symbol">' + FXCUR.initials(r.currencyCode) + '</span>'
        + '<div><span class="fx-rate-name">' + FXCUR.name(r.currencyCode) + '</span><span class="fx-rate-code">' + r.currencyCode + '</span></div></div>'
        + '<span class="fx-rate-date">' + fmtDate(r.announcedAt) + '</span>'
        + '</div>'
        + '<div class="fx-rate-base"><span>매매기준율</span><strong>' + fmt(r.baseRate) + '</strong></div>'
        + '<div class="fx-rate-grid">'
        + '<div class="fx-rate-mini"><span>살 때</span><strong>' + fmt(r.sellRate) + '</strong></div>'
        + '<div class="fx-rate-mini"><span>팔 때</span><strong>' + fmt(r.buyRate) + '</strong></div>'
        + '</div>'
        + '</article>';
    }).join('');
  }

  function pageBtn(p, label, opts) {
    opts = opts || {};
    var cls = 'guide-page';
    if (opts.active) cls += ' is-active';
    if (opts.disabled) cls += ' is-disabled';
    var dis = opts.disabled ? ' disabled' : '';
    return '<button type="button" class="' + cls + '" data-p="' + p + '"' + dis + '>' + (label || p) + '</button>';
  }

  function renderPager(pages) {
    var el = elPager();
    if (!el) return;
    if (pages <= 1) { el.innerHTML = ''; return; }

    var html = '';
    html += pageBtn(page - 1, '&lsaquo;', { disabled: page === 1 });

    var win = 2;
    var startP = Math.max(1, page - win);
    var endP = Math.min(pages, page + win);

    if (startP > 1) {
      html += pageBtn(1, '1');
      if (startP > 2) html += '<span class="guide-page-dots">...</span>';
    }
    for (var p = startP; p <= endP; p++) {
      html += pageBtn(p, String(p), { active: p === page });
    }
    if (endP < pages) {
      if (endP < pages - 1) html += '<span class="guide-page-dots">...</span>';
      html += pageBtn(pages, String(pages));
    }

    html += pageBtn(page + 1, '&rsaquo;', { disabled: page === pages });
    el.innerHTML = html;
  }

  function buildSelect() {
    var sel = elSelect();
    if (!sel) return;
    var codes = [];
    var seen = {};
    all.forEach(function (r) { if (!seen[r.currencyCode]) { seen[r.currencyCode] = 1; codes.push(r.currencyCode); } });
    codes = FXCUR.sortCodes(codes);
    var opts = '<option value="">전체 통화</option>';
    codes.forEach(function (c) { opts += '<option value="' + c + '">' + FXCUR.label(c) + '</option>'; });
    sel.innerHTML = opts;
  }

  function bind() {
    if (elSearch()) elSearch().addEventListener('input', applyFilter);
    if (elSelect()) elSelect().addEventListener('change', applyFilter);
    if (elPager()) elPager().addEventListener('click', function (e) {
      var b = e.target.closest('.guide-page');
      if (!b || b.disabled) return;
      var p = Number(b.getAttribute('data-p'));
      if (!isNaN(p)) { page = p; render(); window.scrollTo({ top: 0, behavior: 'smooth' }); }
    });
  }

  function load() {
    fetch('/api/fx/rates/all')
      .then(function (r) { return r.json(); })
      .then(function (res) {
        all = (res && res.data) ? res.data : [];
        buildSelect();
        rows = all.slice();
        page = 1;
        render();
      })
      .catch(function (e) {
        console.error('환율 조회 실패', e);
        all = []; rows = []; render();
      });
  }

  document.addEventListener('DOMContentLoaded', function () { bind(); load(); });
})();
