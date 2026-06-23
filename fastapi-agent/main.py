from fastapi.middleware.cors import CORSMiddleware
from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from dotenv import load_dotenv
from services.review_service import run_review_analysis
from services.user_service import run_user_analysis
import jwt
import redis
import json
import os

load_dotenv()

app = FastAPI(title="FX Bank AI API")
security = HTTPBearer()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],  # 프론트 주소
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

redis_client = redis.Redis(
    host=os.getenv("REDIS_HOST"),
    port=int(os.getenv("REDIS_PORT", 6379)),
    password=os.getenv("REDIS_PASSWORD") or None,
    decode_responses=True
)

def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    print("[DEBUG] token:", token)

    if redis_client.get(f"BLACKLIST:{token}"):
        raise HTTPException(status_code=401, detail="로그아웃된 토큰이에요")

    try:
        # 서명 검증 없이 payload만 추출
        payload = jwt.decode(token, options={"verify_signature": False})
        user_id = payload.get("sub")
        print("[DEBUG] user_id:", user_id)
    except Exception as e:
        print("[DEBUG] JWT 디코드 실패:", e)
        raise HTTPException(status_code=401, detail="토큰이 유효하지 않아요")

    user_json = redis_client.get(f"USER:{user_id}")
    print("[DEBUG] user_json:", user_json)
    if not user_json:
        raise HTTPException(status_code=401, detail="세션이 만료됐어요. 다시 로그인해주세요")

    return json.loads(user_json)

@app.get("/")
def health_check():
    return {"status": "ok", "message": "FX Bank FastAPI 서버 정상 작동 중"}

@app.post("/analyze-reviews")
async def analyze_reviews():
    result = run_review_analysis()
    return result

@app.post("/analyze-user")
async def analyze_user(current_user: dict = Depends(get_current_user)):
    print("[DEBUG] current_user:", current_user)  # 추가
    user_no = current_user.get("userNo")
    print("[DEBUG] user_no:", user_no)  # 추가
    result = run_user_analysis(user_no)
    return result