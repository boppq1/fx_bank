/* ============================================================
   auth.js  ·  로그인 / 회원가입
   - 탭 슬라이드 전환 (알약만 이동, 폼은 즉시 교체)
   - 로그인: accessToken을 RAM(메모리 변수)에만 보관 (XSS 탈취 차단)
   - 회원가입: 신분증 OCR 인증(필수) + 중복확인 + 카카오 우편번호 + payload POST
   ============================================================ */

window.accessToken = null;

/* 회원가입 상태 */
let isIdChecked = false;
let checkedUserId = "";
let isOcrVerified = false;   // 신분증 OCR 인증 완료 여부 (필수)
let ocrToken = "";           // OCR 성공 시 서버가 발급한 1회용 인증 토큰

document.addEventListener("DOMContentLoaded", function () {
  initTabs();
  initPasswordToggle();
  initLogin();
  initRegister();
  initIdCardOcr();
  initTermsAgreement();
  initPhoneHyphen();
});

/* 휴대폰 번호 자동 하이픈 */
function initPhoneHyphen() {
  const phone = document.getElementById("phone");
  if (!phone) return;
  phone.addEventListener("input", () => {
    let d = phone.value.replace(/[^0-9]/g, "").slice(0, 11);
    if (d.length < 4) phone.value = d;
    else if (d.length < 8) phone.value = d.slice(0, 3) + "-" + d.slice(3);
    else phone.value = d.slice(0, 3) + "-" + d.slice(3, 7) + "-" + d.slice(7);
  });
}

/* ============================================================
   1) 탭 전환
   ============================================================ */
function initTabs() {
  const tabs = document.querySelector(".auth-tabs");
  const buttons = document.querySelectorAll(".tab-btn");
  const panels = document.querySelectorAll(".panel");

  function activate(name) {
    tabs.setAttribute("data-active", name);
    document.querySelectorAll(".tab-btn").forEach((b) => {
      b.classList.toggle("is-active", b.dataset.tab === name);
    });
    panels.forEach((p) => {
      p.classList.toggle("is-active", p.dataset.panel === name);
    });
  }

  buttons.forEach((btn) => btn.addEventListener("click", () => activate(btn.dataset.tab)));
  document.querySelectorAll(".switch-link").forEach((link) => {
    link.addEventListener("click", () => activate(link.dataset.tab));
  });

  if (window.location.pathname.startsWith("/register")) activate("register");
}

/* ============================================================
   2) 비밀번호 표시 토글
   ============================================================ */
function initPasswordToggle() {
  document.querySelectorAll(".pw-toggle").forEach((btn) => {
    btn.addEventListener("click", () => {
      const input = document.getElementById(btn.dataset.target);
      if (!input) return;
      const show = input.type === "password";
      input.type = show ? "text" : "password";
      btn.style.opacity = show ? "0.9" : "0.5";
    });
  });
}

/* ============================================================
   3) 로그인
   ============================================================ */
function initLogin() {
  const btn = document.getElementById("loginBtn");
  if (btn) btn.addEventListener("click", handleLogin);
  ["loginId", "loginPw"].forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.addEventListener("keydown", (e) => { if (e.key === "Enter") handleLogin(); });
  });
}

async function handleLogin() {
  const userId = document.getElementById("loginId").value.trim();
  const secuPw = document.getElementById("loginPw").value;
  if (!userId || !secuPw) { alert("아이디와 비밀번호를 입력해 주세요."); return; }

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId: userId, secuPw: secuPw }),
    });
    const result = await response.json();
    if (result.success) {
      window.accessToken = result.data.accessToken;
      if (result.data.reauthRequired === "true") {
        alert("개인정보 보관기한(3일)이 지나 신분증 재인증이 필요합니다.");
        location.href = "/reauth";
        return;
      }
      location.href = "/";
    } else {
      alert(result.message);
    }
  } catch (error) {
    console.error("로그인 요청 실패:", error);
    alert("통신 오류가 발생했습니다.");
  }
}

/* ============================================================
   4) 회원가입
   ============================================================ */
function initRegister() {
  const form = document.getElementById("registerForm");
  if (form) form.addEventListener("submit", handleRegister);

  const idInput = document.getElementById("userId");
  if (idInput) {
    idInput.addEventListener("input", () => { isIdChecked = false; checkedUserId = ""; });
  }
}

/* 아이디 중복 확인 (전역 — onclick 인라인에서 호출) */
function checkDuplicateId() {
  const userIdInput = document.getElementById("userId").value.trim();
  if (!userIdInput) { alert("아이디를 입력해 주세요."); return; }

  fetch(`/api/auth/check-id?userId=${encodeURIComponent(userIdInput)}`)
    .then((response) => response.json())
    .then((res) => {
      if (res.success) {
        if (res.data === true) { alert(res.message); isIdChecked = false; }
        else { alert(res.message); isIdChecked = true; checkedUserId = userIdInput; }
      } else {
        alert("검증 실패: " + res.message);
      }
    })
    .catch((err) => alert("통신 오류: " + err));
}

