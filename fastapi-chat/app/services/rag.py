from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.config import OPENAI_API_KEY, CHAT_MODEL
from app.services.embedding import get_vectorstore

PROMPT = ChatPromptTemplate.from_template(
    """너는 부산은행(BNK)의 친절한 외환 상담 도우미야.
아래 [참고자료]를 바탕으로 고객 질문에 답하되, 다음 규칙을 꼭 지켜:

- "문서에 따르면", "자료에 의하면" 같은 표현은 절대 쓰지 마.
- 실제 은행 상담원처럼 자연스럽고 친근하게, 바로 본론으로 답해.
- 너무 길게 늘어놓지 말고 핵심을 간결하게 설명해.
- 참고자료에 없는 내용이면 모르는 척 지어내지 말고, "정확한 내용은 가까운 영업점이나 고객센터(1588-6200)로 문의해 주세요"라고 안내해.

[참고자료]
{context}

[고객 질문]
{question}
"""
)

def answer_question(question: str) -> str:
    vectorstore = get_vectorstore()
    retriever = vectorstore.as_retriever(search_kwargs={"k": 5})

    # 관련 문서 검색
    docs = retriever.invoke(question)
    context = "\n\n".join(doc.page_content for doc in docs)

    # LLM 호출
    llm = ChatOpenAI(
        model=CHAT_MODEL,
        temperature=0,
        openai_api_key=OPENAI_API_KEY,
    )
    chain = PROMPT | llm
    response = chain.invoke({"context": context, "question": question})
    return response.content