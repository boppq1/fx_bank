from openai import OpenAI
from database import get_connection
from chroma_store import get_embedding, search_similar
from datetime import date
import os

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

AGE_DEFAULT_MBTI = {
    "20대": "RFGA",
    "30대": "RPGA",
    "40대": "SPGA",
    "50대이상": "SPLW",
}

MBTI_TO_TENDENCY = {
    "S": "안전외화형",
    "R": "환율수익형",
    "P": "목적외화형",
    "F": "편의환전형",
    "G": "고금리외화형",
    "L": "안전외화형",
    "A": "환율수익형",
    "W": "안전외화형",
}

MBTI_DESCRIPTIONS = {
    "SPGA": "안전을 추구하면서도 글로벌한 목표를 향해 적극적으로 나아가는 타입이에요 🌍",
    "SPGW": "안전하게 글로벌 목표를 세우고 신중하게 기회를 기다리는 타입이에요 🌏",
    "SPLA": "안전하게 국내에서 목표를 향해 적극적으로 나아가는 타입이에요 🏦",
    "SPLW": "안전하게 국내에서 목표를 세우고 신중하게 관망하는 타입이에요 🏠",
    "SFGA": "안전하면서도 자유롭게 글로벌하게 적극적으로 활동하는 타입이에요 ✈️",
    "SFGW": "안전하면서도 자유롭게 글로벌 시장을 관망하는 타입이에요 🌐",
    "SFLA": "안전하게 국내에서 자유롭고 적극적으로 활동하는 타입이에요 💳",
    "SFLW": "안전하게 국내에서 편하게 관망하는 타입이에요 😊",
    "RPGA": "도전적으로 목표를 세우고 글로벌하게 적극적으로 투자하는 타입이에요 🚀",
    "RPGW": "도전적이지만 글로벌 목표를 신중하게 기다리는 타입이에요 📊",
    "RPLA": "도전적으로 국내에서 목표를 향해 적극적으로 나아가는 타입이에요 💪",
    "RPLW": "도전적이지만 국내에서 신중하게 관망하는 타입이에요 🤔",
    "RFGA": "도전적이고 자유롭게 글로벌하게 적극적으로 활동하는 타입이에요 🔥",
    "RFGW": "도전적이고 자유롭게 글로벌 시장을 관망하는 타입이에요 👀",
    "RFLA": "도전적이고 자유롭게 국내에서 적극적으로 활동하는 타입이에요 ⚡",
    "RFLW": "도전적이고 자유롭지만 신중하게 관망하는 타입이에요 🎯",
}

def calculate_age(birth_date):
    today = date.today()
    age = today.year - birth_date.year
    if (today.month, today.day) < (birth_date.month, birth_date.day):
        age -= 1
    return age

def get_age_group(age):
    if age < 30:
        return "20대"
    elif age < 40:
        return "30대"
    elif age < 50:
        return "40대"
    else:
        return "50대이상"

def update_user_birth_date(user_no, birth_date):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE users
        SET birth_date = :birth_date
        WHERE user_no = :user_no
    """, {"birth_date": birth_date, "user_no": user_no})
    conn.commit()
    cursor.close()
    conn.close()

def fetch_user_info(user_no):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        SELECT u.gender, usi.rrn_masked
        FROM users u
        LEFT JOIN user_sensitive_infos usi ON u.user_no = usi.user_no
        WHERE u.user_no = :user_no
    """, {"user_no": user_no})
    row = cursor.fetchone()
    cursor.close()
    conn.close()

    if not row:
        return None

    gender, rrn_masked = row
    birth_date = None
    if rrn_masked:
        try:
            parts = rrn_masked.split("-")
            rrn_front = parts[0]               # 앞 6자리: 940512
            gender_code = int(parts[1][0])     # 뒷자리 첫 번째 숫자: 1

            year = int(rrn_front[:2])
            month = int(rrn_front[2:4])
            day = int(rrn_front[4:6])

            # 성별 코드로 세기 확정
            # 1,2 → 1900년대 / 3,4 → 2000년대 / 9,0 → 1800년대
            if gender_code in (1, 2):
                year = 1900 + year
            elif gender_code in (3, 4):
                year = 2000 + year
            else:
                year = 1800 + year

            birth_date = date(year, month, day)
        except:
            birth_date = None

    # 파싱한 생년월일을 users 테이블에 업데이트
    if birth_date:
        update_user_birth_date(user_no, birth_date)

    return gender, birth_date