function handleRegister(e) {
  e.preventDefault();

  // 0) 신분증 OCR 인증(필수) — 가장 먼저 막는다
  if (!isOcrVerified || !ocrToken) {
    alert("신분증 OCR 인증을 먼저 완료해 주세요.");
    return;
  }

  const currentUserId = document.getElementById("userId").value.trim();
  if (!isIdChecked || checkedUserId !== currentUserId) {
    alert("아이디 중복 확인이 필요합니다.");
    return;
  }

  const pw = document.getElementById("secuPw").value;
  const pwOk = pw.length >= 8 && /[A-Za-z]/.test(pw) && /[0-9]/.test(pw) && /[^A-Za-z0-9]/.test(pw);
  if (!pwOk) {
    alert("비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 8자 이상이어야 합니다.");
    return;
  }

  const rrnValue = document.getElementById("rrn").value.trim();
  const rrnDigits = rrnValue.replace(/[^0-9]/g, "");
  if (rrnValue.includes("*") || rrnDigits.length !== 13) {
    alert("주민등록번호 가려진 뒷자리를 정확히 입력해 주세요. (앞6 + 뒤7)");
    return;
  }

  if (!document.getElementById("privacyAgreed").checked) {
    alert("개인정보 수집 및 이용에 동의해 주세요.");
    return;
  }

  const payload = {
    userId: currentUserId,
    secuPw: document.getElementById("secuPw").value,
    nameKo: document.getElementById("nameKo").value,
    nameEn: document.getElementById("nameEn").value,
    rrn: document.getElementById("rrn").value,
    rrnMasked: document.getElementById("rrnMasked").value,
    privacyAgreed: document.getElementById("privacyAgreed").checked,
    ocrToken: ocrToken,               // 서버에서 OCR 인증 검증·소비
    phone: document.getElementById("phone").value,
    email: document.getElementById("email").value,
    addrKo: document.getElementById("address").value,
    addrDetailKo: document.getElementById("address_detail").value,
    zipCodeKo: document.getElementById("postcode").value,
    addrEn: document.getElementById("address_en").value,
    addrDetailEn: document.getElementById("address_detail_en").value,
    zipCodeEn: document.getElementById("postcode_en").value,
    gender: document.getElementById("gender").value,
  };

  fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
    .then((response) => response.json())
    .then((res) => {
      if (res.success) { alert(res.message); location.href = "/login"; }
      else { console.log(res.message); alert("가입 실패: " + res.message); }
    })
    .catch((err) => alert("전송 에러: " + err));
}

/* ============================================================
   5) 카카오 우편번호 (한글/영문 동시 매핑)
   ============================================================ */
function executeDaumPostcode() {
  new kakao.Postcode({
    oncomplete: function (data) {
      var addr = "";
      var extraAddr = "";
      if (data.userSelectedType === "R") addr = data.roadAddress;
      else addr = data.jibunAddress;

      if (data.userSelectedType === "R") {
        if (data.bname !== "" && /[동로가]$/.test(data.bname)) extraAddr += data.bname;
        if (data.buildingName !== "" && data.apartment === "Y")
          extraAddr += extraAddr !== "" ? ", " + data.buildingName : data.buildingName;
        if (extraAddr !== "") extraAddr = " (" + extraAddr + ")";
      }

      document.getElementById("postcode").value = data.zonecode;
      document.getElementById("address").value = addr + extraAddr;
      document.getElementById("postcode_en").value = data.zonecode;
      document.getElementById("address_en").value = data.roadAddressEnglish;
      document.getElementById("address_detail").focus();
    },
  }).open();
}

/* ============================================================
   6) 신분증 OCR (FastAPI 중계) — 이름/주소/주민번호 자동 채움 + 인증 토큰 수령
   ============================================================ */
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
  const textEl = document.getElementById("idCardText");

  setOcrStatus("⏳ 신분증을 인식하는 중입니다...", "loading");
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("/api/auth/ocr/id-card", { method: "POST", body: formData });
    const result = await response.json();

    if (!result.success) {
      // 인증 실패 → 인증 상태 해제
      isOcrVerified = false;
      ocrToken = "";
      updateRegisterSubmitState();
      setOcrStatus("❌ 인식 실패: " + result.message, "error");
      return;
    }

    const data = result.data || {};
    if (data.name) document.getElementById("nameKo").value = data.name;
    if (data.address) document.getElementById("address").value = data.address;
    if (data.rrnMasked) {
      const visiblePrefix = data.rrnMasked.split("*")[0]; // "030830-4"
      document.getElementById("rrn").value = visiblePrefix;
      document.getElementById("rrnMasked").value = data.rrnMasked;
    }

    // ★ OCR 인증 완료 처리 (토큰 보관 + 가입 버튼 활성 조건 갱신)
    isOcrVerified = true;
    ocrToken = data.ocrToken || "";
    updateRegisterSubmitState();

    if (textEl) textEl.textContent = "✅ 신분증 인증 완료 (필요 시 직접 수정하세요)";
    setOcrStatus("✅ 신분증 인증 완료! 주민번호 가려진 뒷자리(뒤 6자리)를 입력해 주세요.", "ok");
  } catch (err) {
    console.error("OCR 요청 실패:", err);
    isOcrVerified = false;
    ocrToken = "";
    updateRegisterSubmitState();
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

/* ============================================================
   7) 가입 버튼 활성/비활성 — 개인정보 동의 + 신분증 OCR 인증 둘 다 충족해야 활성
   ============================================================ */
function initTermsAgreement() {
  const checkbox = document.getElementById("privacyAgreed");
  if (checkbox) checkbox.addEventListener("change", updateRegisterSubmitState);
  updateRegisterSubmitState();
}

function updateRegisterSubmitState() {
  const checkbox = document.getElementById("privacyAgreed");
  const submitBtn = document.getElementById("registerSubmit");
  if (!checkbox || !submitBtn) return;
  submitBtn.disabled = !(checkbox.checked && isOcrVerified);
}
