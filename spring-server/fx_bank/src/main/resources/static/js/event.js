function getEventReturnUrl() {
    return window.location.pathname + window.location.search + window.location.hash;
}

function redirectToLogin(returnUrl) {
    window.location.href = '/login?returnUrl=' + encodeURIComponent(returnUrl || getEventReturnUrl());
}

function isUnauthorizedResponse(res) {
    return res.status === 401 || (res.redirected && res.url && res.url.includes('/login'));
}

function joinEvent() {
    fetch('/event/join', {
        method: 'POST',
        credentials: 'same-origin'
    })
    .then(async res => {
        if (isUnauthorizedResponse(res)) {
            redirectToLogin('/event');
            throw new Error('__LOGIN_REDIRECT__');
        }
        if (!res.ok) {
            const message = await readErrorMessage(res);
            throw new Error(message || '이벤트 참여 처리에 실패했습니다.');
        }
        return res.text();
    })
    .then(() => {
        window.location.href = '/event/status';
    })
    .catch(err => {
        if (err && err.message === '__LOGIN_REDIRECT__') return;
        showEventDialog({
            title: '이벤트 참여 실패',
            message: getReadableError(err),
            type: 'error'
        });
    });
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
    try {
        const byteString = atob(base64Image);
        const ab = new ArrayBuffer(byteString.length);
        const ia = new Uint8Array(ab);
        for (let i = 0; i < byteString.length; i++) {
            ia[i] = byteString.charCodeAt(i);
        }
        const blob = new Blob([ab], { type: 'image/jpeg' });
        uploadEventImage(letter, blob, 'capture.jpg');
    } catch (error) {
        showEventDialog({
            title: '이미지 처리 실패',
            message: '촬영한 이미지를 처리하지 못했습니다. 다시 시도해주세요.',
            type: 'error'
        });
    }
};

function uploadEventImage(letter, fileOrBlob, fileName) {
    const formData = new FormData();
    formData.append('file', fileOrBlob, fileName || 'capture.jpg');
    formData.append('letter', letter);

    fetch('/event/detect', {
        method: 'POST',
        body: formData,
        credentials: 'same-origin'
    })
    .then(async res => {
        if (isUnauthorizedResponse(res)) {
            redirectToLogin('/event/status');
            throw new Error('__LOGIN_REDIRECT__');
        }
        if (!res.ok) {
            const message = await readErrorMessage(res);
            throw new Error(message || `${letter} 글자를 찾지 못했습니다. 다시 촬영해주세요.`);
        }
        return res.json();
    })
    .then(result => {
        showEventResult(result);
    })
    .catch(err => {
        if (err && err.message === '__LOGIN_REDIRECT__') return;
        showEventDialog({
            title: '인증 실패',
            message: getReadableError(err),
            type: 'error'
        });
    });
}

function updateResult(resultJson) {
    try {
        const result = typeof resultJson === 'string' ? JSON.parse(resultJson) : resultJson;
        showEventResult(result);
    } catch (error) {
        showEventDialog({
            title: '처리 오류',
            message: '인증 결과를 처리하지 못했습니다. 다시 시도해주세요.',
            type: 'error'
        });
    }
}

function showEventResult(result) {
    const applied = result && (result.applied || result.isApplied);
    if (applied === 'Y') {
        showEventDialog({
            title: '이벤트 완료',
            message: 'B, N, K 인증 완료! 우대금리 쿠폰이 발급되었습니다.',
            type: 'success',
            onClose: () => window.location.reload()
        });
    } else {
        showEventDialog({
            title: '인증 완료',
            message: '남은 글자를 계속 찾아주세요.',
            type: 'success',
            onClose: () => window.location.reload()
        });
    }
}

function showEventDialog({ title, message, type = 'success', onClose }) {
    let overlay = document.querySelector('.event-dialog-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'event-dialog-overlay';
        overlay.innerHTML = `
            <div class="event-dialog" role="dialog" aria-modal="true" aria-labelledby="eventDialogTitle">
                <div class="event-dialog-icon" aria-hidden="true"></div>
                <h3 id="eventDialogTitle"></h3>
                <p></p>
                <button type="button" class="event-dialog-button">확인</button>
            </div>
        `;
        document.body.appendChild(overlay);
    }

    const dialog = overlay.querySelector('.event-dialog');
    const icon = overlay.querySelector('.event-dialog-icon');
    const titleEl = overlay.querySelector('h3');
    const messageEl = overlay.querySelector('p');
    const button = overlay.querySelector('.event-dialog-button');

    dialog.classList.toggle('is-error', type === 'error');
    icon.textContent = type === 'error' ? '!' : '✓';
    titleEl.textContent = title || '알림';
    messageEl.textContent = message || '';

    const close = () => {
        overlay.classList.remove('is-open');
        document.body.classList.remove('event-dialog-lock');
        button.removeEventListener('click', close);
        if (typeof onClose === 'function') onClose();
    };

    button.addEventListener('click', close);
    document.body.classList.add('event-dialog-lock');
    overlay.classList.add('is-open');
    setTimeout(() => button.focus(), 30);
}

async function readErrorMessage(res) {
    const contentType = res.headers.get('content-type') || '';
    try {
        if (contentType.includes('application/json')) {
            const body = await res.json();
            return body.message || body.error || body.detail || '';
        }
        const text = await res.text();
        if (!text || text.includes('<html') || text.includes('Internal Server Error')) return '';
        return text;
    } catch (error) {
        return '';
    }
}

function getReadableError(err) {
    const message = err && err.message ? err.message : String(err || '알 수 없는 오류');
    if (message.includes('Failed to fetch')) return '서버와 통신하지 못했습니다.';
    if (message.includes('Internal Server Error')) return '서버 처리 중 오류가 발생했습니다.';
    return message;
}