"""
Trains a ticket-priority classifier on tickets_training_data.csv and reports real,
unmassaged evaluation numbers - no cherry-picked test set, no tuning against the test
data itself. Run generate_training_data.py first.

Why a RandomForestClassifier and not something fancier: it handles the mix of sparse
TF-IDF text features and dense numeric features without needing feature scaling, gives
directly interpretable feature importances (useful for explaining a prediction to a
support engineer later), and is genuinely a reasonable first real model to reach for on
a small-to-medium tabular+text problem like this one - not a compromise, an actual
sound default choice.
"""

import joblib
import pandas as pd
from sklearn.compose import ColumnTransformer
from sklearn.ensemble import RandomForestClassifier
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import OneHotEncoder

PRIORITY_ORDER = ["LOW", "MEDIUM", "HIGH", "URGENT"]


def load_data() -> pd.DataFrame:
    df = pd.read_csv("tickets_training_data.csv")
    df["ticket_text"] = df["subject"].fillna("") + " " + df["body"].fillna("")
    return df


def build_pipeline() -> Pipeline:
    # ColumnTransformer runs a different transformer per column/column-group, then
    # concatenates the results into one feature matrix the classifier trains on - the
    # standard scikit-learn way to mix text and structured features in one model.
    preprocessor = ColumnTransformer(transformers=[
        ("text", TfidfVectorizer(max_features=600, ngram_range=(1, 2), stop_words="english", min_df=2), "ticket_text"),
        ("plan", OneHotEncoder(handle_unknown="ignore"), ["plan_tier"]),
        ("numeric", "passthrough", ["account_age_days", "prior_ticket_count_30d"]),
    ])

    return Pipeline([
        ("preprocess", preprocessor),
        ("classifier", RandomForestClassifier(
            n_estimators=300,
            max_depth=14,
            min_samples_leaf=3,
            class_weight="balanced",
            random_state=42,
            n_jobs=-1,
        )),
    ])


def main():
    df = load_data()
    feature_cols = ["ticket_text", "plan_tier", "account_age_days", "prior_ticket_count_30d"]
    X = df[feature_cols]
    y = df["priority"]

    # Stratified split: keeps the same class balance in train and test, so the
    # reported accuracy isn't accidentally flattered or hurt by an unlucky split.
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    pipeline = build_pipeline()
    pipeline.fit(X_train, y_train)

    y_pred = pipeline.predict(X_test)

    print(f"Train rows: {len(X_train)}   Test rows: {len(X_test)}")
    print(f"\nAccuracy on held-out test data: {accuracy_score(y_test, y_pred):.3f}")
    print("\nPer-class precision/recall/F1:")
    print(classification_report(y_test, y_pred, labels=PRIORITY_ORDER))
    print("Confusion matrix (rows = actual, columns = predicted), order LOW/MEDIUM/HIGH/URGENT:")
    print(confusion_matrix(y_test, y_pred, labels=PRIORITY_ORDER))

    # Feature importance: which signals the model actually leaned on. Cheap sanity
    # check that it learned something sensible rather than a spurious correlation -
    # if "account_age_days" dominated over the tone words, that would be a red flag.
    feature_names = pipeline.named_steps["preprocess"].get_feature_names_out()
    importances = pipeline.named_steps["classifier"].feature_importances_
    top_features = sorted(zip(feature_names, importances), key=lambda x: -x[1])[:15]
    print("\nTop 15 most influential features:")
    for name, importance in top_features:
        print(f"  {importance:.4f}  {name}")

    joblib.dump(pipeline, "priority_model.joblib")
    print("\nSaved trained pipeline -> priority_model.joblib")


if __name__ == "__main__":
    main()
