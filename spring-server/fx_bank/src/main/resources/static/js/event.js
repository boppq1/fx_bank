// 이벤트 참여 신청
function joinEvent() {
    fetch('/event/join', {
        method: 'POST'
    })
    .then(res => {
        if (!res.ok) throw new Error('참여 처리 실패');
        return res.text();
    })
    .then(() => {
        window.location.href = '/event/status';
    })
    .catch(err => alert('오류가 발생했습니다: ' + err.message));
}

// 카메라 열기 (Flutter 앱에서만 동작)
function openCamera(letter) {
    FlutterEventBridge.postMessage(JSON.stringify({   // FlutterBridge → FlutterEventBridge
        action: "openCamera",
        letter: letter
    }));
}

// Flutter가 사진 찍은 뒤 호출하는 콜백 (main.dart의 window.onCameraResult와 일치)
window.onCameraResult = function(letter, base64Image) {
    // base64 문자열을 파일(Blob)로 변환해서 서버에 업로드
    const byteString = atob(base64Image);
    const ab = new ArrayBuffer(byteString.length);
    const ia = new Uint8Array(ab);
    for (let i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i);
    }
    const blob = new Blob([ab], { type: 'image/jpeg' });

    const formData = new FormData();
    formData.append('file', blob, 'capture.jpg');
	formData.append('letter', letter);

    fetch('/event/detect', {
        method: 'POST',
        body: formData
    })
    .then(res => {
        if (!res.ok) throw new Error('인증 처리 실패');
        return res.json();
    })
    .then(result => {
        if (result.applied === 'Y') {
            alert('🎉 B, N, K 모두 인증 완료! 우대금리 쿠폰이 발급되었습니다.');
        } else {
            alert('인증 완료! 계속 도전하세요.');
        }
        window.location.reload();   // 현황 페이지 새로고침해서 ✅ 반영
    })
    .catch(err => alert('오류: ' + err.message));
};

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