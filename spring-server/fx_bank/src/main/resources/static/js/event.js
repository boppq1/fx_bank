function joinEvent() {
    fetch('/event/join', {
        method: 'POST'
    })
    .then(res => {
        if (!res.ok) throw new Error('李몄뿬 泥섎━ ?ㅽ뙣');
        return res.text();
    })
    .then(() => {
        window.location.href = '/event/status';
    })
    .catch(err => alert('?ㅻ쪟媛 諛쒖깮?덉뒿?덈떎: ' + err.message));
}

function openCamera(letter) {
    try {
        if (window.FlutterEventBridge && typeof window.FlutterEventBridge.postMessage === 'function') {
            window.FlutterEventBridge.postMessage(JSON.stringify({
                action: 'openCamera',
                letter: letter
            }));
            return;
        }
    } catch (error) {
        console.warn('Flutter camera bridge failed. Falling back to browser camera.', error);
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
        if (!res.ok) throw new Error('?몄쬆 泥섎━ ?ㅽ뙣');
        return res.json();
    })
    .then(result => {
        if (result.applied === 'Y') {
            alert('B, N, K 紐⑤몢 ?몄쬆 ?꾨즺! ?곕?湲덈━ 荑좏룿??諛쒓툒?섏뿀?듬땲??');
        } else {
            alert('?몄쬆 ?꾨즺! 怨꾩냽 吏꾪뻾??二쇱꽭??');
        }
        window.location.reload();
    })
    .catch(err => alert('?ㅻ쪟: ' + err.message));
}

function updateResult(resultJson) {
    const result = JSON.parse(resultJson);
    if (result.isApplied === 'Y') {
        alert('B, N, K 紐⑤몢 ?몄쬆 ?꾨즺! ?곕?湲덈━媛 ?곸슜?섏뿀?듬땲??');
    } else {
        alert('?몄쬆 ?꾨즺! 怨꾩냽 吏꾪뻾??二쇱꽭??');
    }
    window.location.reload();
}