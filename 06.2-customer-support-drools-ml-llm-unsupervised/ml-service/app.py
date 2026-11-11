from __future__ import annotations

from typing import List

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel, Field
from sklearn.cluster import KMeans
from sklearn.ensemble import IsolationForest
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

app = FastAPI(
    title="Customer Support ML Service",
    description="Supervised ticket classification plus unsupervised clustering and anomaly detection.",
    version="2.0.0",
)

# ---------------------------------------------------------------------------
# 1) SUPERVISED LEARNING: learn ticket categories from labeled examples.
# ---------------------------------------------------------------------------
training_texts = [
    "production server is down", "all users cannot log in",
    "system outage after deployment", "website unavailable for every customer",
    "forgot my password", "cannot reset my password",
    "login credentials are not working", "account locked cannot sign in",
    "please explain my invoice", "billing amount is incorrect",
    "payment was charged twice", "refund for duplicate payment",
]
training_labels = [
    "PRODUCTION_INCIDENT", "PRODUCTION_INCIDENT", "PRODUCTION_INCIDENT", "PRODUCTION_INCIDENT",
    "ACCOUNT_ACCESS", "ACCOUNT_ACCESS", "ACCOUNT_ACCESS", "ACCOUNT_ACCESS",
    "BILLING", "BILLING", "BILLING", "BILLING",
]
supervised_model = Pipeline([
    ("tfidf", TfidfVectorizer(ngram_range=(1, 2))),
    ("classifier", LogisticRegression(max_iter=1000, random_state=42)),
])
supervised_model.fit(training_texts, training_labels)

# ---------------------------------------------------------------------------
# 2) UNSUPERVISED LEARNING: cluster ticket text without category labels.
# KMeans discovers groups from the text patterns; names are not supplied as
# training labels. Cluster IDs may change when the training corpus changes.
# ---------------------------------------------------------------------------
# Unlabeled examples used only to demonstrate discovery of recurring themes.
# These examples have no target category labels.
cluster_training_texts = training_texts + [
    "Kubernetes pod keeps crashing after deployment",
    "container restart loop and application logs show errors",
    "deployment failed and service is unavailable",
    "database connection timeout and query failures",
    "database server is slow and connections are exhausted",
    "SQL query latency is increasing",
    "payment API timeout during checkout",
    "duplicate payment transaction and refund request",
    "invoice amount is incorrect and billing dispute",
    "password reset email never arrives",
    "account locked after several login attempts",
    "single sign on authentication failure",
    "high CPU usage and memory pressure on production server",
    "request latency spike and unusual traffic volume",
    "service errors increased after release",
]
cluster_vectorizer = TfidfVectorizer(ngram_range=(1, 2), stop_words="english")
cluster_matrix = cluster_vectorizer.fit_transform(cluster_training_texts)
cluster_model = KMeans(n_clusters=4, random_state=42, n_init=10)
cluster_model.fit(cluster_matrix)
cluster_terms = cluster_vectorizer.get_feature_names_out()

# ---------------------------------------------------------------------------
# 3) UNSUPERVISED ANOMALY DETECTION: learn a baseline from synthetic,
# normal-looking telemetry. Real systems should fit this to their own
# verified normal historical metrics.
# ---------------------------------------------------------------------------
rng = np.random.default_rng(42)
normal_metrics = np.column_stack([
    np.clip(rng.normal(35, 12, 1200), 1, 75),       # CPU %
    np.clip(rng.normal(45, 12, 1200), 5, 80),       # memory %
    np.clip(rng.lognormal(np.log(1.2), 0.35, 1200), 0.1, 3.0),  # error rate %
    np.clip(rng.lognormal(np.log(180), 0.35, 1200), 30, 650),   # latency ms
])
anomaly_model = Pipeline([
    ("scale", StandardScaler()),
    ("detector", IsolationForest(
        n_estimators=200, contamination=0.05, random_state=42
    )),
])
anomaly_model.fit(normal_metrics)


class Ticket(BaseModel):
    text: str = Field(min_length=1, max_length=10000)


class Telemetry(BaseModel):
    cpu_percent: float = Field(ge=0, le=100)
    memory_percent: float = Field(ge=0, le=100)
    error_rate_percent: float = Field(ge=0, le=100)
    latency_ms: float = Field(ge=0, le=600000)


@app.get("/health")
def health():
    return {
        "status": "UP",
        "supervised_model": "TF-IDF + LogisticRegression",
        "unsupervised_models": ["TF-IDF + KMeans ticket clustering", "IsolationForest telemetry anomaly detection"],
    }


@app.post("/predict")
def predict(ticket: Ticket):
    """Supervised classification: predicts a category learned from labeled examples."""
    probabilities = supervised_model.predict_proba([ticket.text])[0]
    index = int(probabilities.argmax())
    category = str(supervised_model.classes_[index])
    confidence = float(probabilities[index])
    # Demonstration mapping only; not a validated production priority policy.
    priority = {
        "PRODUCTION_INCIDENT": "CRITICAL",
        "ACCOUNT_ACCESS": "MEDIUM",
        "BILLING": "LOW",
    }[category]
    return {
        "learning_type": "supervised",
        "category": category,
        "priority": priority,
        "confidence": round(confidence, 4),
    }


@app.post("/unsupervised/cluster")
def cluster_ticket(ticket: Ticket):
    """Unsupervised clustering: groups ticket text by similarity without labels."""
    vector = cluster_vectorizer.transform([ticket.text])
    cluster_id = int(cluster_model.predict(vector)[0])
    center = cluster_model.cluster_centers_[cluster_id]
    top_indices = center.argsort()[::-1][:5]
    terms = [str(cluster_terms[i]) for i in top_indices if center[i] > 0]
    return {
        "learning_type": "unsupervised",
        "method": "KMeans",
        "cluster_id": cluster_id,
        "top_terms": terms,
        "explanation": "The ticket was grouped with similar text patterns; the cluster ID is not a predefined business category.",
    }


@app.post("/unsupervised/anomaly")
def detect_anomaly(metrics: Telemetry):
    """Unsupervised anomaly detection: flags telemetry unlike the learned baseline."""
    values = [[
        metrics.cpu_percent,
        metrics.memory_percent,
        metrics.error_rate_percent,
        metrics.latency_ms,
    ]]
    prediction = int(anomaly_model.predict(values)[0])
    decision_score = float(anomaly_model.decision_function(values)[0])
    return {
        "learning_type": "unsupervised",
        "method": "IsolationForest",
        "is_anomaly": prediction == -1,
        "status": "ANOMALY" if prediction == -1 else "NORMAL",
        "anomaly_score": round(-decision_score, 4),
        "input_metrics": metrics.model_dump(),
        "note": "Demo baseline uses synthetic telemetry. Train and validate on your own normal historical data before operational use.",
    }
