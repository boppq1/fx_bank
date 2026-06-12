from fastapi import FastAPI, UploadFile, File
from ultralytics import YOLO
from dotenv import load_dotenv

import os
import shutil
import cv2
import uuid
import requests
import time
import json
import re


# =========================
# .env 로드
# =========================
load_dotenv()

CLOVA_INVOKE_URL = os.getenv("CLOVA_INVOKE_URL")
CLOVA_SECRET_KEY = os.getenv("CLOVA_SECRET_KEY")

if not CLOVA_INVOKE_URL:
    raise RuntimeError("CLOVA_INVOKE_URL이 .env에 설정되지 않았습니다.")

if not CLOVA_SECRET_KEY:
    raise RuntimeError("CLOVA_SECRET_KEY가 .env에 설정되지 않았습니다.")

if not CLOVA_INVOKE_URL.endswith("/general"):
    CLOVA_INVOKE_URL = CLOVA_INVOKE_URL.rstrip("/") + "/general"


app = FastAPI()

# =========================
# YOLO 모델
# =========================
model = YOLO("models/best.pt")

UPLOAD_DIR = "uploads"
CROP_DIR = "crops"

os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(CROP_DIR, exist_ok=True)


@app.get("/")
def root():
    return {
        "message": "fastapi-ocr server is running"
    }


# =========================
# CLOVA OCR 호출 함수
# =========================
def clova_ocr_image(file_path):
    file_ext = os.path.splitext(file_path)[1].replace(".", "").lower()

    if file_ext == "jpeg":
        file_ext = "jpg"

    request_json = {
        "version": "V2",
        "requestId": str(uuid.uuid4()),
        "timestamp": int(time.time() * 1000),
        "lang": "ko",
        "images": [
            {
                "format": file_ext,
                "name": os.path.basename(file_path)
            }
        ]
    }

    headers = {
        "X-OCR-SECRET": CLOVA_SECRET_KEY
    }

    data = {
        "message": json.dumps(request_json, ensure_ascii=False)
    }

    with open(file_path, "rb") as f:
        response = requests.post(
            CLOVA_INVOKE_URL,
            headers=headers,
            data=data,
            files={"file": f},
            timeout=60
        )

    if response.status_code != 200:
        return {
            "success": False,
            "text": "",
            "statusCode": response.status_code,
            "error": response.text
        }

    result = response.json()

    texts = []

    for image in result.get("images", []):
        for field in image.get("fields", []):
            texts.append(field.get("inferText", ""))

    return {
        "success": True,
        "text": " ".join(texts),
        "raw": result
    }


# =========================
# 후처리 함수
# =========================
def extract_korean_name(text):
    korean_parts = re.findall(r"[가-힣]{2,4}", text)

    if korean_parts:
        return korean_parts[0]

    return text.strip()


def normalize_rrn(text):
    digits = re.sub(r"[^0-9]", "", text)

    if len(digits) >= 13:
        digits = digits[:13]
        return digits[:6] + "-" + digits[6:]

    return text.strip()


def mask_rrn(text):
    rrn = normalize_rrn(text)

    match = re.match(r"(\d{6})-(\d)(\d{6})", rrn)

    if match:
        return f"{match.group(1)}-{match.group(2)}******"

    return text.strip()


def clean_issue_date(text):
    digits = re.sub(r"[^0-9]", "", text)

    if len(digits) >= 8:
        return f"{digits[:4]}.{digits[4:6]}.{digits[6:8]}"

    return text.strip()


def clean_address(text):
    if not text:
        return ""

    # 1. 주민번호 패턴 제거
    text = re.sub(r"\d{6}[-\s]?\d{7}", " ", text)
    text = re.sub(r"\d{6}\s+\d{7}", " ", text)

    # 2. 면허증 배경 영어 문구 제거
    text = re.sub(r"[A-Za-z]+(?:'[A-Za-z]+)?", " ", text)

    # 3. 한글, 숫자, 공백, 주소에 필요한 기호만 남김
    text = re.sub(r"[^가-힣0-9\s,.\-()]", " ", text)

    # 4. 공백 정리
    text = re.sub(r"\s+", " ", text).strip()

    # 5. 주소 시작 지점 찾기
    region_pattern = (
        r"(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|"
        r"대전광역시|울산광역시|세종특별자치시|경기도|강원특별자치도|"
        r"충청북도|충청남도|전북특별자치도|전라남도|경상북도|경상남도|"
        r"제주특별자치도)"
    )

    match = re.search(region_pattern, text)

    if match:
        text = text[match.start():]

    # 6. 광역시/도 바로 뒤에 이상하게 끼는 단독 숫자 제거
    # 예: 부산광역시 3 연제구 -> 부산광역시 연제구
    text = re.sub(
        r"(특별시|광역시|특별자치시|특별자치도|도)\s+\d+\s+(?=[가-힣]+[시군구])",
        r"\1 ",
        text
    )

    # 7. 다시 공백 정리
    text = re.sub(r"\s+", " ", text).strip()

    return text


