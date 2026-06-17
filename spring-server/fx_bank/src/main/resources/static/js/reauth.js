/* ============================================================
   reauth.js  ·  주민번호 보관 만료 후 OCR 재인증
   - 로그인 시 accessToken 은 메모리에만 있고 페이지 이동 시 증발하므로,
     진입 시 /api/auth/refresh(HttpOnly 쿠키)로 accessToken 을 복구한다.
   - 신분증 OCR 로 주민번호 자동 채움 → 보정 → /api/auth/reauth 로 제출
   ============================================================ */
window.accessToken = null;

document.addEventListener("DOMContentLoaded", async function () {
  await recoverAccessToken();
  initIdCardOcr();
  initReauthForm();
});

/* 메모리 휘발된 accessToken 을 쿠키 기반 refresh 로 복구 */
async function recoverAccessToken() {
  try {
    const res = await fetch("/api/auth/refresh", { method: "POST" });
    const result = await res.json();
    if (result.success) {
      window.accessToken = result.data.accessToken;
    } else {
      alert("로그인이 필요합니다. 다시 로그인해 주세요.");
      location.href = "/login";
    }
  } catch (e) {
    alert("인증 정보를 확인할 수 없습니다. 다시 로그인해 주세요.");
    location.href = "/login";
  }
}

/* 신분증 OCR 업로드 (회원가입과 동일한 중계 엔드포인트) */
function initIdCardOcr() {
  const drop = document.getElementById("idCardDrop");
  const fileInput = document.getElementById("idCardFile");
  if (!drop || !fileInput) return;

  drop.addEventListener("click", () => fileInput.click());
  fileInput.addEventListener("change", () => {
    if (fileInput.files && fileInput.files[0]) uploadIdCard(fileInput.files[0]);
  });
}

async function uploadIdCard(file) {
  setOcrStatus("⏳ 신분증을 인식하는 중입니다...", "loading");
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("/api/auth/ocr/id-card", { method: "POST", body: formData });
    const result = await response.json();

    if (!result.success) {
      setOcrStatus("❌ 인식 실패: " + result.message, "error");
      return;
    }

    const data = result.data || {};
    if (data.rrnMasked) {
      document.getElementById("rrn").value = data.rrnMasked;
      document.getElementById("rrnMasked").value = data.rrnMasked;
    }
    document.getElementById("idCardText").textContent = "✅ 신분증 인식 완료";
    setOcrStatus("주민번호 가려진 뒷자리(뒤 7자리)를 직접 입력해 주세요.", "ok");
  } catch (err) {
    console.error("OCR 요청 실패:", err);
    setOcrStatus("❌ OCR 서버 통신 오류가 발생했습니다.", "error");
  }
}

function setOcrStatus(msg, type) {
  const statusEl = document.getElementById("idCardStatus");
  if (!statusEl) return;
  statusEl.hidden = false;
  statusEl.textContent = msg;
  statusEl.className = "idcard-status is-" + type;
}

/* 재인증 제출 */
function initReauthForm() {
  const form = document.getElementById("reauthForm");
  if (form) form.addEventListener("submit", handleReauth);
}

async function handleReauth(e) {
  e.preventDefault();

  const rrn = document.getElementById("rrn").value.trim();
  if (!rrn) {
    alert("주민등록번호를 입력해 주세요.");
    return;
  }
  if (!window.accessToken) {
    alert("로그인이 필요합니다.");
    location.href = "/login";
    return;
  }

  try {
    const response = await fetch("/api/auth/reauth", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: "Bearer " + window.accessToken,
      },
      body: JSON.stringify({
        rrn: rrn,
        rrnMasked: document.getElementById("rrnMasked").value,
      }),
    });
    const result = await response.json();

    if (result.success) {
      alert(result.message);
      location.href = "/";
    } else {
      alert("재인증 실패: " + result.message);
    }
  } catch (err) {
    alert("통신 오류가 발생했습니다.");
  }
}
