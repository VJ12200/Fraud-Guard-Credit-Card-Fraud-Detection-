# FraudGuard — Real-Time Credit Card Fraud Detection

> A production-style, two-layer fraud detection system combining a **Java rule engine** with an **XGBoost ML model** trained on 1.85M transactions, featuring AML compliance rules, graph-based money laundering detection, and a real-time REST API.

![Java](https://img.shields.io/badge/Java-17+-orange?style=flat-square&logo=openjdk)
![SpringBoot](https://img.shields.io/badge/SpringBoot-3.x-brightgreen?style=flat-square&logo=springboot)
![Python](https://img.shields.io/badge/Python-3.9+-blue?style=flat-square&logo=python)
![FastAPI](https://img.shields.io/badge/FastAPI-Enabled-009688?style=flat-square&logo=fastapi)
![XGBoost](https://img.shields.io/badge/XGBoost-ML-red?style=flat-square)
![Optuna](https://img.shields.io/badge/Optuna-Tuned-purple?style=flat-square)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-UI-005F0F?style=flat-square)


---

## Table of Contents

- [Overview](#overview)
- [Results](#results)
- [Architecture](#architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [ML Pipeline](#ml-pipeline)
- [Rule Engine](#rule-engine)
- [Risk Scoring](#risk-scoring)
- [UI](#ui)
- [Test Cases](#test-cases)
- [Acknowledgements](#acknowledgements)

---

## Overview

FraudGuard is a full-stack fraud detection system that processes credit card transactions through two independent layers:

1. **Rule Engine (Java)** — deterministic AML compliance rules that fire instantly with no ML dependency
2. **ML Model (Python)** — XGBoost classifier trained on 1.85M synthetic transactions with 15 engineered features, served via FastAPI

Both layers run on every transaction. If the Python service is unavailable, the rule engine continues operating independently — the system never goes fully down.

Transactions can be submitted as a **single object or a JSON array** through the same endpoint, and are evaluated for high-amount violations, structuring, frequency bursts, graph-based money laundering cycles, and ML-detected anomalies simultaneously.

---

## Results

| Metric | Value |
|---|---|
| **PR-AUC** | **0.967** |
| **F1 Score** (threshold=0.686) | **0.922** |
| **Recall** | **98.7%** — 1,482 / 1,501 fraud cases caught |
| **Precision** | 94.9% at best-F1 threshold |
| False Alarms | 72 on 257,834 legitimate transactions |
| Training samples | 1,852,394 transactions |
| Fraud rate | 0.58% (class imbalance handled with SMOTE) |
| Baseline (random) | PR-AUC 0.0058 — model is **167x better** |

### PR Curve

![PR Curve](results/pr_curve.png)

The curve stays near 1.0 precision until ~0.95 recall before dropping — a strong model signature. The wide flat F1 plateau (threshold 0.05–0.90) indicates robustness to threshold changes in production.

### Threshold Tradeoff

| Threshold | Precision | Recall | F1 | Caught | False Alarms |
|---|---|---|---|---|---|
| 0.10 | 0.715 | 0.960 | 0.820 | 1441/1501 | 574 |
| 0.20 | 0.808 | 0.951 | 0.874 | 1427/1501 | 338 |
| 0.30 | 0.856 | 0.939 | 0.896 | 1410/1501 | 238 |
| 0.50 | 0.908 | 0.919 | 0.914 | 1380/1501 | 140 |
| **0.69** | **0.949** | **0.894** | **0.921** | **1342/1501** | **72** |
| 0.80 | 0.965 | 0.877 | 0.919 | 1317/1501 | 48 |

---

## Architecture

```
Browser / Postman
      │
      ▼
POST /transactions              Spring Boot :8081
      │
      ├─── TransactionController
      │         │  (unified endpoint — single or batch JSON)
      │         ▼
      │    TransactionService
      │         │
      │         ├── RuleEngine ──────────────────────────────────┐
      │         │     ├── HighAmountRule   (amount > 1,000,000)  │
      │         │     ├── StructuringRule  (cumulative > 1M/hr)  │  Java
      │         │     ├── FrequencyRule    (> 10 txns/hr)        │
      │         │     └── GraphCycleRule   (DFS cycle detection)  │
      │         │                                                 │
      │         ├── FeatureService  (5 behavioural features)     │
      │         │                                                 │
      │         └── AIService ──────────────────────────────────┘
      │                   │
      │                   │  HTTP POST
      │                   ▼
      │          FastAPI :8000/anomaly        Python
      │                   │
      │                   ▼
      │          XGBoost (15 features)
      │                   │
      │                   ▼
      │          { fraud, probability, risk_level }
      │
      ▼
RiskScoringService  →  score (0–100)
AlertRepository     →  persisted to DB
      │
      ▼
Response  →  "FLAGGED | Risk: 80 | 2 violation(s): High Amount Rule, AI Anomaly Detection"
```

### Graceful Degradation

```
Python service DOWN  →  AIService catches ResourceAccessException
                     →  logs warning, returns false
                     →  rule engine result returned normally
                     →  system never crashes
```

---

## Features

### Rule Engine
- **High Amount Rule** — flags any single transaction exceeding $1,000,000
- **Structuring Rule** — monitors cumulative spend per sender over a rolling 1-hour window; flags when total exceeds $1,000,000 (detects split-transaction laundering)
- **Frequency Rule** — flags senders with more than 10 transactions in the last hour
- **Graph Cycle Detection** — builds a directed transaction graph and runs DFS from every node to detect money laundering rings (A→B→C→A patterns)

### ML Model
- XGBoost classifier trained on 1.85M synthetic transactions
- Hyperparameters tuned via Optuna (50 trials, TPE sampler, maximising PR-AUC)
- SMOTE oversampling on training fold only to handle 0.58% fraud rate
- `scale_pos_weight` set to neg/pos ratio as additional imbalance correction
- Threshold tuned post-training via precision-recall curve analysis

### Feature Engineering (computed at inference, no user input required)
| Feature | Description |
|---|---|
| `hour` | Hour of day extracted from transaction timestamp |
| `time_since_last_txn` | Seconds since same card's previous transaction |
| `txn_count_per_card_day` | Number of transactions this card made today |
| `amt_vs_card_mean` | Signed deviation from this card's historical average spend |
| `amt_vs_card_std` | Z-score-like ratio against card's spend standard deviation |
| `geo_distance` | Euclidean distance between cardholder and merchant location |

### API Design
- Single endpoint accepts both `{...}` and `[{...}, {...}]` — no separate batch route
- Jackson `readTree` parsing handles type detection at runtime
- Per-transaction try/catch in batch processing — one failure never crashes others
- `@JsonIgnoreProperties(ignoreUnknown = true)` on model prevents unknown field errors

### UI
- Thymeleaf frontend with dark terminal aesthetic
- Single and batch transaction modes with toggle
- Category dropdown with all 13 dataset categories
- Auto-computed feature note shown to users
- Colour-coded results (green OK, red FLAGGED, yellow FAILED)

---

## Tech Stack

| Component | Technology |
|---|---|
| REST API | Java 17, Spring Boot 3.x |
| Persistence | Spring Data JPA, H2 (default) / PostgreSQL |
| UI | Thymeleaf, HTML/CSS/JS |
| ML Model | XGBoost |
| Preprocessing | scikit-learn, pandas, numpy |
| Class Imbalance | imbalanced-learn (SMOTE) |
| Hyperparameter Tuning | Optuna |
| Inference API | FastAPI, Pydantic, Uvicorn |
| Model Serialisation | joblib |
| Visualisation | matplotlib |

---

## Project Structure

```
fraudguard/
├── java/                                   Spring Boot application
│   ├── src/main/java/com/example/fintechcompliance/
│   │   ├── controller/
│   │   │   ├── TransactionController.java  Unified single/batch endpoint
│   │   │   └── ViewController.java         Thymeleaf form handler
│   │   ├── model/
│   │   │   ├── FinancialTransaction.java
│   │   │   └── FinancialAlert.java
│   │   ├── repository/
│   │   │   ├── TransactionRepository.java
│   │   │   └── AlertRepository.java
│   │   ├── ruleengine/
│   │   │   ├── Rule.java                   Interface
│   │   │   ├── RuleEngine.java             Spring-managed bean, autowires List<Rule>
│   │   │   ├── HighAmountRule.java
│   │   │   ├── FrequencyRule.java
│   │   │   └── StructuringRule.java
│   │   └── service/
│   │       ├── AIService.java              HTTP client to Python API
│   │       ├── CycleDetection.java         DFS graph traversal
│   │       ├── ExplanationService.java
│   │       ├── FeatureService.java         Runtime feature engineering
│   │       ├── GraphService.java           Transaction graph + TTL cache
│   │       ├── RiskScoringService.java     Weighted score aggregation
│   │       └── TransactionService.java     Orchestration
│   ├── src/main/resources/
│   │   ├── templates/UI.html
│   │   └── application.properties.example
│   └── pom.xml
│
├── python/                                 ML pipeline
│   ├── preprocess.py                       Feature engineering + encoders
│   ├── tune.py                             Optuna hyperparameter search (run once)
│   ├── train.py                            Fast retraining using saved params
│   ├── evaluate.py                         Metrics, threshold tuning, plots
│   ├── app.py                              FastAPI inference server
│   └── models/                             pkl files saved here (gitignored)
│
├── data/
│   └── README.md                           Dataset download instructions
│
├── results/
│   └── pr_curve.png                        Evaluation plots
│
├── .gitignore
├── requirements.txt
└── README.md
```

---

## Quick Start

### Prerequisites
- Java 17+
- Python 3.9+
- Maven 3.8+

### 1. Clone the repository

```bash
git clone https://github.com/yourusername/fraudguard.git
cd fraudguard
```

### 2. Download the dataset

See [data/README.md](data/README.md) for instructions.
Place `fraudTrain.csv` in the `data/` directory.

### 3. Set up Python environment

```bash
pip install -r requirements.txt
```

### 4. Train the ML model

```bash
cd python

# Run once — finds best hyperparameters via Optuna (~30–60 mins)
python tune.py

# Fast retraining using saved params (~5 mins)
python train.py

# Evaluate model and save decision threshold
python evaluate.py
```

### 5. Start the Python inference API

```bash
cd python
uvicorn app:app --reload --port 8000
```

Verify: `http://localhost:8000/health`

Expected response:
```json
{
  "status": "ok",
  "features_expected": 15,
  "threshold": 0.6856
}
```

### 6. Configure Spring Boot

```bash
cd java
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties
```

Edit `application.properties` with your database settings if not using H2.

### 7. Start Spring Boot

```bash
cd java
mvn spring-boot:run
```

### 8. Open the UI

```
http://localhost:8081/ui
```

---

## API Reference

### `POST /transactions`

Unified endpoint — accepts both a single transaction object and a JSON array.

**Single transaction:**
```bash
curl -X POST http://localhost:8081/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "senderId": "card_001",
    "receiverId": "merchant_42",
    "amount": 5000,
    "category": "shopping_net",
    "gender": "M",
    "age": 34,
    "cityPop": 50000,
    "lat": 37.77,
    "lon": -122.41,
    "merchLat": 37.80,
    "merchLong": -122.45
  }'
```

**Batch:**
```bash
curl -X POST http://localhost:8081/transactions \
  -H "Content-Type: application/json" \
  -d '[
    {"senderId":"card_001","receiverId":"merchant_A","amount":500,"category":"grocery_pos","gender":"M","age":28},
    {"senderId":"card_002","receiverId":"merchant_B","amount":1500000,"category":"misc_net","gender":"F","age":55}
  ]'
```

**Response (always a list):**
```json
[
  "Transaction OK",
  "FLAGGED | Risk: 50 | 1 violation(s) detected: High Amount Rule"
]
```

### Request Fields

| Field | Type | Required | Description |
|---|---|---|---|
| `senderId` | String | ✅ | Card or user identifier |
| `receiverId` | String | ✅ | Merchant identifier |
| `amount` | Double | ✅ | Transaction amount (must be > 0) |
| `category` | String | ❌ | Merchant category (default: `misc_net`) |
| `gender` | String | ❌ | `M` or `F` (default: `M`) |
| `age` | Integer | ❌ | Cardholder age |
| `cityPop` | Integer | ❌ | Population of cardholder's city |
| `lat` / `lon` | Double | ❌ | Cardholder coordinates |
| `merchLat` / `merchLong` | Double | ❌ | Merchant coordinates |

### Supported Categories

`grocery_pos` · `grocery_net` · `shopping_pos` · `shopping_net` · `gas_transport` · `entertainment` · `food_dining` · `health_fitness` · `travel` · `home` · `kids_pets` · `personal_care` · `misc_net`

### `GET /transactions/{id}/alerts`

Returns all alerts generated for a specific transaction.

```json
[
  {
    "id": 1,
    "transactionId": 42,
    "ruleViolated": "High Amount Rule",
    "riskScore": 50.0,
    "explanation": "1 violation(s) detected: High Amount Rule"
  }
]
```

### `GET /health` (Python API)

```json
{
  "status": "ok",
  "features_expected": 15,
  "threshold": 0.6856
}
```

---

## ML Pipeline

### Files

| File | Purpose | When to run |
|---|---|---|
| `preprocess.py` | Feature engineering, label encoding | Called by other scripts |
| `tune.py` | Optuna hyperparameter search | **Once only** — saves `best_params.pkl` |
| `train.py` | Fast retraining with saved params | Any time you retrain |
| `evaluate.py` | Metrics, threshold selection, plots | After every training run |
| `app.py` | FastAPI inference server | Keep running alongside Spring Boot |

### When to re-run `tune.py`

| Situation | Re-tune? |
|---|---|
| Routine retraining on same data | ❌ Use `train.py` |
| Added new features | ✅ Yes |
| Switched dataset | ✅ Yes |
| PR-AUC dropped significantly | ✅ Yes |

### Feature Vector (sent from Java → Python)

```
amt, city_pop, age, hour, lat, long, merch_lat, merch_long,
category_enc, gender_enc, time_since_last_txn,
txn_count_per_card_day, amt_vs_card_mean, amt_vs_card_std, geo_distance
```

All behavioural features (`time_since_last_txn`, `txn_count_per_card_day`, `amt_vs_card_mean`, `amt_vs_card_std`, `geo_distance`) are computed at runtime by `FeatureService.java` from live DB queries — no user input needed.

### Top 10 Feature Importances (LightGBM reference run)

| Feature | Importance |
|---|---|
| `category_enc` | 2073 |
| `hour` | 1824 |
| `amt` | 1717 |
| `age` | 1692 |
| `txn_count_per_card_day` | 1448 |
| `amt_vs_card_mean` | 1223 |
| `city_pop` | 1195 |
| `time_since_last_txn` | 1065 |
| `geo_distance` | 825 |
| `lat` | 821 |

---

## Rule Engine

Rules are Spring `@Component` beans implementing the `Rule` interface. `RuleEngine` autowires `List<Rule>` — adding a new rule requires only creating a new `@Component` class, no changes to `RuleEngine`.

```java
public interface Rule {
    boolean apply(FinancialTransaction t);
    String getName();
}
```

### HighAmountRule
Fires when `amount > 1,000,000`. Straightforward threshold check.

### StructuringRule
Queries all transactions from the same sender in the last hour (excluding current), sums their amounts. Fires when cumulative total exceeds $1,000,000. Detects the common money laundering technique of splitting large transfers into smaller amounts below reporting thresholds.

### FrequencyRule
Queries transactions from the same sender in the last hour. Fires when count ≥ 10. Detects card testing and rapid-fire fraud attacks.

### Graph Cycle Detection
`GraphService` builds a directed graph where each edge represents a transaction (sender → receiver). `CycleDetection` runs DFS from each sender node. If a path returns to the origin through 3+ nodes, a money laundering ring is detected. Graph is cached with a 5-minute TTL and invalidated after each batch save.

---

## Risk Scoring

All violations are scored and summed, capped at 100.

| Rule | Score |
|---|---|
| High Amount Rule | +50 |
| Structuring Rule | +40 |
| Graph Cycle Fraud | +70 |
| High Frequency Rule | +30 |
| AI Anomaly Detection | +60 |
| Amount > $2,000,000 | +10 bonus |
| **Maximum** | **100** |

---

## UI

Available at `http://localhost:8081/ui`

**Single mode** — form fields for all transaction properties including optional location coordinates and cardholder profile. Behavioural features are computed automatically.

**Batch mode** — paste a raw JSON array directly. Required fields: `senderId`, `receiverId`, `amount`. All other fields optional.

Results are colour-coded:
- 🟢 Green — Transaction OK
- 🔴 Red — FLAGGED (with risk score and violated rules)
- 🟡 Yellow — FAILED (processing error, batch continues)

---

## Test Cases

### Clean transaction
```json
{"senderId":"card_clean","receiverId":"merchant_groceries","amount":42.50,"category":"grocery_pos","gender":"M","age":28}
```
Expected: `Transaction OK`

### High Amount Rule
```json
{"senderId":"card_01","receiverId":"merchant_wire","amount":1500000,"category":"misc_net","gender":"M","age":45}
```
Expected: `FLAGGED | Risk: 50 | High Amount Rule`

### Graph Cycle (send as one batch)
```json
[
  {"senderId":"node_A","receiverId":"node_B","amount":1000,"category":"misc_net","gender":"M","age":35},
  {"senderId":"node_B","receiverId":"node_C","amount":1000,"category":"misc_net","gender":"M","age":35},
  {"senderId":"node_C","receiverId":"node_A","amount":1000,"category":"misc_net","gender":"M","age":35}
]
```
Expected: `FLAGGED | Risk: 70 | Graph Cycle Fraud`

### Maximum risk (all rules)
Send 11 transactions from same sender in rapid succession, with amount > $1M and an existing cycle. Expected: `FLAGGED | Risk: 100`

---

## Acknowledgements

- Dataset: [Credit Card Transactions Fraud Detection Dataset](https://www.kaggle.com/datasets/kartik2112/fraud-detection) by Kartik Shenoy — CC0 Public Domain