# =========================
# OCR API
# =========================
@app.post("/ocr/id-card")
async def ocr_id_card(file: UploadFile = File(...)):
    file_id = str(uuid.uuid4())
    upload_path = os.path.join(UPLOAD_DIR, f"{file_id}_{file.filename}")

    # 업로드 이미지 저장
    with open(upload_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    # YOLO 탐지
    results = model.predict(
        source=upload_path,
        conf=0.25
    )

    # 신분증 타입 판단
    id_type = "unknown"

    for r in results:
        for box in r.boxes:
            cls_id = int(box.cls[0])
            detected_label = model.names[cls_id]

            if detected_label == "driver_license":
                id_type = "driver_license"
            elif detected_label == "resident_card":
                id_type = "resident_card"

    print("신분증 타입:", id_type)

    target_labels = ["name", "rrn", "address", "issue_date"]

    saved_crops = []
    ocr_results = {}

    for r in results:
        orig_img = r.orig_img
        img_h, img_w = orig_img.shape[:2]

        for i, box in enumerate(r.boxes):
            cls_id = int(box.cls[0])
            label = model.names[cls_id]
            conf = float(box.conf[0])

            if label not in target_labels:
                continue

            x1, y1, x2, y2 = map(int, box.xyxy[0])

            box_w = x2 - x1
            box_h = y2 - y1

            # =========================
            # 라벨별 crop 여백 조정
            # =========================
            if label == "name":
                if id_type == "driver_license":
                    pad_left = int(box_w * 0.08)
                    pad_right = int(box_w * 0.35)
                    pad_top = int(box_h * 0.15)
                    pad_bottom = int(box_h * 0.15)
                else:
                    pad_left = int(box_w * 0.15)
                    pad_right = int(box_w * 1.20)
                    pad_top = int(box_h * 0.60)
                    pad_bottom = int(box_h * 0.60)

            elif label == "address":
                if id_type == "driver_license":
                    pad_left = int(box_w * 0.05)
                    pad_right = int(box_w * 0.05)
                    pad_top = int(box_h * 0.05)
                    pad_bottom = int(box_h * 0.05)
                else:
                    pad_left = int(box_w * 0.08)
                    pad_right = int(box_w * 0.08)
                    pad_top = int(box_h * 0.05)
                    pad_bottom = int(box_h * 0.05)

            elif label == "rrn":
                if id_type == "driver_license":
                    pad_left = int(box_w * 0.08)
                    pad_right = int(box_w * 0.08)
                    pad_top = int(box_h * 0.08)
                    pad_bottom = int(box_h * 0.08)
                else:
                    pad_left = int(box_w * 0.12)
                    pad_right = int(box_w * 0.12)
                    pad_top = int(box_h * 0.12)
                    pad_bottom = int(box_h * 0.12)

            elif label == "issue_date":
                if id_type == "driver_license":
                    pad_left = int(box_w * 0.08)
                    pad_right = int(box_w * 0.03)
                    pad_top = int(box_h * 0.10)
                    pad_bottom = int(box_h * 0.10)
                else:
                    pad_left = int(box_w * 0.15)
                    pad_right = int(box_w * 0.15)
                    pad_top = int(box_h * 0.15)
                    pad_bottom = int(box_h * 0.15)

            else:
                pad_left = int(box_w * 0.10)
                pad_right = int(box_w * 0.10)
                pad_top = int(box_h * 0.10)
                pad_bottom = int(box_h * 0.10)

            crop_x1 = max(0, x1 - pad_left)
            crop_y1 = max(0, y1 - pad_top)
            crop_x2 = min(img_w, x2 + pad_right)
            crop_y2 = min(img_h, y2 + pad_bottom)

            crop = orig_img[crop_y1:crop_y2, crop_x1:crop_x2]

            crop_path = os.path.join(CROP_DIR, f"{file_id}_{label}_{i}.jpg")
            cv2.imwrite(crop_path, crop)

            # CLOVA OCR 호출
            ocr_response = clova_ocr_image(crop_path)
            ocr_text = ocr_response.get("text", "")

            saved_crops.append({
                "label": label,
                "confidence": round(conf, 3),
                "cropPath": crop_path,
                "ocrText": ocr_text
            })

            if label not in ocr_results:
                ocr_results[label] = []

            if ocr_text:
                ocr_results[label].append(ocr_text)

    # =========================
    # OCR 결과 정리
    # =========================
    name_raw = " ".join(ocr_results.get("name", []))
    rrn_raw = " ".join(ocr_results.get("rrn", []))
    address_raw = " ".join(ocr_results.get("address", []))
    issue_date_raw = " ".join(ocr_results.get("issue_date", []))

    name_clean = extract_korean_name(name_raw)
    rrn_normalized = normalize_rrn(rrn_raw)
    rrn_masked = mask_rrn(rrn_raw)
    address_clean = clean_address(address_raw)
    issue_date_clean = clean_issue_date(issue_date_raw)

    return {
        "success": True,
        "message": "YOLO + CLOVA OCR complete",
        "idType": id_type,

        "name": name_clean,
        "rrnMasked": rrn_masked,
        "address": address_clean,
        "issueDate": issue_date_clean,

        "raw": {
            "name": name_raw,
            "rrn": rrn_raw,
            "rrnNormalized": rrn_normalized,
            "address": address_raw,
            "issueDate": issue_date_raw
        },

        "uploadPath": upload_path,
        "crops": saved_crops
    }