

import sys
import pandas as pd
import joblib

from xgboost import XGBClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.metrics import average_precision_score
from imblearn.over_sampling import SMOTE
from preprocess import preprocess




TRAIN_CSV    = "fraudTrain.csv"
SMOTE_RATIO  = 0.1
RANDOM_STATE = 42




if "--retune" in sys.argv:
    print("Running tune.py first...")
    import subprocess
    subprocess.run([sys.executable, "tune.py"], check=True)
    print()




try:
    best_params = joblib.load("best_params.pkl")
    print("Loaded best_params.pkl ")
    print(f"Params: {best_params}\n")
except FileNotFoundError:
    print("ERROR: best_params.pkl not found.")
    print("Run tune.py first")
    sys.exit(1)




print("Loading data...")
df = pd.read_csv(TRAIN_CSV)
df = preprocess(df, training=True)

features = [
    'amt', 'city_pop', 'age', 'hour',
    'lat', 'long', 'merch_lat', 'merch_long',
    'category_enc', 'gender_enc',
    'time_since_last_txn', 'txn_count_per_card_day',
    'amt_vs_card_mean', 'amt_vs_card_std', 'geo_distance'
]

missing = [col for col in features if col not in df.columns]
if missing:
    raise ValueError(f"Missing required feature columns: {missing}")

X = df[features]
y = df['is_fraud']




X_train, X_val, y_train, y_val = train_test_split(
    X, y, test_size=0.2, stratify=y, random_state=RANDOM_STATE
)
print(f"Train : {len(X_train):,}  (fraud: {y_train.sum():,})")
print(f"Val   : {len(X_val):,}  (fraud: {y_val.sum():,})")




scaler = StandardScaler()
X_train_scaled = scaler.fit_transform(X_train)
X_val_scaled   = pd.DataFrame(scaler.transform(X_val), columns=features)




sm = SMOTE(sampling_strategy=SMOTE_RATIO, random_state=RANDOM_STATE)
X_train_res, y_train_res = sm.fit_resample(X_train_scaled, y_train)
X_train_res = pd.DataFrame(X_train_res, columns=features)

neg_res = (y_train_res == 0).sum()
pos_res = (y_train_res == 1).sum()
print(f"\nAfter SMOTE — Legit: {neg_res:,}  Fraud: {pos_res:,}")




print("\nTraining XGBoost with saved params...")
clf = XGBClassifier(**best_params)
clf.fit(
    X_train_res, y_train_res,
    eval_set=[(X_val_scaled, y_val)],
    verbose=50,
)

final_pr_auc = average_precision_score(
    y_val, clf.predict_proba(X_val_scaled)[:, 1]
)
print(f"\nFinal PR-AUC : {final_pr_auc:.4f}")




importances = pd.Series(clf.feature_importances_, index=features)
print("\nTop 10 features:")
print(importances.sort_values(ascending=False).head(10).to_string())




joblib.dump(clf,      "clf_model.pkl")
joblib.dump(scaler,   "scaler.pkl")
joblib.dump(features, "features.pkl")

print(f"\n{'='*52}")
print(f"  Saved: clf_model.pkl, scaler.pkl, features.pkl")
print(f"  PR-AUC: {final_pr_auc:.4f}")
print(f"{'='*52}")
print("Run evaluate.py")