def fetch_user_logs(user_no):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        SELECT activity_type, search_keyword, product_no
        FROM user_logs
        WHERE user_no = :user_no
        ORDER BY created_dt DESC
        FETCH FIRST 50 ROWS ONLY
    """, {"user_no": user_no})
    rows = cursor.fetchall()
    cursor.close()
    conn.close()
    return rows

def calculate_mbti_score(logs):
    scores = {
        "S": 0, "R": 0,
        "P": 0, "F": 0,
        "L": 0, "G": 0,
        "A": 0, "W": 0,
    }

    S_KEYWORDS = ["안전", "원금보장", "보장", "안심", "손실없음", "확정금리"]
    R_KEYWORDS = ["환율", "수익", "환차익", "타이밍", "매도", "단기", "투자"]
    P_KEYWORDS = ["유학", "이민", "목표", "여행", "장기", "적립", "계획"]
    F_KEYWORDS = ["간편", "즉시", "수수료", "편의", "빠른", "실시간", "비대면"]
    L_KEYWORDS = ["국내", "원화", "적금", "예금"]
    G_KEYWORDS = ["달러", "AUD", "USD", "외화", "글로벌", "해외", "NZD"]
    A_KEYWORDS = ["가입", "신청", "적극", "도전"]

    for activity, keyword, product_no in logs:
        if activity in ["PRODUCT_APPLY"]:
            scores["A"] += 2
        elif activity in ["PRODUCT_VIEW", "BANNER_CLICK"]:
            scores["A"] += 1
        elif activity == "CATEGORY_VIEW":
            scores["W"] += 1

        if not keyword:
            continue

        for kw in S_KEYWORDS:
            if kw in keyword: scores["S"] += 1
        for kw in R_KEYWORDS:
            if kw in keyword: scores["R"] += 1
        for kw in P_KEYWORDS:
            if kw in keyword: scores["P"] += 1
        for kw in F_KEYWORDS:
            if kw in keyword: scores["F"] += 1
        for kw in L_KEYWORDS:
            if kw in keyword: scores["L"] += 1
        for kw in G_KEYWORDS:
            if kw in keyword: scores["G"] += 1
        for kw in A_KEYWORDS:
            if kw in keyword: scores["A"] += 1

    mbti = ""
    mbti += "S" if scores["S"] >= scores["R"] else "R"
    mbti += "P" if scores["P"] >= scores["F"] else "F"
    mbti += "G" if scores["G"] >= scores["L"] else "L"
    mbti += "A" if scores["A"] >= scores["W"] else "W"

    return mbti, scores

def analyze_user_mbti(user_no):
    user_info = fetch_user_info(user_no)
    gender, birth_date = user_info if user_info else (None, None)

    age = calculate_age(birth_date) if birth_date else None
    age_group = get_age_group(age) if age else None

    logs = fetch_user_logs(user_no)

    if not logs:
        if age_group:
            mbti = AGE_DEFAULT_MBTI.get(age_group, "SFLW")
            print(f"[fallback] 로그 없음 → 나이대 기본값: {mbti}")
        else:
            mbti = "SFLW"
            print(f"[fallback] 정보 없음 → 기본값: {mbti}")
        return mbti, age, gender, {}

    mbti, scores = calculate_mbti_score(logs)
    print(f"[MBTI] 점수: {scores} → {mbti}")

    return mbti, age, gender, scores

def update_user_mbti(user_no, mbti):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        UPDATE users
        SET users_mbti = :mbti,
            updated_dt = SYSDATE
        WHERE user_no = :user_no
    """, {"mbti": mbti, "user_no": user_no})
    conn.commit()
    cursor.close()
    conn.close()

