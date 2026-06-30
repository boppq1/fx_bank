window.accessToken = null;

let isIdChecked = false;
let checkedUserId = "";
let ocrToken = "";

document.addEventListener("DOMContentLoaded", function () {
  initTabs();
  initPasswordToggle();
  initLogin();
  initRegister();
  initIdCardOcr();
  initTermsAgreement();
  initPhoneHyphen();
});

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

function initTabs() {
  const tabs = document.querySelector(".auth-tabs");
  const buttons = document.querySelectorAll(".tab-btn");
  const panels = document.querySelectorAll(".panel");
  if (!tabs) return;

  function activate(name) {
    tabs.setAttribute("data-active", name);
    buttons.forEach((b) => b.classList.toggle("is-active", b.dataset.tab === name));
    panels.forEach((p) => p.classList.toggle("is-active", p.dataset.panel === name));
  }

  buttons.forEach((btn) => btn.addEventListener("click", () => activate(btn.dataset.tab)));
  document.querySelectorAll(".switch-link").forEach((link) => {
    link.addEventListener("click", () => activate(link.dataset.tab));
  });

  if (window.location.pathname.startsWith("/register")) activate("register");
}

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
  if (!userId || !secuPw) {
    alert("아이디와 비밀번호를 입력해 주세요.");
    return;
  }

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ userId, secuPw }),
    });
    const result = await response.json();

    if (result.success) {
      window.accessToken = result.data.accessToken;
      if (result.data.reauthRequired === "true") {
        alert("개인정보 보관기간이 지나 신분증 재인증이 필요합니다.");
        location.href = "/reauth";
        return;
      }
      const returnUrl = new URLSearchParams(location.search).get("returnUrl");
      location.href = (returnUrl && returnUrl.startsWith("/") && !returnUrl.startsWith("//")) ? returnUrl : "/";
    } else {
      alert(result.message || "로그인에 실패했습니다.");
    }
  } catch (error) {
    console.error("로그인 요청 실패:", error);
    alert("통신 오류가 발생했습니다.");
  }
}

function initRegister() {
  const form = document.getElementById("registerForm");
  if (form) form.addEventListener("submit", handleRegister);

  const idInput = document.getElementById("userId");
  if (idInput) {
    idInput.addEventListener("input", () => { isIdChecked = false; checkedUserId = ""; });
  }
}

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
          alert(res.message || "이미 사용 중인 아이디입니다.");
          isIdChecked = false;
        } else {
          alert(res.message || "사용 가능한 아이디입니다.");
          isIdChecked = true;
          checkedUserId = userIdInput;
        }
      } else {
        alert("검증 실패: " + (res.message || "아이디 중복 확인에 실패했습니다."));
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

  const pw = document.getElementById("secuPw").value;
  const pwOk = pw.length >= 8 && /[A-Za-z]/.test(pw) && /[0-9]/.test(pw) && /[^A-Za-z0-9]/.test(pw);
  if (!pwOk) {
    alert("비밀번호는 영문, 숫자, 특수문자를 모두 포함하여 8자 이상이어야 합니다.");
    return;
  }

  const rrnValue = document.getElementById("rrn").value.trim();
  const rrnDigits = rrnValue.replace(/[^0-9]/g, "");
  if (rrnValue.includes("*") || rrnDigits.length !== 13) {
    alert("주민등록번호 가려진 뒷자리를 정확히 입력해 주세요. (앞 6자리 + 뒤 7자리)");
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
    ocrToken,
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
      if (res.success) {
        alert(res.message || "회원가입이 완료되었습니다.");
        location.href = "/login";
      } else {
        alert("가입 실패: " + (res.message || "회원가입에 실패했습니다."));
      }
    })
    .catch((err) => alert("전송 오류: " + err));
}

