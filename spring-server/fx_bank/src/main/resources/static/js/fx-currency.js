/* fx-currency.js — 통화 코드 ↔ 한글명/검색어 공용 사전
 * 파라미터는 통화코드(USD 등)로 보내되, 화면 표기는 "미국 달러 (USD)" 형태로.
 * 검색은 코드/한글 국가명/통화명 모두 매칭.
 */
window.FXCUR = (function () {
  var M = {
    'USD':      { ko: '미국 달러',           kw: '미국 달러 us dollar 유에스' },
    'JPY(100)': { ko: '일본 엔 (100엔)',     kw: '일본 엔 엔화 japan yen' },
    'EUR':      { ko: '유로',                kw: '유럽연합 유로 euro eu' },
    'CNH':      { ko: '중국 위안',           kw: '중국 위안 위안화 china yuan cny cnh' },
    'GBP':      { ko: '영국 파운드',         kw: '영국 파운드 uk pound britain' },
    'AUD':      { ko: '호주 달러',           kw: '호주 오스트레일리아 australia' },
    'CAD':      { ko: '캐나다 달러',         kw: '캐나다 canada' },
    'CHF':      { ko: '스위스 프랑',         kw: '스위스 프랑 swiss franc' },
    'HKD':      { ko: '홍콩 달러',           kw: '홍콩 hongkong' },
    'SGD':      { ko: '싱가포르 달러',       kw: '싱가포르 singapore' },
    'THB':      { ko: '태국 바트',           kw: '태국 바트 thai baht' },
    'AED':      { ko: '아랍에미리트 디르함', kw: '아랍에미리트 uae 두바이 디르함 dirham' },
    'BHD':      { ko: '바레인 디나르',       kw: '바레인 디나르 bahrain' },
    'BND':      { ko: '브루나이 달러',       kw: '브루나이 brunei' },
    'DKK':      { ko: '덴마크 크로네',       kw: '덴마크 크로네 denmark krone' },
    'IDR(100)': { ko: '인도네시아 루피아 (100)', kw: '인도네시아 루피아 indonesia rupiah' },
    'KWD':      { ko: '쿠웨이트 디나르',     kw: '쿠웨이트 디나르 kuwait' },
    'MYR':      { ko: '말레이시아 링깃',     kw: '말레이시아 링깃 malaysia ringgit' },
    'NOK':      { ko: '노르웨이 크로네',     kw: '노르웨이 크로네 norway' },
    'NZD':      { ko: '뉴질랜드 달러',       kw: '뉴질랜드 newzealand' },
    'SAR':      { ko: '사우디 리얄',         kw: '사우디 리얄 saudi riyal' },
    'SEK':      { ko: '스웨덴 크로나',       kw: '스웨덴 크로나 sweden krona' },
    'KRW':      { ko: '대한민국 원',         kw: '한국 대한민국 원 korea won' }
  };
  var PRIORITY = ['USD', 'EUR', 'JPY(100)', 'CNH', 'GBP', 'AUD', 'CAD', 'CHF', 'HKD', 'SGD'];

  function name(c) { return (M[c] && M[c].ko) || c; }
  function label(c) { return name(c) + ' (' + c + ')'; }
  function initials(c) { return (c || '').replace(/[^A-Za-z]/g, '').substring(0, 2).toUpperCase() || '–'; }
  function priority(c) { var i = PRIORITY.indexOf(c); return i < 0 ? 999 : i; }
  function matches(c, q) {
    if (!q) return true;
    q = String(q).trim().toLowerCase();
    if (!q) return true;
    var hay = (c + ' ' + name(c) + ' ' + ((M[c] && M[c].kw) || '')).toLowerCase();
    return hay.indexOf(q) !== -1;
  }
  /** 통화코드 목록을 우선순위→이름순 정렬 */
  function sortCodes(codes) {
    return codes.slice().sort(function (a, b) {
      var pa = priority(a), pb = priority(b);
      if (pa !== pb) return pa - pb;
      return name(a) < name(b) ? -1 : 1;
    });
  }
  return { name: name, label: label, initials: initials, priority: priority, matches: matches, sortCodes: sortCodes };
})();
