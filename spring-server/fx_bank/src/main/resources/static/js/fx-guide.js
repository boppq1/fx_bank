/* fx-guide.js — 외환 상품·서비스 안내 탭 전환
 * .guide-tab-btn[data-tab] 클릭 시 동일 data-panel 패널만 노출.
 * URL 해시(#faq 등)로 진입 시 해당 탭 자동 활성화 (헤더 '고객지원 > FAQ' 연동).
 */
(function () {
  function activate(tab) {
    var btns = document.querySelectorAll('.guide-tab-btn');
    var panels = document.querySelectorAll('.guide-panel');
    var matched = false;

    btns.forEach(function (b) {
      var on = b.getAttribute('data-tab') === tab;
      b.classList.toggle('is-active', on);
      if (on) matched = true;
    });
    panels.forEach(function (p) {
      p.classList.toggle('is-active', p.getAttribute('data-panel') === tab);
    });
    return matched;
  }

  document.addEventListener('DOMContentLoaded', function () {
    var btns = document.querySelectorAll('.guide-tab-btn');
    if (!btns.length) return;

    btns.forEach(function (b) {
      b.addEventListener('click', function () {
        var tab = b.getAttribute('data-tab');
        activate(tab);
        if (history.replaceState) {
          history.replaceState(null, '', '#' + tab);
        }
      });
    });

    // 해시로 진입 시 해당 탭, 없으면 첫 번째 탭
    var hash = (location.hash || '').replace('#', '');
    if (!hash || !activate(hash)) {
      activate(btns[0].getAttribute('data-tab'));
    }
  });
})();
