/* ============================================================
   auth.js  쨌  濡쒓렇??/ ?뚯썝媛??
   - ???щ씪?대뱶 ?꾪솚 (?뚯빟留??대룞, ?쇱? 利됱떆 援먯껜)
   - 濡쒓렇?? accessToken??RAM(硫붾え由?蹂???먮쭔 蹂닿? (XSS ?덉랬 李⑤떒)
   - ?뚯썝媛?? ?좊텇利?OCR ?몄쬆(?꾩닔) + 以묐났?뺤씤 + 移댁뭅???고렪踰덊샇 + payload POST
   ============================================================ */

window.accessToken = null;

/* ?뚯썝媛???곹깭 */
let isIdChecked = false;
let checkedUserId = "";
/*let isOcrVerified = false;*/   // ?좊텇利?OCR ?몄쬆 ?꾨즺 ?щ? (?꾩닔)
let ocrToken = "";           // OCR ?깃났 ???쒕쾭媛 諛쒓툒??1?뚯슜 ?몄쬆 ?좏겙

document.addEventListener("DOMContentLoaded", function () {
  initTabs();
  initPasswordToggle();
  initLogin();
  initRegister();
  initIdCardOcr();
  initTermsAgreement();
  initPhoneHyphen();
});

/* ?대???踰덊샇 ?먮룞 ?섏씠??*/
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
   1) ???꾪솚
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
   2) 鍮꾨?踰덊샇 ?쒖떆 ?좉?
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
   3) 濡쒓렇??
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
  if (!userId || !secuPw) { alert("?꾩씠?붿? 鍮꾨?踰덊샇瑜??낅젰??二쇱꽭??"); return; }

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
        alert("媛쒖씤?뺣낫 蹂닿?湲고븳(3????吏???좊텇利??ъ씤利앹씠 ?꾩슂?⑸땲??");
        location.href = "/reauth";
        return;
      }
      // returnUrl ???덇퀬 ?대? 寃쎈줈("/"濡??쒖옉, "//" ?꾨떂)???뚮쭔 洹몄そ?쇰줈 蹂듦?, ?놁쑝硫?湲곗〈泥섎읆 硫붿씤.
      // (JWT 諛쒓툒/Redis/?좏겙 蹂닿? ???몄쬆 濡쒖쭅怨?臾닿????붾㈃ ?대룞留?遺꾧린 ???ㅽ뵂 由щ떎?대젆??諛⑹?)
      const returnUrl = new URLSearchParams(location.search).get("returnUrl");
      location.href = (returnUrl && returnUrl.startsWith("/") && !returnUrl.startsWith("//")) ? returnUrl : "/";
    } else {
      alert(result.message);
    }
  } catch (error) {
    console.error("濡쒓렇???붿껌 ?ㅽ뙣:", error);
    alert("?듭떊 ?ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.");
  }
}

/* ============================================================
   4) ?뚯썝媛??
   ============================================================ */
function initRegister() {
  const form = document.getElementById("registerForm");
  if (form) form.addEventListener("submit", handleRegister);

  const idInput = document.getElementById("userId");
  if (idInput) {
    idInput.addEventListener("input", () => { isIdChecked = false; checkedUserId = ""; });
  }
}

/* ?꾩씠??以묐났 ?뺤씤 (?꾩뿭 ??onclick ?몃씪?몄뿉???몄텧) */
function checkDuplicateId() {
  const userIdInput = document.getElementById("userId").value.trim();
  if (!userIdInput) { alert("?꾩씠?붾? ?낅젰??二쇱꽭??"); return; }

  fetch(`/api/auth/check-id?userId=${encodeURIComponent(userIdInput)}`)
    .then((response) => response.json())
    .then((res) => {
      if (res.success) {
        if (res.data === true) { alert(res.message); isIdChecked = false; }
        else { alert(res.message); isIdChecked = true; checkedUserId = userIdInput; }
      } else {
        alert("寃利??ㅽ뙣: " + res.message);
      }
    })
    .catch((err) => alert("?듭떊 ?ㅻ쪟: " + err));
}

