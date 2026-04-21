import pandas as pd
import numpy as np
import joblib
import matplotlib.pyplot as plt
from sklearn.metrics import (
    classification_report,
    confusion_matrix,
    precision_recall_curve,
    auc,
    average_precision_score
)
from sklearn.model_selection import train_test_split
from preprocess import preprocess




PRECISION_FLOOR = 0.30
PLOT_CURVE      = True




clf      = joblib.load("clf_model.pkl")
scaler   = joblib.load("scaler.pkl")
features = joblib.load("features.pkl")




df = pd.read_csv("fraudTrain.csv")
df = preprocess(df, training=False)

missing = [col for col in features if col not in df.columns]
if missing:
    raise ValueError(f"Missing required feature columns after preprocessing: {missing}")

X = df[features]
y = df["is_fraud"]


_, X_test, _, y_test = train_test_split(
    X, y, test_size=0.2, random_state=42, stratify=y
)

X_scaled = scaler.transform(X_test)




probs = clf.predict_proba(X_scaled)[:, 1]




precision_curve, recall_curve, thresholds = precision_recall_curve(y_test, probs)
pr_auc        = auc(recall_curve, precision_curve)
avg_precision = average_precision_score(y_test, probs)

print(f"\n{'='*52}")
print(f"  PR-AUC            : {pr_auc:.4f}")
print(f"  Average Precision : {avg_precision:.4f}")
print(f"{'='*52}")






f1_scores = (
    2 * precision_curve[:-1] * recall_curve[:-1]
    / (precision_curve[:-1] + recall_curve[:-1] + 1e-9)
)

valid_mask = precision_curve[:-1] >= PRECISION_FLOOR

if valid_mask.any():
    
    
    best_idx       = np.where(valid_mask)[0][0]   
    best_threshold = thresholds[best_idx]
    best_recall    = recall_curve[best_idx]
    best_precision = precision_curve[best_idx]
    best_f1        = f1_scores[best_idx]
    print(f"\n  Recall-first threshold : {best_threshold:.4f}")
    print(f"    Precision            : {best_precision:.4f}")
    print(f"    Recall               : {best_recall:.4f}")
    print(f"    F1                   : {best_f1:.4f}")
    print(f"    (Precision floor was : {PRECISION_FLOOR})")
else:
    
    best_idx       = f1_scores.argmax()
    best_threshold = thresholds[best_idx]
    best_recall    = recall_curve[best_idx]
    best_precision = precision_curve[best_idx]
    best_f1        = f1_scores[best_idx]
    print(f"\n  No threshold meets precision floor {PRECISION_FLOOR}.")
    print(f"  Falling back to best-F1 threshold: {best_threshold:.4f}")


f1_best_idx        = f1_scores.argmax()
f1_threshold       = thresholds[f1_best_idx]
f1_at_best         = f1_scores[f1_best_idx]

print(f"\n  Best-F1 threshold: {f1_threshold:.4f}  (F1={f1_at_best:.4f})")




best_pred = (probs >= best_threshold).astype(int)

print(f"\n{'─'*52}")
print(f"Confusion Matrix  (threshold={best_threshold:.4f})")
print(f"{'─'*52}")
cm = confusion_matrix(y_test, best_pred)
print(cm)

tn, fp, fn, tp = cm.ravel()
print(f"\n  True Positives  (caught fraud)   : {tp}")
print(f"  False Negatives (missed fraud)   : {fn}")
print(f"  False Positives (false alarms)   : {fp}")
print(f"  True Negatives  (correct legit)  : {tn}")

print("\nClassification Report:")
print(classification_report(y_test, best_pred, zero_division=0,
                            target_names=["Legit", "Fraud"]))




total_fraud = y_test.sum()
print(f"\n{'Threshold':>10} {'Precision':>10} {'Recall':>10} {'F1':>8} "
      f"{'Caught':>10} {'FalseAlarms':>12}")
print("─" * 64)

sweep_thresholds = [0.10, 0.20, 0.30, 0.40, 0.50, 0.60, 0.70, 0.80, best_threshold]
sweep_thresholds = sorted(set(round(t, 4) for t in sweep_thresholds))

for t in sweep_thresholds:
    pred  = (probs >= t).astype(int)
    _tp   = int(((pred == 1) & (y_test == 1)).sum())
    _fp   = int(((pred == 1) & (y_test == 0)).sum())
    _fn   = int(((pred == 0) & (y_test == 1)).sum())
    p     = _tp / (_tp + _fp + 1e-9)
    r     = _tp / (_tp + _fn + 1e-9)
    f1    = 2 * p * r / (p + r + 1e-9)
    marker = " ◀ selected" if abs(t - best_threshold) < 1e-5 else ""
    print(f"{t:>10.4f} {p:>10.3f} {r:>10.3f} {f1:>8.3f} "
          f"{_tp:>5}/{total_fraud:<4} {_fp:>10}{marker}")




joblib.dump(best_threshold, "threshold.pkl")
print(f"\nThreshold {best_threshold:.4f} saved to threshold.pkl ✅")




if PLOT_CURVE:
    fig, axes = plt.subplots(1, 3, figsize=(18, 5))

    
    axes[0].plot(recall_curve, precision_curve,
                 label=f"PR-AUC = {pr_auc:.4f}", linewidth=2, color="steelblue")
    axes[0].axhline(y=y_test.mean(), color="grey", linestyle=":",
                    label=f"Baseline (random) = {y_test.mean():.4f}")
    axes[0].axvline(x=best_recall, color="crimson", linestyle="--", alpha=0.7,
                    label=f"Selected recall = {best_recall:.3f}")
    axes[0].scatter([best_recall], [best_precision],
                    color="crimson", zorder=5, s=80)
    axes[0].set_xlabel("Recall")
    axes[0].set_ylabel("Precision")
    axes[0].set_title("Precision-Recall Curve")
    axes[0].legend(fontsize=9)
    axes[0].grid(True, alpha=0.3)

    
    axes[1].plot(thresholds, f1_scores, label="F1 Score",
                 color="green", linewidth=2)
    axes[1].axvline(x=best_threshold, color="crimson", linestyle="--",
                    label=f"Selected threshold = {best_threshold:.4f}")
    axes[1].axvline(x=f1_threshold, color="orange", linestyle=":",
                    label=f"Best-F1 threshold = {f1_threshold:.4f}")
    axes[1].set_xlabel("Threshold")
    axes[1].set_ylabel("F1 Score")
    axes[1].set_title("F1 vs Decision Threshold")
    axes[1].legend(fontsize=9)
    axes[1].grid(True, alpha=0.3)

    
    axes[2].plot(thresholds, recall_curve[:-1],
                 label="Recall", color="steelblue", linewidth=2)
    axes[2].plot(thresholds, precision_curve[:-1],
                 label="Precision", color="darkorange", linewidth=2)
    axes[2].axvline(x=best_threshold, color="crimson", linestyle="--",
                    label=f"Selected = {best_threshold:.4f}")
    axes[2].axhline(y=PRECISION_FLOOR, color="grey", linestyle=":",
                    label=f"Precision floor = {PRECISION_FLOOR}")
    axes[2].set_xlabel("Threshold")
    axes[2].set_ylabel("Score")
    axes[2].set_title("Recall & Precision vs Threshold")
    axes[2].legend(fontsize=9)
    axes[2].grid(True, alpha=0.3)

    plt.tight_layout()
    plt.savefig("pr_curve.png", dpi=150)
    plt.show()
    print("Plot saved to pr_curve.png")