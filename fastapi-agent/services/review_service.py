import os
import json
from openai import OpenAI
from database import get_connection
from chroma_store import add_review, get_embedding

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def fetch_reviews():
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        SELECT r.review_no, r.product_no, r.review_text, r.rating
        FROM product_reviews r
        JOIN products p ON r.product_no = p.product_no
        WHERE p.active = 'Y'
        ORDER BY r.created_dt DESC
    """)
    rows = []
    for row in cursor.fetchall():
        review_no, product_no, review_text, rating = row
        # CLOB을 문자열로 변환 (연결 끊기 전에)
        if hasattr(review_text, 'read'):
            review_text = review_text.read()
        rows.append((review_no, product_no, review_text, rating))
    cursor.close()
    conn.close()
    return rows

def analyze_sentiment(review_text):
    prompt = f"""
아래 외화 금융 상품 리뷰를 분석해 주세요. 이 리뷰를 쓴 사용자가 선호하는 금융 상품 성향을 판단해야 합니다.
[리뷰]
{review_text}

반드시 아래 JSON 형식으로만 답하세요. 부연 설명은 하지 마세요.
{{
    "sentiment" : "긍정/부정/중립",
    "keywords" : ["키워드1", "키워드2", "키워드3"],
    "tendency" : "안정형/목표형/도전형/참여형/글로벌형"
}}
"""
    response = client.chat.completions.create(
        model="gpt-5-mini",
        messages=[{"role": "user", "content": prompt}]
    )
    return response.choices[0].message.content.strip()

def update_product_metadata(product_no, best_tendency, sentiment_score):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE products
        SET product_tendency = :tendency,
            sentiment_score = :score,
            updated_dt = SYSDATE
        WHERE product_no = :product_no
    """, {"tendency": best_tendency, "score": sentiment_score, "product_no": product_no})
    conn.commit()
    cursor.close()
    conn.close()

def run_review_analysis():
    print("[리뷰 분석] 시작")
    reviews = fetch_reviews()

    if not reviews:
        print("[리뷰 분석] 리뷰 없음")
        return {"status": "ok", "reviewed": 0}

    product_sentiments = {}
    product_tendency_data = {}

    for i, (review_no, product_no, review_text, rating) in enumerate(reviews):
        print(f"[{i+1}/{len(reviews)}] 리뷰 {review_no} 분석 중...")
        analysis = analyze_sentiment(review_text)
        try:
            parsed = json.loads(analysis)

            add_review(
                review_no=review_no,
                product_no=product_no,
                review_text=review_text,
                sentiment=parsed.get("sentiment"),
                keywords=parsed.get("keywords"),
                tendency=parsed.get("tendency"),
                embedding=get_embedding(review_text)
            )

            if product_no not in product_sentiments:
                product_sentiments[product_no] = []
            product_sentiments[product_no].append(parsed.get("sentiment"))

            if product_no not in product_tendency_data:
                product_tendency_data[product_no] = {}
            t = parsed.get("tendency")
            product_tendency_data[product_no][t] = product_tendency_data[product_no].get(t, 0) + 1

        except Exception as e:
            print(f"Error processing review {review_no}: {e}")

    for product_no in product_sentiments.keys():
        sentiments = product_sentiments[product_no]
        score = sentiments.count("긍정") - sentiments.count("부정")

        counts = product_tendency_data.get(product_no, {})
        best_tendency = max(counts, key=counts.get) if counts else "안정형"

        update_product_metadata(product_no, best_tendency, score)
        print(f"상품 {product_no} 업데이트 완료: 성향={best_tendency}, 점수={score}")

    return {"status": "ok", "reviewed": len(reviews)}