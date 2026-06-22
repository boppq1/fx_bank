from fastapi import FastAPI
from dotenv import load_dotenv
from services.review_service import run_review_analysis
from services.user_service import run_user_analysis

load_dotenv()

app = FastAPI(title="FX Bank AI API")

@app.get("/")
def health_check():
    return {"status": "ok", "message": "FX Bank FastAPI 서버 정상 작동 중"}

@app.post("/analyze-reviews")
async def analyze_reviews():
    result = run_review_analysis()
    return result

@app.post("/analyze-user/{user_no}")
async def analyze_user(user_no: int):
    result = run_user_analysis(user_no)
    return result