function handleRegister(e) {
  e.preventDefault();

  // 0) ?좊텇利?OCR ?몄쬆(?꾩닔) ??媛??癒쇱? 留됰뒗??
/*  if (!isOcrVerified || !ocrToken) {
    alert("?좊텇利?OCR ?몄쬆??癒쇱? ?꾨즺??二쇱꽭??");
    return;
  }*/

  const currentUserId = document.getElementById("userId").value.trim();
  if (!isIdChecked || checkedUserId !== currentUserId) {
    alert("?꾩씠??以묐났 ?뺤씤???꾩슂?⑸땲??");
    return;
  }

  const pw = document.getElementById("secuPw").value;
  const pwOk = pw.length >= 8 && /[A-Za-z]/.test(pw) && /[0-9]/.test(pw) && /[^A-Za-z0-9]/.test(pw);
  if (!pwOk) {
    alert("鍮꾨?踰덊샇???곷Ц, ?レ옄, ?뱀닔臾몄옄瑜?紐⑤몢 ?ы븿?섏뿬 8???댁긽?댁뼱???⑸땲??");
    return;
  }

  const rrnValue = document.getElementById("rrn").value.trim();
  const rrnDigits = rrnValue.replace(/[^0-9]/g, "");
  if (rrnValue.includes("*") || rrnDigits.length !== 13) {
    alert("二쇰??깅줉踰덊샇 媛?ㅼ쭊 ?룹옄由щ? ?뺥솗???낅젰??二쇱꽭?? (?? + ??)");
    return;
  }

  if (!document.getElementById("privacyAgreed").checked) {
    alert("媛쒖씤?뺣낫 ?섏쭛 諛??댁슜???숈쓽??二쇱꽭??");
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
    ocrToken: ocrToken,               // ?쒕쾭?먯꽌 OCR ?몄쬆 寃利씲룹냼鍮?
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
      else { console.log(res.message); alert("媛???ㅽ뙣: " + res.message); }
    })
    .catch((err) => alert("?꾩넚 ?먮윭: " + err));
}

/* ============================================================
   5) 移댁뭅???고렪踰덊샇 (?쒓?/?곷Ц ?숈떆 留ㅽ븨)
   ============================================================ */
function executeDaumPostcode() {
  new kakao.Postcode({
    oncomplete: function (data) {
      var addr = "";
      var extraAddr = "";
      if (data.userSelectedType === "R") addr = data.roadAddress;
      else addr = data.jibunAddress;

      if (data.userSelectedType === "R") {
        if (data.bname !== "" && /[?숇줈媛]$/.test(data.bname)) extraAddr += data.bname;
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
   6) ?좊텇利?OCR (FastAPI 以묎퀎) ???대쫫/二쇱냼/二쇰?踰덊샇 ?먮룞 梨꾩? + ?몄쬆 ?좏겙 ?섎졊
   ============================================================ */
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

  async function openCameraGuide() {
    // 모바일 WebView에서는 getUserMedia 실패 후 fileInput.click()을 호출하면
    // 사용자 직접 클릭으로 인정되지 않아 카메라가 안 열릴 수 있다.
    // 회원가입 OCR은 안정성을 우선해서 네이티브 파일/카메라 선택창을 바로 연다.
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

  setOcrStatus("???좊텇利앹쓣 ?몄떇?섎뒗 以묒엯?덈떎...", "loading");
  try {
    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch("/api/auth/ocr/id-card", { method: "POST", body: formData });
    const result = await response.json();

    if (!result.success) {
      // ?몄쬆 ?ㅽ뙣 ???몄쬆 ?곹깭 ?댁젣
      /*isOcrVerified = false;*/
      ocrToken = "";
      updateRegisterSubmitState();
      setOcrStatus("???몄떇 ?ㅽ뙣: " + result.message, "error");
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

    // ??OCR ?몄쬆 ?꾨즺 泥섎━ (?좏겙 蹂닿? + 媛??踰꾪듉 ?쒖꽦 議곌굔 媛깆떊)
    /*isOcrVerified = true;*/
    ocrToken = data.ocrToken || "";
    updateRegisterSubmitState();

    if (textEl) textEl.textContent = "???좊텇利??몄쬆 ?꾨즺 (?꾩슂 ??吏곸젒 ?섏젙?섏꽭??";
    setOcrStatus("???좊텇利??몄쬆 ?꾨즺! 二쇰?踰덊샇 媛?ㅼ쭊 ?룹옄由???6?먮━)瑜??낅젰??二쇱꽭??", "ok");
  } catch (err) {
    console.error("OCR ?붿껌 ?ㅽ뙣:", err);
    /*isOcrVerified = false;*/
    ocrToken = "";
    updateRegisterSubmitState();
    setOcrStatus("??OCR ?쒕쾭 ?듭떊 ?ㅻ쪟媛 諛쒖깮?덉뒿?덈떎.", "error");
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
   7) 媛??踰꾪듉 ?쒖꽦/鍮꾪솢????媛쒖씤?뺣낫 ?숈쓽 + ?좊텇利?OCR ?몄쬆 ????異⑹”?댁빞 ?쒖꽦
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
  submitBtn.disabled = !(checkbox.checked);
}
