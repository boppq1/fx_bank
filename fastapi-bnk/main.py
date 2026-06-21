from fastapi import FastAPI, UploadFile, File, HTTPException
from ultralytics import YOLO
import shutil, uuid, os

app = FastAPI()

# 모델 로드 (서버 시작 시 한 번만)
model = YOLO("bnk_best.pt")

UPLOAD_DIR = "/tmp/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.post("/detect")
async def detect(file: UploadFile = File(...)):
    # 이미지 저장
    file_path = f"{UPLOAD_DIR}/{uuid.uuid4()}.jpg"
    with open(file_path, "wb") as f:
        shutil.copyfileobj(file.file, f)

    try:
        # YOLO 추론
        results = model(file_path, conf=0.5)
        detected = [model.names[int(c)] for c in results[0].boxes.cls]

        # 중복 제거
        detected_set = set(detected)

        return {
            "detected": list(detected_set),
            "has_B": "B" in detected_set,
            "has_N": "N" in detected_set,
            "has_K": "K" in detected_set,
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    finally:
        # 임시 파일 삭제
        if os.path.exists(file_path):
            os.remove(file_path)