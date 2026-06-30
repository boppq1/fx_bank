from fastapi.middleware.cors import CORSMiddleware
from fastapi import FastAPI, Depends, HTTPException
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from contextlib import asynccontextmanager
from dotenv import load_dotenv
from services.review_service import run_review_analysis
from services.user_service import run_user_analysis
import jwt
import redis
import json
import os
import logging

load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 스케줄러

scheduler = BackgroundScheduler(timezone="Asia/Seoul")

def scheduled_review_analysis():
    logger.info("[스케줄러] 리뷰 자동 분석 시작")
    try:
        result = run_review_analysis()
        logger.info(f"[스케줄러] 완료: {result}")
    except Exception as e:
        logger.error(f"[스케줄러] 오류 발생: {e}")

scheduler.add_job(
    scheduled_review_analysis,
    trigger=CronTrigger(hour=10, minute=30),
    id="daily_review_analysis",
    replace_existing=True,
)

# 앱 수명 주기 

@asynccontextmanager
async def lifespan(app: FastAPI):
    scheduler.start()
    logger.info("[스케줄러] 시작 — 매일 10:30 KST 리뷰 분석 예약")
    yield
    scheduler.shutdown(wait=False)
    logger.info("[스케줄러] 종료")

# FastAPI 앱

app = FastAPI(title="FX Bank AI API", lifespan=lifespan)
security = HTTPBearer()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://klsbank.store"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

redis_client = redis.Redis(
    host=os.getenv("REDIS_HOST", "redis"),
    port=int(os.getenv("REDIS_PORT", 6379)),
    password=os.getenv("REDIS_PASSWORD") or None,
    decode_responses=True
)

# 인증

def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security)):
    token = credentials.credentials
    print("[DEBUG] token:", token)

    try:
        payload = jwt.decode(token, options={"verify_signature": False})
        user_id = payload.get("sub")
        print("[DEBUG] user_id:", user_id)
    except Exception as e:
        print("[DEBUG] JWT 디코드 실패:", e)
        raise HTTPException(status_code=401, detail="유효하지 않은 토큰")

    user_json = redis_client.get(f"USER:{user_id}")
    print("[DEBUG] user_json:", user_json)
    if not user_json:
        raise HTTPException(status_code=401, detail="만료된 세션 다시 로그인해야 됨")

    return json.loads(user_json)

# 라우터

@app.get("/")
def health_check():
    return {"status": "ok", "message": "FX Bank FastAPI 서버 정상 작동 중"}

@app.post("/analyze-reviews")
async def analyze_reviews():
    result = run_review_analysis()
    return result

@app.post("/analyze-user")
async def analyze_user(current_user: dict = Depends(get_current_user)):
    print("[DEBUG] current_user:", current_user)
    user_no = current_user.get("userNo")
    print("[DEBUG] user_no:", user_no)
    result = run_user_analysis(user_no)
    return result