def get_tendency_from_mbti(mbti):
    s_or_r = mbti[0]
    p_or_f = mbti[1]
    g_or_l = mbti[2]

    if s_or_r == "S" and p_or_f == "P":
        return "목적외화형"
    elif s_or_r == "S" and p_or_f == "F":
        return "편의환전형"
    elif s_or_r == "R" and g_or_l == "G":
        return "환율수익형"
    elif s_or_r == "R" and g_or_l == "L":
        return "고금리외화형"
    else:
        return "목적외화형"

def recommend_products(user_no, mbti):
    tendency = get_tendency_from_mbti(mbti)

    logs = fetch_user_logs(user_no)
    search_keywords = " ".join([log[1] for log in logs if log[1]])
    query_text = f"{tendency} {search_keywords}".strip()

    results = search_similar(query_text, n_results=20)

    if not results or not results["ids"][0]:
        return [], []

    metadatas = results["metadatas"][0]
    documents = results["documents"][0]

    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("""
        SELECT product_no, product_name, product_tendency, sentiment_score
        FROM products
        WHERE active = 'Y'
        AND (sentiment_score IS NULL OR sentiment_score >= 0)
    """)
    product_map = {
        str(row[0]): {
            "product_no": row[0],
            "product_name": row[1],
            "product_tendency": row[2],
            "sentiment_score": row[3] or 0
        }
        for row in cursor.fetchall()
    }

    main_recommendations = []
    sub_recommendations = []
    seen_products = set()

    for meta, doc in zip(metadatas, documents):
        product_no = meta.get("product_no")
        if not product_no or product_no in seen_products:
            continue

        product = product_map.get(product_no)
        if not product:
            continue

        seen_products.add(product_no)

        item = {
            "product_no": product["product_no"],
            "product_name": product["product_name"],
            "product_tendency": product["product_tendency"],
            "sentiment_score": product["sentiment_score"],
            "review_text": doc,
            "sentiment": meta.get("sentiment", ""),
            "tendency": meta.get("tendency", "")
        }

        if meta.get("tendency") == tendency and len(main_recommendations) < 2:
            main_recommendations.append(item)
        elif len(sub_recommendations) < 3:
            sub_recommendations.append(item)

        if len(main_recommendations) >= 2 and len(sub_recommendations) >= 3:
            break

    # 메인 추천 없으면 유사도 높은 순으로 서브에서 채우기
    if not main_recommendations:
        main_recommendations = sub_recommendations[:2]
        sub_recommendations = sub_recommendations[2:]

    # 그래도 부족하면 감성 점수 높은 순으로 채우기
    if len(main_recommendations) < 2:
        fallback = sorted(product_map.values(), key=lambda x: x["sentiment_score"], reverse=True)
        for p in fallback:
            if str(p["product_no"]) not in seen_products and len(main_recommendations) < 2:
                main_recommendations.append({
                    "product_no": p["product_no"],
                    "product_name": p["product_name"],
                    "product_tendency": p["product_tendency"],
                    "sentiment_score": p["sentiment_score"],
                    "review_text": "",
                    "sentiment": "",
                    "tendency": ""
                })

    cursor.close()
    conn.close()
    return main_recommendations, sub_recommendations

def run_user_analysis(user_no):
    print(f"[사용자 분석] user_no: {user_no} 시작")

    mbti, age, gender, scores = analyze_user_mbti(user_no)
    update_user_mbti(user_no, mbti)

    description = MBTI_DESCRIPTIONS.get(mbti, "나만의 금융 스타일을 가진 타입이에요 😊")
    tendency = get_tendency_from_mbti(mbti)

    print(f"[사용자 분석] 나이: {age}세, MBTI: {mbti}, 성향: {tendency}")

    main_recs, sub_recs = recommend_products(user_no, mbti)
    print(f"[사용자 분석] 메인 추천 {len(main_recs)}개, 보조 추천 {len(sub_recs)}개")

    return {
        "status": "ok",
        "user_no": user_no,
        "age": age,
        "gender": gender,
        "mbti": mbti,
        "mbti_description": description,
        "mbti_scores": scores,
        "tendency": tendency,
        "main_recommendations": main_recs,
        "sub_recommendations": sub_recs
    }