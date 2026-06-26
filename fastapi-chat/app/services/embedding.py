from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma
from app.config import OPENAI_API_KEY, CHROMA_DIR, EMBED_MODEL
from app.services.pdf_loader import load_and_split

def get_embeddings():
    return OpenAIEmbeddings(
        model=EMBED_MODEL,
        openai_api_key=OPENAI_API_KEY,
    )

def ingest_pdf(pdf_path: str):
    """PDF를 벡터DB에 저장 (최초 1회 실행)"""
    chunks = load_and_split(pdf_path)
    Chroma.from_documents(
        documents=chunks,
        embedding=get_embeddings(),
        persist_directory=CHROMA_DIR,
    )
    return len(chunks)

def get_vectorstore():
    """저장된 벡터DB 불러오기"""
    return Chroma(
        persist_directory=CHROMA_DIR,
        embedding_function=get_embeddings(),
    )