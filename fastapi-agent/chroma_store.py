import chromadb
from openai import OpenAI
import os

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

chroma_client = chromadb.PersistentClient(path="./chroma_db")

collection = chroma_client.get_or_create_collection(
    name="reviews",
    metadata={"hnsw:space": "cosine"}
)

def get_embedding(text):
    response = client.embeddings.create(
        model="text-embedding-3-small",
        input=text
    )
    return response.data[0].embedding

def add_review(review_no, product_no, review_text, sentiment, keywords, tendency, embedding):
    collection.upsert(
        ids=[str(review_no)],
        embeddings=[embedding],
        documents=[review_text],
        metadatas=[{
            "product_no": str(product_no),
            "sentiment": sentiment,
            "keywords": ", ".join(keywords),
            "tendency": tendency
        }]
    )

def search_similar(query_text, n_results=5, filter_product_no=None):
    query_embedding = get_embedding(query_text)
    where = {"product_no": str(filter_product_no)} if filter_product_no else None
    results = collection.query(
        query_embeddings=[query_embedding],
        n_results=n_results,
        where=where
    )
    return results

