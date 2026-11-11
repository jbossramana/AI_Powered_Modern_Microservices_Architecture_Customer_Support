The initial demo knowledge articles are embedded in KnowledgeBase.java for easy startup.
For a real RAG pipeline, load approved policy/FAQ documents, chunk them, embed chunks with an embedding model,
store them in a persistent vector database, retrieve by similarity, apply tenant/access filters, and pass only
retrieved authorized context to the model. Do not treat this keyword-based starter retriever as semantic vector search.
