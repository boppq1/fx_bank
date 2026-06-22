const PRODUCT_NO = 1; // 이벤트 상품 번호

// 이벤트 참여 신청
function joinEvent() {
    fetch('/event/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `productNo=${PRODUCT_NO}`
    })
    .then(res => res.text())
    .then(() => {
        window.location.href = '/event/status';
    })
    .catch(err => alert('오류가 발생했습니다.'));
}

// 카메라 열기 (Flutter 앱에서만 동작)
function openCamera(letter) {
    FlutterBridge.postMessage(JSON.stringify({
        action: "openCamera",
        letter: letter,
        productNo: PRODUCT_NO
    }));
}

// Flutter에서 결과 받아서 페이지 업데이트
function updateResult(resultJson) {
    const result = JSON.parse(resultJson);
    if (result.isApplied === 'Y') {
        alert('🎉 B, N, K 모두 인증 완료! 우대금리가 적용되었습니다.');
    } else {
        alert('인증 완료! 계속 도전하세요.');
    }
    window.location.reload();
}