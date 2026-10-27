from fastapi import FastAPI
from pydantic import BaseModel, Field
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline

app = FastAPI(title="Customer Support ML Classifier", version="1.0.0")
texts = [
    "production server is down", "all users cannot log in", "system outage after deployment",
    "website unavailable for every customer", "forgot my password", "cannot reset my password",
    "login credentials are not working", "account locked cannot sign in", "please explain my invoice",
    "billing amount is incorrect", "payment was charged twice", "refund for duplicate payment"
]
labels = [
    "PRODUCTION_INCIDENT", "PRODUCTION_INCIDENT", "PRODUCTION_INCIDENT", "PRODUCTION_INCIDENT",
    "ACCOUNT_ACCESS", "ACCOUNT_ACCESS", "ACCOUNT_ACCESS", "ACCOUNT_ACCESS",
    "BILLING", "BILLING", "BILLING", "BILLING"
]
model = Pipeline([("tfidf", TfidfVectorizer(ngram_range=(1, 2))),
                  ("classifier", LogisticRegression(max_iter=1000, random_state=42))])
model.fit(texts, labels)

class Ticket(BaseModel):
    text: str = Field(min_length=1, max_length=10000)

@app.get("/health")
def health():
    return {"status": "UP", "model": "TF-IDF + LogisticRegression"}

@app.post("/predict")
def predict(ticket: Ticket):
    probs = model.predict_proba([ticket.text])[0]
    idx = int(probs.argmax())
    category = str(model.classes_[idx])
    confidence = float(probs[idx])
    # Simplified demo mapping; not a validated production priority policy.
    priority = {"PRODUCTION_INCIDENT": "CRITICAL", "ACCOUNT_ACCESS": "MEDIUM", "BILLING": "LOW"}[category]
    return {"category": category, "priority": priority, "confidence": round(confidence, 4)}
