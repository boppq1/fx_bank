/* ============================================================
   header-auth.js  ·  헤더 로그인 상태 토글
   - 페이지 진입 시 /api/auth/refresh(HttpOnly 쿠키)로 accessToken 복구
   - 로그인 상태면: 로그인/회원가입 숨김 → "OOO님 안녕하세요" + 로그아웃 표시
   - 토큰 안의 nameKo 클레임으로 사용자 이름 표시
   ============================================================ */
(function () {
  document.addEventListener("DOMContentLoaded", initHeaderAuth);

  async function initHeaderAuth() {
    // 공용 single-flight 로 토큰 복구 (페이지 스크립트와 refresh 호출을 공유 → 중복/회전충돌 방지)
    const token = await window.acquireAccessToken();
    if (token) {
      showLoggedIn(getNameFromToken(token));
    } else {
      showGuest();
    }
  }

  /* JWT payload 의 nameKo 클레임 추출 (서명 검증은 서버 몫, 여기선 표시용 디코딩만) */
  function getNameFromToken(token) {
    try {
      const payload = token.split(".")[1];
      const json = decodeURIComponent(
        atob(payload.replace(/-/g, "+").replace(/_/g, "/"))
          .split("")
          .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
          .join("")
      );
      return JSON.parse(json).nameKo || "고객";
    } catch (e) {
      return "고객";
    }
  }

  function showLoggedIn(nameKo) {
    const greeting = document.getElementById("userGreeting");
    if (greeting) greeting.textContent = nameKo + "님 안녕하세요";

    document.querySelectorAll(".auth-guest").forEach((el) => (el.hidden = true));
    document.querySelectorAll(".auth-user").forEach((el) => (el.hidden = false));

    bindLogout(document.getElementById("logoutLink"));
    bindLogout(document.getElementById("gnbLogout"));
  }

  function showGuest() {
    document.querySelectorAll(".auth-guest").forEach((el) => (el.hidden = false));
    document.querySelectorAll(".auth-user").forEach((el) => (el.hidden = true));
  }

  function bindLogout(el) {
    if (!el) return;
    el.addEventListener("click", async (e) => {
      e.preventDefault();
      try {
        const headers = { "Content-Type": "application/json" };
        if (window.accessToken) headers["Authorization"] = "Bearer " + window.accessToken;
        await fetch("/api/auth/logout", { method: "POST", headers });
      } catch (err) {
        /* 로그아웃 통신 실패해도 클라이언트 상태는 초기화 */
      }
      window.accessToken = null;
      location.href = "/";
    });
  }
})();
