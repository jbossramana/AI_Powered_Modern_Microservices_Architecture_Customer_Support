"""
Serves the trained priority-classification pipeline over HTTP. This is the piece a
Java service actually talks to - see ../java-integration for the Spring Boot side.

Run with: uvicorn serve:app --host 0.0.0.0 --port 8000
"""

from contextlib import asynccontextmanager

import joblib
import pandas as pd
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

MODEL_PATH = "priority_model.joblib"
model = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global model
    try:
        model = joblib.load(MODEL_PATH)
    except FileNotFoundError:
        model = None  # /health reports this clearly instead of the app failing to boot
    yield


app = FastAPI(title="Ticket Priority Classifier", version="1.0.0", lifespan=lifespan)


class TicketFeatures(BaseModel):
    subject: str = Field(..., min_length=1)
    body: str = Field(..., min_length=1)
    plan_tier: str = Field(..., pattern="^(FREE|PRO|BUSINESS)$")
    account_age_days: int = Field(..., ge=0)
    prior_ticket_count_30d: int = Field(..., ge=0)


class PredictionResponse(BaseModel):
    priority: str
    confidence: float
    class_probabilities: dict[str, float]


@app.get("/health")
def health():
    return {"status": "ok" if model is not None else "model_not_loaded"}


@app.post("/predict", response_model=PredictionResponse)
def predict(ticket: TicketFeatures) -> PredictionResponse:
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded - run train_model.py first")

    row = pd.DataFrame([{
        "ticket_text": f"{ticket.subject} {ticket.body}",
        "plan_tier": ticket.plan_tier,
        "account_age_days": ticket.account_age_days,
        "prior_ticket_count_30d": ticket.prior_ticket_count_30d,
    }])

    probabilities = model.predict_proba(row)[0]
    classes = model.classes_
    class_probabilities = {cls: round(float(p), 4) for cls, p in zip(classes, probabilities)}
    predicted_index = probabilities.argmax()

    return PredictionResponse(
        priority=classes[predicted_index],
        confidence=round(float(probabilities[predicted_index]), 4),
        class_probabilities=class_probabilities,
    )
