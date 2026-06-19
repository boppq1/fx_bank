chroma_client = chromadb.PersistentClient(path="./chroma_db")

# 데이터 저장소
collection = chroma_client.get_or_create_collection(
    name="reviews",
    metadata={"hnsw:space" : "cosine"}
)

def add_review(review_no, product_no, review_text, sentiment, keywords, tendency, embedding) :
    collection.upsert(
        ids=[str(review_no)],
        embedding=[embedding],
        documents=[review_text],
        metadatas=[{
            "product_no" : str(product_no),
            "sentiment" : sentiment,
            "keywords" : ", ".join(keywords),
            "tendency" : tendency
        }]
    )

def search_similar(query_text, n_results=5, filter_product_no=None) :
    query_embedding = get_embedding(query_text)
    where = {"product_no": str(filter_product_no)} if filter_product_no else None

    results = collection.query(
        query_embedding=[query_embedding],
        n_results=n_results,
        where = where
    )

    return results