function executeDaumPostcode() {
  new kakao.Postcode({
    oncomplete: function (data) {
      let addr = "";
      let extraAddr = "";
      if (data.userSelectedType === "R") addr = data.roadAddress;
      else addr = data.jibunAddress;

      if (data.userSelectedType === "R") {
        if (data.bname !== "" && /[동로가]$/.test(data.bname)) extraAddr += data.bname;
        if (data.buildingName !== "" && data.apartment === "Y") {
          extraAddr += extraAddr !== "" ? ", " + data.buildingName : data.buildingName;
        }
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

function initIdCardOcr() {
  const drop = document.getElementById("idCardDrop");
  const fileInput = document.getElementById("idCardFile");
  const modal = document.getElementById("authCameraModal");
  const video = document.getElementById("authCameraVideo");
  const canvas = document.getElementById("authCaptureCanvas");
  const captureBtn = document.getElementById("authCaptureButton");
  const closeBtn = document.getElementById("authCloseCameraButton");
  if (!drop || !fileInput) return;

  let stream = null;

  function stopCamera() {
    if (stream) {
      stream.getTracks().forEach(track => track.stop());
      stream = null;
    }
    if (video) video.srcObject = null;
    if (modal) modal.hidden = true;
  }

  function openCameraGuide() {
    fileInput.click();
  }

  function captureIdCard() {
    if (!video || !canvas || !video.videoWidth || !video.videoHeight) return;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext("2d");
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    canvas.toBlob(blob => {
      if (!blob) return;
      const file = new File([blob], "id-card-" + Date.now() + ".jpg", { type: "image/jpeg" });
      stopCamera();
      uploadIdCard(file);
    }, "image/jpeg", 0.92);
  }

  drop.addEventListener("click", openCameraGuide);
  fileInput.addEventListener("change", () => {
    if (fileInput.files && fileInput.files[0]) uploadIdCard(fileInput.files[0]);
  });
  if (captureBtn) captureBtn.addEventListener("click", captureIdCard);
  if (closeBtn) closeBtn.addEventListener("click", stopCamera);
}

async function uploadIdCard(file) {
  const textEl = document.getElementById("idCardText");
  setOcrStatus("신분증을 인식하는 중입니다...", "loading");

  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("/api/auth/ocr/id-card", { method: "POST", body: formData });
    const result = await response.json();

    if (!result.success) {
      ocrToken = "";
      updateRegisterSubmitState();
      setOcrStatus("인식 실패: " + (result.message || "신분증을 다시 촬영해 주세요."), "error");
      return;
    }

    const data = result.data || {};
    if (data.name) document.getElementById("nameKo").value = data.name;
    if (data.address) document.getElementById("address").value = data.address;
    if (data.rrnMasked) {
      const visiblePrefix = data.rrnMasked.split("*")[0];
      document.getElementById("rrn").value = visiblePrefix;
      document.getElementById("rrnMasked").value = data.rrnMasked;
    }

    ocrToken = data.ocrToken || "";
    updateRegisterSubmitState();

    if (textEl) textEl.textContent = "신분증 인증 완료. 필요한 항목은 직접 수정해 주세요.";
    setOcrStatus("신분증 인증 완료! 주민등록번호 가려진 뒷자리를 입력해 주세요.", "ok");
  } catch (err) {
    console.error("OCR 요청 실패:", err);
    ocrToken = "";
    updateRegisterSubmitState();
    setOcrStatus("OCR 서버 통신 오류가 발생했습니다.", "error");
  }
}

function setOcrStatus(msg, type) {
  const statusEl = document.getElementById("idCardStatus");
  if (!statusEl) return;
  statusEl.hidden = false;
  statusEl.textContent = msg;
  statusEl.className = "idcard-status is-" + type;
}

function initTermsAgreement() {
  const checkbox = document.getElementById("privacyAgreed");
  if (checkbox) checkbox.addEventListener("change", updateRegisterSubmitState);
  updateRegisterSubmitState();
}

function updateRegisterSubmitState() {
  const checkbox = document.getElementById("privacyAgreed");
  const submitBtn = document.getElementById("registerSubmit");
  if (!checkbox || !submitBtn) return;
  submitBtn.disabled = !checkbox.checked;
}