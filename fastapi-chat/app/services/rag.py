from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.config import OPENAI_API_KEY, CHAT_MODEL
from app.services.embedding import get_vectorstore

PROMPT = ChatPromptTemplate.from_template(
    """아래 문서 내용을 참고해서 질문에 답해줘.
문서에 없는 내용이면 모른다고 답해줘.

[문서]
{context}

[질문]
{question}
"""
)

def answer_question(question: str) -> str:
    vectorstore = get_vectorstore()
    retriever = vectorstore.as_retriever(search_kwargs={"k": 3})

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