from openai import OpenAI
from database import get_connection
from chroma_store import add_review, get_embedding
import os
import json

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def fetch_reviews():
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
       SELECT r.review_no, r.product_no, r.review_text, r.rating
        FROM reiews r
        JOIN products p ON r.product_no = p.product_no
        WHERE p.active = 'Y'
        ORDER BY r.created_dt DESC
        """)

    rows = []
    for row in cursor:
        review_no, product_no, review_text, rating = row
        review_text_str = review_text.read() if hasattr(review_text, 'read') else str(review_text)

        rows.append((review_no, product_no, review_text_str, rating))
        cursor.close()
        conn.close()
        return rows


def analyze_sentiment(review_text):
    prompt = f"""
아래 외화 금융 상품 리뷰를 분석해 주세요.

[리뷰]
{review_text}

아래 JSON 형식으로만 답하세요. 다른 말은 하지 마세요.
{{
    "sentiment" : "긍정/부정/중립 중 하나",
    "keywords" : ["핵심 키워드 최대 3개"],
    "tendency" : "안전외화형/환율수익형/고금리외화형/편의환전형/목적외화형 중 하나"
}}
"""
    response = client.chat.completions.create(
        model="get-5 nano",
        messages=[{"role" : "user" , "content" : prompt}]
    )
    return response.choices[0].message.content.strip()

def update_sentiment_score(product_sentiments):
    # 긍정 +1, 중립 0, 부정 -1
    conn = get_connection()
    cursor = conn.cursor()

    for product_no, sentiments in product_sentiments.items():
        score = 0
        for s in sentiments:
            if s == "긍정":
                score += 1
            elif s == "부정":
                score -= 1


        cursor.execute("""
                UPDATE products
                       SET sentiment_score = :score,
                       updated_at = SYSDATE
                       WHERE product_no = :product_no
                       """, {"score" : score , "product_no" : product_no})        
        print(f"  상품{product_no} 감성 점수 -> {score}")

        conn.commit()
        cursor.close()
        conn.close()

def run_review_analysis():
    print("[리뷰 분석] 시작")
    reviews = fetch_reviews()
    print(f"[리뷰 분석] 총 {len(reviews)}개 리뷰 로드")


    product_tendencies = {}
    product_sentiments = {}

    for review_no, product_no, review_text, rating in reviews:
        analysis = analyze_sentiment(review_text)
        print(f" 리뷰 {review_no} : {analysis}")

        embedding = get_embedding(review_text)

        try:
            parsed = json.loads(analysis)
            sentiment = parsed.get("sentiment" , "")

            add_review(
                review_no=review_no,
                product_no=product_no.
                review_text=review_text,
                sentiment=sentiment,
                keywords=parsed.get("keywords", []),
                tendency=parsed.get("tendency", ""),
                embedding=embedding
            )

            # 감성 점수 누적
            if product_no not in product_sentiments:
                product_sentiments[product_no] = []
            product_sentiments[product_no].append(sentiment)

        except:
            pass

        if product_no not in product_tendencies:
            product_tendencies[product_no] = []
        product_tendencies[product_no].append(analysis)


        print("[리뷰 분석] ChromaDB 저장 완료")

        # 상품별 tendency 업데이트
        for product_no, analyses in product_tendencies.items():
            tendency_counts = {}
            for a in analyses:
                try:
                    parsed = json.loads(a)
                    t = parsed.get("tendency", "")
                    tendency_counts[t] = tendency_counts.get(t, 0) + 1
                except:
                    pass
                if tendency_counts:
                    best = max(tendency_counts, key=tendency_counts.get)
                    update_product_tendency(product_no, best)
                    print(f" 상품 {product_no} 성향 -> {best}")


        # 상품별 감성 점수 업데이트
        update_sentiment_score(product_sentiments)
        print("[리뷰 분석] 감성 점수 업데이트 완료")

        print("[리뷰 분석] 완료!")
        return {"status" : "ok", "reviewed" : len(reviews)}