/* Shared auth helpers: token refresh is single-flight, login redirect keeps the current page. */
(function () {
  window.getCurrentReturnUrl = function () {
    return location.pathname + location.search + location.hash;
  };

  window.redirectToLogin = function (returnUrl) {
    const target = returnUrl || window.getCurrentReturnUrl();
    location.href = '/login?returnUrl=' + encodeURIComponent(target);
  };

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