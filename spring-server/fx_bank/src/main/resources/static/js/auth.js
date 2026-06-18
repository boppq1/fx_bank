/* ============================================================
   auth.js  ·  로그인 / 회원가입
   - 탭 슬라이드 전환 (알약만 이동, 폼은 즉시 교체)
   - 로그인: accessToken을 RAM(메모리 변수)에만 보관 (XSS 탈취 차단)
   - 회원가입: 중복확인 + 카카오 우편번호(한/영) + payload POST
   ============================================================ */

/* ★핵심 보안: 토큰은 localStorage가 아닌 JS 메모리 변수에만 보관.
   페이지 이동 시 증발하지만, index 진입 시 /api/auth/refresh(HttpOnly 쿠키)로 복구.
   index.html 전역 변수와 일관성을 위해 window.accessToken 사용. */
window.accessToken = null;

/* 회원가입 아이디 중복 확인 상태 */
let isIdChecked = false;
let checkedUserId = "";

document.addEventListener("DOMContentLoaded", function () {
  initTabs();
  initPasswordToggle();
  initLogin();
  initRegister();
  initIdCardOcr();
  initTermsAgreement();
  initPhoneHyphen();
});

/* 휴대폰 번호 입력 시 자동 하이픈 (010-1234-5678 형식) */
function initPhoneHyphen() {
  const phone = document.getElementById("phone");
  if (!phone) return;
  phone.addEventListener("input", () => {
    let d = phone.value.replace(/[^0-9]/g, "").slice(0, 11); // 숫자만, 최대 11자리
    if (d.length < 4) {
      phone.value = d;
    } else if (d.length < 8) {
      phone.value = d.slice(0, 3) + "-" + d.slice(3);
    } else {
      phone.value = d.slice(0, 3) + "-" + d.slice(3, 7) + "-" + d.slice(7);
    }
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
    // 알약 위치
    tabs.setAttribute("data-active", name);
    // 탭 버튼 활성 표시
    document.querySelectorAll(".tab-btn").forEach((b) => {
      b.classList.toggle("is-active", b.dataset.tab === name);
    });
    // 패널 즉시 교체
    panels.forEach((p) => {
      p.classList.toggle("is-active", p.dataset.panel === name);
    });
  }

  // 상단 탭 버튼
  buttons.forEach((btn) => {
    btn.addEventListener("click", () => activate(btn.dataset.tab));
  });

  // 하단 "회원가입 / 로그인" 전환 링크
  document.querySelectorAll(".switch-link").forEach((link) => {
    link.addEventListener("click", () => activate(link.dataset.tab));
  });

  // /register 로 진입하면 회원가입 탭을 기본으로 활성화 (login/register가 같은 fragment 공유)
  if (window.location.pathname.startsWith("/register")) {
    activate("register");
  }
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

  // Enter 키로 로그인
  ["loginId", "loginPw"].forEach((id) => {
    const el = document.getElementById(id);
    if (el) {
      el.addEventListener("keydown", (e) => {
        if (e.key === "Enter") handleLogin();
      });
    }
  });
}

async function handleLogin() {
  const userId = document.getElementById("loginId").value.trim();
  const secuPw = document.getElementById("loginPw").value;

  if (!userId || !secuPw) {
    alert("아이디와 비밀번호를 입력해 주세요.");
    return;
  }

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId: userId, secuPw: secuPw }),
    });

    const result = await response.json();

    if (result.success) {
      // 오직 RAM 메모리 변수에만 할당 (localStorage 사용 안 함)
      window.accessToken = result.data.accessToken;

      // 주민번호 보관 만료(3일 경과)/부재 시 OCR 재인증 화면으로 유도
      if (result.data.reauthRequired === "true") {
        alert("개인정보 보관기한(3일)이 지나 신분증 재인증이 필요합니다.");
        location.href = "/reauth";
        return;
      }

      // index 진입 시 /api/auth/refresh 로 access token 을 복구하므로 바로 이동
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

  // 아이디를 다시 수정하면 중복확인 상태 초기화
  const idInput = document.getElementById("userId");
  if (idInput) {
    idInput.addEventListener("input", () => {
      isIdChecked = false;
      checkedUserId = "";
    });
  }
}

/* 아이디 중복 확인 (전역 — onclick 인라인에서 호출) */
function checkDuplicateId() {
  const userIdInput = document.getElementById("userId").value.trim();

  if (!userIdInput) {
    alert("아이디를 입력해 주세요.");
    return;
  }

  fetch(`/api/auth/check-id?userId=${encodeURIComponent(userIdInput)}`)
    .then((response) => response.json())
    .then((res) => {
      if (res.success) {
        if (res.data === true) {
          alert(res.message); // 이미 사용 중
          isIdChecked = false;
        } else {
          alert(res.message); // 사용 가능
          isIdChecked = true;
          checkedUserId = userIdInput;
        }
      } else {
        alert("검증 실패: " + res.message);
      }
    })
    .catch((err) => alert("통신 오류: " + err));
}

