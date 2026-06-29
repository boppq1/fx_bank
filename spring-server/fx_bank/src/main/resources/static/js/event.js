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

function openCamera(letter) {
    if (window.FlutterEventBridge && typeof window.FlutterEventBridge.postMessage === 'function') {
        window.FlutterEventBridge.postMessage(JSON.stringify({
            action: 'openCamera',
            letter: letter
        }));
        return;
    }

    openBrowserCamera(letter);
}

function openBrowserCamera(letter) {
    let input = document.getElementById('eventCameraInput');
    if (!input) {
        input = document.createElement('input');
        input.id = 'eventCameraInput';
        input.type = 'file';
        input.accept = 'image/*';
        input.capture = 'environment';
        input.style.display = 'none';
        document.body.appendChild(input);
    }

    input.onchange = function () {
        const file = input.files && input.files[0];
        input.value = '';
        if (!file) return;
        uploadEventImage(letter, file);
    };

    input.click();
}

window.onCameraResult = function(letter, base64Image) {
    const byteString = atob(base64Image);
    const ab = new ArrayBuffer(byteString.length);
    const ia = new Uint8Array(ab);
    for (let i = 0; i < byteString.length; i++) {
        ia[i] = byteString.charCodeAt(i);
    }
    const blob = new Blob([ab], { type: 'image/jpeg' });
    uploadEventImage(letter, blob, 'capture.jpg');
};

function uploadEventImage(letter, fileOrBlob, fileName) {
    const formData = new FormData();
    formData.append('file', fileOrBlob, fileName || 'capture.jpg');
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
            alert('B, N, K 모두 인증 완료! 우대금리 쿠폰이 발급되었습니다.');
        } else {
            alert('인증 완료! 계속 진행해 주세요.');
        }
        window.location.reload();
    })
    .catch(err => alert('오류: ' + err.message));
}

function updateResult(resultJson) {
    const result = JSON.parse(resultJson);
    if (result.isApplied === 'Y') {
        alert('B, N, K 모두 인증 완료! 우대금리가 적용되었습니다.');
    } else {
        alert('인증 완료! 계속 진행해 주세요.');
    }
    window.location.reload();
}