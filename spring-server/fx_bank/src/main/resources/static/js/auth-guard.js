// 페이지가 켜지자마자 실행되는 공통 로그인 체크 스크립트
function goLogin() {
    if (window.redirectToLogin) { window.redirectToLogin(); return; }
    location.href = '/login?returnUrl=' + encodeURIComponent(location.pathname + location.search + location.hash);
}

(async function checkAuth() {
    // 화면을 일단 숨김 (HTML body에 style="display:none"이 없어도 JS로 즉시 숨김)
    document.documentElement.style.display = 'none';

    try {
        const response = await fetch('/api/auth/refresh', { method: 'POST' });
        const result = await response.json();

        if (result.success) {
            window.accessToken = result.data.accessToken;
            // 인증 성공하면 화면 보여주기
            document.documentElement.style.display = 'block';
        } else {
            alert("로그인이 필요한 페이지입니다.");
            goLogin();
        }
    } catch (error) {
        goLogin();
    }
})();