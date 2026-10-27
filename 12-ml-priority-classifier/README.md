# Ticket Priority Classifier — a Traditional ML Model, Not an LLM

Every other module in this series uses generative AI: a language model reads a prompt
and produces text. This one is a genuinely different tool for a genuinely different
job: a **trained classifier** (`RandomForestClassifier`, from scikit-learn) that has
seen thousands of labeled examples and predicts a ticket's priority from its text plus
account signals — no prompt, no generation, no hallucination risk, because it isn't
generating language at all.

## The problem this solves that an LLM doesn't (or shouldn't)

`02-agent-spring-ai` already classifies priority with an LLM, so why build this at all?
Three real reasons a team would reach for classical ML alongside their LLM features,
not instead of them:

1. **Cost and latency at volume.** A RandomForest prediction is a handful of matrix
   operations - typically single-digit milliseconds, no network call to a model
   provider. Classifying a million historical tickets to backfill a dashboard with an
   LLM is slow and expensive; with this model it's a for-loop.
2. **Determinism.** The same input always produces the same output. That matters when
   "why was this ticket marked URGENT" needs a reproducible answer - a support team
   auditing decisions can't get two different answers from asking twice.
3. **You already have the labels.** If a company has years of tickets with priority
   already assigned (by past triage, by SLA outcomes, by whatever), that's exactly the
   supervised-learning setup classical ML wants - and asking an LLM to "learn" from
   millions of historical examples via a prompt isn't really how LLMs work anyway.

The tradeoff going the other way: this model can only ever predict priority. It can't
explain its reasoning in English, can't answer a follow-up question, can't handle a
category it never saw in training, and needs a real labeled dataset to exist at all -
none of which are problems for the LLM-based approach. Neither tool is strictly better;
they solve different halves of "classify this ticket."

## The real, measured results

There's no historical ticket dataset available in this environment, so
`generate_training_data.py` builds a synthetic one - 2,200 tickets with genuine,
learnable signal (urgency language, plan tier, how many times this customer has
contacted support in the last 30 days) plus **deliberate 12% label noise**, so the
model has something real to learn without the suspiciously perfect accuracy a
noise-free synthetic dataset would produce.

`train_model.py` was actually run, not just written. On a held-out 440-ticket test set
it never saw during training:

```
Accuracy: 0.902

              precision    recall  f1-score   support
         LOW       0.92      0.93      0.92       108
      MEDIUM       0.87      0.87      0.87       103
        HIGH       0.90      0.87      0.89       116
      URGENT       0.92      0.94      0.93       113
```

The confusion matrix is the more interesting number than accuracy alone: **every
single misclassification was between adjacent priority levels.** LOW was never once
predicted as HIGH or URGENT; URGENT was never once predicted as LOW or MEDIUM. When
this model is wrong, it's wrong by one step on the scale, never by three - which is a
meaningfully different (and much safer) failure mode than being wrong randomly.

Feature importance confirms the model learned real signal, not a spurious shortcut:
the top-ranked features were words like *"completely"* and *"rush"* (straight out of
the URGENT and LOW tone phrases) and `prior_ticket_count_30d` ranked #3 overall - the
tabular repeat-contact signal pulling real weight alongside the text, which is the
whole point of mixing feature types in one model.

## Prerequisites

```bash
cd model
pip install -r requirements.txt
```

## Run it

```bash
cd model
python3 generate_training_data.py   # writes tickets_training_data.csv
python3 train_model.py              # trains, evaluates, writes priority_model.joblib
uvicorn serve:app --host 0.0.0.0 --port 8000
```

## Try it — three real requests, with the real responses they produced

curl -X POST http://localhost:8000/predict -H "Content-Type: application/json" -d "{\"subject\":\"System down\",\"body\":\"This is completely unacceptable, losing money every minute.\",\"plan_tier\":\"BUSINESS\",\"account_age_days\":400,\"prior_ticket_count_30d\":4}"

{"priority":"URGENT","confidence":0.9081,"class_probabilities":{"HIGH":0.0747,"LOW":0.0118,"MEDIUM":0.0053,"URGENT":0.9081}}


**Clearly calm** (Free tier, first-time contact):
curl -X POST http://localhost:8000/predict -H "Content-Type: application/json" -d "{\"subject\":\"Quick question\",\"body\":\"No rush at all, just curious how the sharing permissions work when you get a chance.\",\"plan_tier\":\"FREE\",\"account_age_days\":30,\"prior_ticket_count_30d\":0}"


{"priority":"LOW","confidence":0.7524,"class_probabilities":{"HIGH":0.0703,"LOW":0.7524,"MEDIUM":0.1105,"URGENT":0.0668}}


**Genuinely middle-of-the-road** ("running into an issue, would appreciate help"):
```bash
curl -s -X POST http://localhost:8000/predict -H "Content-Type: application/json" -d '{
  "subject": "Login issue",
  "body": "Running into an issue here and would appreciate some help when you can.",
  "plan_tier": "PRO", "account_age_days": 200, "prior_ticket_count_30d": 1
}'
```
```json
{"priority":"MEDIUM","confidence":0.9054,"class_probabilities":{"HIGH":0.0476,"LOW":0.0355,"MEDIUM":0.9054,"URGENT":0.0115}}
```

Notice the model isn't just picking a label - the confidence and the *shape* of the
probability distribution both track how genuinely unambiguous each example is. Send
`plan_tier: "ENTERPRISE"` (not one of FREE/PRO/BUSINESS) and you'll get a `422` -
Pydantic validating the request shape before it ever reaches the model.

## The Java side — same port pattern as the LLM adapters

```bash
cd java-integration
mvn spring-boot:run
```
curl -X POST http://localhost:8000/predict -H "Content-Type: application/json" -d "{\"subject\":\"Quick question\",\"body\":\"No rush at all, just curious how the sharing permissions work when you get a chance.\",\"plan_tier\":\"FREE\",\"account_age_days\":30,\"prior_ticket_count_30d\":0}"


If you've looked at `clean-architecture-spring-ai`, `PriorityPredictionPort` and
`MlModelPriorityAdapter` here will look immediately familiar - it's the exact same
shape as `TicketClassifierPort` and its two LLM-backed adapters
(`SpringAiTicketClassifierAdapter`, `LangChain4jTicketClassifierAdapter`). That's the
real payoff of designing ports around a *capability* ("predict this ticket's
priority") instead of around a *technology* ("call an LLM"): a completely different
kind of AI - not just a different framework, a different paradigm entirely - slots in
behind the same interface with zero changes to whatever code calls it. `RestClient`,
`@JsonProperty` for the snake_case↔camelCase bridge, and the overall adapter shape are
otherwise identical to patterns used everywhere else in this series.

## Honest limitations

- **Synthetic data.** The 90.2% accuracy is real and was actually measured, but it's
  90.2% on a synthetic dataset whose patterns this same script generated. A production
  version needs real historical tickets - expect real-world accuracy to differ, likely
  lower, since real language is messier than a phrase bank.
- **No retraining pipeline.** `priority_model.joblib` is trained once and frozen. A
  real deployment needs a schedule (or trigger) to retrain as new labeled tickets
  arrive, plus a way to compare a new model against the currently-deployed one before
  swapping it in.
- **No drift monitoring.** If real ticket language shifts over time (new product
  features, new complaint patterns), this model's accuracy silently degrades with no
  built-in way to notice - unlike a prompt, which a human can just read and update.
- **One model, one label.** Real systems doing this in production often run several
  models (one per prediction task) or a multi-output model, with its own versioning
  and rollback story per model - out of scope for one teaching example.
