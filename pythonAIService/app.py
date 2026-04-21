from fastapi import FastAPI
from pydantic import BaseModel
import numpy as np
import joblib

app = FastAPI(title="Fraud Guard API")

clf           = joblib.load("clf_model.pkl")
scaler        = joblib.load("scaler.pkl")
features      = joblib.load("features.pkl")
best_threshold = joblib.load("threshold.pkl")
le_category   = joblib.load("le_category.pkl")
le_gender     = joblib.load("le_gender.pkl")

class Transaction(BaseModel):
    amt:                    float
    city_pop:               int   = 0
    age:                    int   = 0
    hour:                   int   = 0
    lat:                    float = 0.0
    long_:                  float = 0.0
    merch_lat:              float = 0.0
    merch_long:             float = 0.0
    category:               str   = "misc_net"
    gender:                 str   = "M"
    
    time_since_last_txn:    float = 999999.0  
    txn_count_per_card_day: int   = 1
    amt_vs_card_mean:       float = 0.0
    amt_vs_card_std:        float = 1.0
    geo_distance:           float = 0.0

@app.post("/anomaly")
def detect(txn: Transaction):
    try:
        cat_enc = le_category.transform([txn.category])[0]
    except ValueError:
        cat_enc = 0

    gender_enc = le_gender.transform([txn.gender])[0]

    
    data = [[
        txn.amt, txn.city_pop, txn.age, txn.hour,
        txn.lat, txn.long_, txn.merch_lat, txn.merch_long,
        cat_enc, gender_enc,
        txn.time_since_last_txn, txn.txn_count_per_card_day,
        txn.amt_vs_card_mean, txn.amt_vs_card_std, txn.geo_distance
    ]]

    scaled = scaler.transform(data)
    prob   = float(clf.predict_proba(scaled)[0][1])

    return {
        "fraud":             int(prob >= best_threshold),
        "fraud_probability": round(prob, 4),
        "risk_level":        "HIGH" if prob >= best_threshold else "LOW"
    }

@app.get("/health")
def health():
    return {
        "status":            "ok",
        "features_expected": len(features),
        "threshold":         round(float(best_threshold), 4),
    }