function handleRegister(e) {
  e.preventDefault();

  const currentUserId = document.getElementById("userId").value.trim();

  if (!isIdChecked || checkedUserId !== currentUserId) {
    alert("아이디 중복 확인이 필요합니다.");
    return;
  }

  // 비밀번호 정책: 영문 + 숫자 + 특수문자 포함, 8자 이상
  const pw = document.getElementById("secuPw").value;
  const pwOk =
    pw.length >= 8 &&
    /[A-Za-z]/.test(pw) &&
    /[0-9]/.test(pw) &&
    /[^A-Za-z0-9]/.test(pw);
  if (!pwOk) {
    alert("비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 8자 이상이어야 합니다.");
    return;
  }

  // 주민등록번호: 마스킹(*) 보정 여부 및 13자리 검증
  const rrnValue = document.getElementById("rrn").value.trim();
  const rrnDigits = rrnValue.replace(/[^0-9]/g, "");
  if (rrnValue.includes("*") || rrnDigits.length !== 13) {
    alert("주민등록번호 가려진 뒷자리를 정확히 입력해 주세요. (앞6 + 뒤7)");
    return;
  }

  // 개인정보 동의 검증 (서버에서도 재검증됨)
  if (!document.getElementById("privacyAgreed").checked) {
    alert("개인정보 수집 및 이용에 동의해 주세요.");
    return;
  }

  // 백엔드 RegisterRequestDto 필드명과 일치
  const payload = {
    userId: currentUserId,
    secuPw: document.getElementById("secuPw").value,
    nameKo: document.getElementById("nameKo").value,
    nameEn: document.getElementById("nameEn").value,
    rrn: document.getElementById("rrn").value,
    rrnMasked: document.getElementById("rrnMasked").value,
    privacyAgreed: document.getElementById("privacyAgreed").checked,
    phone: document.getElementById("phone").value,
    email: document.getElementById("email").value,
    addrKo: document.getElementById("address").value,
    addrDetailKo: document.getElementById("address_detail").value,
    zipCodeKo: document.getElementById("postcode").value,
    addrEn: document.getElementById("address_en").value,
    addrDetailEn: document.getElementById("address_detail_en").value,
    zipCodeEn: document.getElementById("postcode_en").value,
    gender: document.getElementById("gender").value,
    // userTendency(투자 성향)는 가입 시 받지 않고, 추후 AI 에이전트가 측정/저장
  };

  fetch("/api/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  })
    .then((response) => response.json())
    .then((res) => {
      if (res.success) {
        alert(res.message);
        location.href = "/login";
      } else {
        console.log(res.message);
        alert("가입 실패: " + res.message);
      }
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

      if (data.userSelectedType === "R") {
        addr = data.roadAddress;
      } else {
        addr = data.jibunAddress;
      }

      if (data.userSelectedType === "R") {
        if (data.bname !== "" && /[동로가]$/.test(data.bname)) {
          extraAddr += data.bname;
        }
        if (data.buildingName !== "" && data.apartment === "Y") {
          extraAddr += extraAddr !== "" ? ", " + data.buildingName : data.buildingName;
        }
        if (extraAddr !== "") {
          extraAddr = " (" + extraAddr + ")";
        }
      }

      // 한글 주소
      document.getElementById("postcode").value = data.zonecode;
      document.getElementById("address").value = addr + extraAddr;

      // 영문 주소 (추가 호출 없이 한 번에)
      document.getElementById("postcode_en").value = data.zonecode;
      document.getElementById("address_en").value = data.roadAddressEnglish;

      document.getElementById("address_detail").focus();
    },
  }).open();
}

/* ============================================================
   6) 신분증 OCR (FastAPI 중계) — 이름/주소/주민번호 자동 채움
   - 백엔드가 CLOVA 키를 숨긴 채 프록시하므로 프론트엔 키가 없음
   ============================================================ */
function initIdCardOcr() {
  const drop = document.getElementById("idCardDrop");
  const fileInput = document.getElementById("idCardFile");
  if (!drop || !fileInput) return;

  drop.addEventListener("click", () => fileInput.click());
  fileInput.addEventListener("change", () => {
    if (fileInput.files && fileInput.files[0]) {
      uploadIdCard(fileInput.files[0]);
    }
  });
}

async function uploadIdCard(file) {
  const statusEl = document.getElementById("idCardStatus");
  const textEl = document.getElementById("idCardText");

  setOcrStatus("⏳ 신분증을 인식하는 중입니다...", "loading");
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("/api/auth/ocr/id-card", {
      method: "POST",
      body: formData, // Content-Type 은 브라우저가 boundary 와 함께 자동 설정
    });
    const result = await response.json();

    if (!result.success) {
      setOcrStatus("❌ 인식 실패: " + result.message, "error");
      return;
    }

    const data = result.data || {};
    // 자동 채움 (사용자가 이후 직접 수정 가능)
    if (data.name) document.getElementById("nameKo").value = data.name;
    if (data.address) document.getElementById("address").value = data.address;
    if (data.rrnMasked) {
      // 마스킹값(예: 030830-4******)에서 가려지지 않은 앞부분만 입력란에 채운다.
      // 별표(*)가 그대로 저장되지 않도록 사용자가 뒷자리를 직접 입력하게 유도.
      const visiblePrefix = data.rrnMasked.split("*")[0]; // "030830-4"
      document.getElementById("rrn").value = visiblePrefix;
      document.getElementById("rrnMasked").value = data.rrnMasked;
    }

    if (textEl) textEl.textContent = "✅ 신분증 인식 완료 (필요 시 직접 수정하세요)";
    setOcrStatus("주민번호 가려진 뒷자리(뒤 6자리)를 직접 입력해 주세요.", "ok");
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

/* ============================================================
   7) 개인정보 동의 체크 → 가입 버튼 활성/비활성
   ============================================================ */
function initTermsAgreement() {
  const checkbox = document.getElementById("privacyAgreed");
  const submitBtn = document.getElementById("registerSubmit");
  if (!checkbox || !submitBtn) return;

  const sync = () => {
    submitBtn.disabled = !checkbox.checked;
  };
  checkbox.addEventListener("change", sync);
  sync();
}
