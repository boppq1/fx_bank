/* ============================================================
   auth-refresh.js  ·  공용 single-flight 토큰 재발급
   - header-auth.js 와 각 페이지가 이 함수 하나(window.acquireAccessToken)를 공유한다.
   - 한 페이지에서 refresh 가 동시에 여러 번 호출돼도 실제 /api/auth/refresh 네트워크 요청은
     '진행 중 1건'으로 합쳐진다(single-flight). → RTR(refresh 토큰 회전)이 한 번만 일어나
     '동시 회전 충돌로 로그인 페이지로 튕기는' 문제를 없앤다. (서버/RTR 정책은 무변경)
   - 토큰을 받으면 window.accessToken 에 저장하고 반환, 실패하면 null 반환.
     (실패 시 /login 리다이렉트 여부는 호출하는 페이지가 각자 결정)
   ============================================================ */
(function () {
  window.acquireAccessToken = function () {
    if (window.accessToken) {
      return Promise.resolve(window.accessToken);
    }
    if (!window.__authRefreshPromise) {
      window.__authRefreshPromise = fetch('/api/auth/refresh', {
        method: 'POST',
        credentials: 'same-origin'
      })
        .then(function (res) { return res.json(); })
        .then(function (body) {
          window.__authRefreshPromise = null;
          if (body && body.success && body.data && body.data.accessToken) {
            window.accessToken = body.data.accessToken;
            return window.accessToken;
          }
          return null;
        })
        .catch(function () {
          window.__authRefreshPromise = null;
          return null;
        });
    }
    return window.__authRefreshPromise;
  };
})();
